package com.ysh.infra.aijournal;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiJournalRssCollector {
    private static final Duration FRESHNESS_WINDOW = Duration.ofDays(2);
    private static final int MAX_ITEMS_PER_SOURCE = 15;

    private final RestClient restClient;

    private static final List<RssSource> RSS_SOURCES = List.of(
            new RssSource("OpenAI",          "https://openai.com/blog/rss.xml"),
            new RssSource("Anthropic",        "https://raw.githubusercontent.com/taobojlen/anthropic-rss-feed/main/anthropic_news_rss.xml"),
            new RssSource("Hugging Face",     "https://huggingface.co/blog/feed.xml")
    );

    private static final List<String> GITHUB_REPOS = List.of(
            "langchain-ai/langchain",
            "huggingface/transformers",
            "openai/openai-python",
            "anthropics/anthropic-sdk-python",
            "run-llama/llama_index",
            "microsoft/autogen",
            "ollama/ollama"
    );

    public CollectionResult collectAll() {
        List<CollectedAiNews> news = new ArrayList<>();
        List<SourceFailure> failures = new ArrayList<>();
        String now = Instant.now().toString();  // ISO-8601

        for (RssSource source : RSS_SOURCES) {
            try {
                List<CollectedAiNews> items = fetchRss(source);
                news.addAll(items);
                log.debug("[{}] {}건 수집", source.name(), items.size());
            } catch (Exception e) {
                String reason = shortReason(e);
                log.warn("[{}] RSS 수집 실패: {}", source.name(), reason);
                failures.add(new SourceFailure(source.name(), reason, now));
            }
        }

        for (String repo : GITHUB_REPOS) {
            try {
                news.addAll(fetchGithubReleases(repo));
            } catch (Exception e) {
                String reason = shortReason(e);
                log.warn("[GitHub:{}] 수집 실패: {}", repo, reason);
                failures.add(new SourceFailure("GitHub:" + repo, reason, now));
            }
        }

        try {
            news.addAll(fetchHackerNews());
        } catch (Exception e) {
            String reason = shortReason(e);
            log.warn("[HackerNews] 수집 실패: {}", reason);
            failures.add(new SourceFailure("HackerNews", reason, now));
        }

        log.info("AI 저널 수집 완료 — 성공 {}건, 실패 소스 {}개", news.size(), failures.size());
        return new CollectionResult(news, failures);
    }

    private List<CollectedAiNews> fetchRss(RssSource source) {
        String xml = restClient.get()
                .uri(source.url())
                .retrieve()
                .body(String.class);
        return parseRssItems(xml, source.name());
    }

    private List<CollectedAiNews> parseRssItems(String xml, String sourceName) {
        List<CollectedAiNews> items = new ArrayList<>();
        Instant cutoff = Instant.now().minus(FRESHNESS_WINDOW);

        Pattern itemPattern = Pattern.compile("<item>(.*?)</item>", Pattern.DOTALL);
        Matcher itemMatcher = itemPattern.matcher(xml);

        while (itemMatcher.find()) {
            String item    = itemMatcher.group(1);
            String title   = extractTag(item, "title");
            String link    = extractTag(item, "link");
            String desc    = extractTag(item, "description");
            String pubDate = extractTag(item, "pubDate");

            // 신선도 필터: pubDate 파싱 실패 또는 cutoff 이전이면 제외
            Instant published = parsePubDate(pubDate);
            if (published == null || published.isBefore(cutoff)) {
                continue;
            }

            if (title != null && link != null) {
                String content = cleanHtml(desc != null ? desc : "");
                if (content.length() > 300) {
                    content = content.substring(0, 300) + "...";
                }
                items.add(new CollectedAiNews(
                        sourceName, title.trim(), content, link.trim(), pubDate
                ));
            }
        }

        // 안전망: 신선도 통과해도 소스당 N건 초과 시 컷
        if (items.size() > MAX_ITEMS_PER_SOURCE) {
            log.warn("[{}] 신선도 통과 {}건 → 상위 {}건만 유지",
                    sourceName, items.size(), MAX_ITEMS_PER_SOURCE);
            items = items.subList(0, MAX_ITEMS_PER_SOURCE);
        }
        return items;
    }

    private List<CollectedAiNews> fetchGithubReleases(String repo) {
        String url = "https://api.github.com/repos/" + repo + "/releases?per_page=3";
        String json = restClient.get()
                .uri(url)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .body(String.class);
        return parseGithubReleases(json, repo);
    }

    @SuppressWarnings("unchecked")
    private List<CollectedAiNews> parseGithubReleases(String json, String repo) {
        List<CollectedAiNews> result = new ArrayList<>();
        Instant cutoff = Instant.now().minus(FRESHNESS_WINDOW);
        try {
            var mapper    = new ObjectMapper();
            var releases  = mapper.readValue(json, List.class);
            for (Object r : releases) {
                var release = (Map<String, Object>) r;
                // 신선도 필터
                String created = (String) release.get("created_at");
                Instant publishedAt = parseGithubCreatedAt(created);
                if (publishedAt == null || publishedAt.isBefore(cutoff)) {
                    continue;
                }
                String tagName  = (String) release.get("tag_name");
                String body     = (String) release.get("body");
                String htmlUrl  = (String) release.get("html_url");
                String title    = repo.split("/")[1] + " " + tagName + " 릴리즈";
                String content  = body != null && body.length() > 300
                        ? body.substring(0, 300) + "..."
                        : (body != null ? body : "");

                result.add(new CollectedAiNews(
                        "GitHub:" + repo.split("/")[1],
                        title, content, htmlUrl, created
                ));
            }
        } catch (Exception e) {
            log.warn("GitHub 릴리즈 파싱 실패: {}", e.getMessage());
        }
        return result;
    }

    private List<CollectedAiNews> fetchHackerNews() {
        // 신선도 필터: cutoff = now - 2일 (AI 저널 도메인 통일).
        // 이전엔 LocalDate.now().atStartOfDay()로 "오늘 자정" 기준이었으나,
        // cron 실행 시각에 따라 윈도우가 0~24시간으로 변동되는 trap이 있어 명시적 2일 윈도우로 교체.
        long cutoffEpoch = Instant.now().minus(FRESHNESS_WINDOW).getEpochSecond();
        String url = "https://hn.algolia.com/api/v1/search?"
                + "query=AI+LLM+machine+learning"
                + "&tags=story"
                + "&numericFilters=created_at_i>" + cutoffEpoch
                + "&hitsPerPage=10";
        String json = restClient.get().uri(url).retrieve().body(String.class);
        return parseHackerNews(json);
    }

    private List<CollectedAiNews> parseHackerNews(String json) {
        List<CollectedAiNews> result = new ArrayList<>();
        try {
            var mapper = new ObjectMapper();
            var root   = mapper.readTree(json);
            var hits   = root.get("hits");
            if (hits == null) return result;

            for (var h : hits) {
                String title = h.has("title")     ? h.get("title").asText()  : "";
                String url   = h.has("url")        ? h.get("url").asText()   : "";
                // [수정 6] "objectId" → "objectID"  (Algolia API 필드명은 대문자 D)
                String objId = h.has("objectID")   ? h.get("objectID").asText() : "";
                String hnUrl = "https://news.ycombinator.com/item?id=" + objId;
                int points   = h.has("points")     ? h.get("points").asInt() : 0;

                if (points >= 100 && !title.isEmpty()) {
                    result.add(new CollectedAiNews(
                            "Hacker News", title, "", url.isEmpty() ? hnUrl : url, ""
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("Hacker News 파싱 실패: {}", e.getMessage());
        }
        return result;
    }

    private String extractTag(String xml, String tag) {
        Pattern p = Pattern.compile(
                "<" + tag + "(?:[^>]*)>\\s*(?:<!\\[CDATA\\[)?(.*?)(?:\\]\\]>)?\\s*</" + tag + ">",
                Pattern.DOTALL);
        Matcher m = p.matcher(xml);
        return m.find() ? m.group(1).trim() : null;
    }

    private String cleanHtml(String html) {
        return html
                .replaceAll("<[^>]+>", "")
                .replaceAll("&amp;",   "&")
                .replaceAll("&lt;",    "<")
                .replaceAll("&gt;",    ">")
                .replaceAll("&quot;",  "\"")
                .replaceAll("&#\\d+;", "")
                .replaceAll("\\s+",    " ")
                .trim();
    }

    record RssSource(String name, String url) {}

    /**
     * rss 실패 이유 return
     * @param e
     * @return
     */
    private static String shortReason(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return e.getClass().getSimpleName();
        int colon = msg.indexOf(':');
        String head = colon > 0 ? msg.substring(0, colon) : msg;
        return head.length() > 100 ? head.substring(0, 100) : head;
    }


    /**
     * RSS pubDate(RFC 1123) → Instant.
     * @param pubDate
     * @return
     */
    private static Instant parsePubDate(String pubDate) {
        if (pubDate == null || pubDate.isBlank()) return null;
        try {
            return ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * GitHub Releases API의 {@code created_at}(ISO-8601 String) → {@link Instant}.
     *
     * null/빈/포맷 오류 시 null 반환 — 호출 측이 cutoff 비교에서 제외 처리.
     * RSS의 {@link #parsePubDate(String)}(RFC 1123)와 포맷이 달라 별도 메서드로 분리.
     *
     * @param created GitHub API의 created_at 필드 값 (예: "2024-05-01T12:34:56Z")
     * @return 파싱된 Instant, 실패 시 null
     */
    private static Instant parseGithubCreatedAt(String created) {
        if (created == null || created.isBlank()) return null;
        try {
            return Instant.parse(created);
        } catch (DateTimeParseException e) {
            log.warn("GitHub created_at 파싱 실패: {}", created);
            return null;
        }
    }

}