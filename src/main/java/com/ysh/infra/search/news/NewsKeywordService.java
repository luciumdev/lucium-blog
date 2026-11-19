package com.ysh.infra.search.news;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.ysh.infra.search.news.dto.CommonKeywordsResponse;
import com.ysh.infra.search.news.dto.CommonKeywordsResponse.KeywordCount;
import com.ysh.infra.search.news.dto.KeywordTrendResponse;
import com.ysh.infra.search.news.dto.KeywordTrendResponse.DailyCount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 뉴스 키워드 집계 (ES aggregations).
 *
 * <ul>
 *   <li>공통 키워드: terms(keywords) → 하위 terms(category). 테크·경제 양쪽에 모두 등장한 키워드만.</li>
 *   <li>키워드 추이: term(keywords) 필터 + date_histogram(day).</li>
 * </ul>
 * 문서 본문은 필요 없으므로 size=0 으로 집계 결과만 받는다.
 * ES 장애 시 빈 응답 — 뉴스 목록 자체는 DB/Redis 경로라 영향 없음.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsKeywordService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter ES_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final String CATEGORY_IT = "IT";
    private static final String CATEGORY_ECONOMY = "ECONOMY";

    private static final String AGG_BY_KEYWORD = "by_keyword";
    private static final String AGG_BY_CATEGORY = "by_category";
    private static final String AGG_PER_DAY = "per_day";

    private static final int KEYWORD_BUCKETS = 50;
    private static final int COMMON_LIMIT = 8;

    private final ElasticsearchOperations operations;

    /**
     * 최근 days 일 동안 테크·경제 양쪽에 등장한 키워드.
     * 정렬: 적은 쪽 건수(min) 내림차순 → 합계 내림차순. 한쪽에 몰린 키워드보다 "겹치는" 키워드 우선.
     */
    public CommonKeywordsResponse commonKeywords(int days) {
        int d = Math.clamp(days, 1, 7);
        LocalDateTime from = LocalDateTime.now(KST).minusDays(d);

        try {
            NativeQuery query = NativeQuery.builder()
                    .withQuery(rangeAndCategories(from))
                    .withAggregation(AGG_BY_KEYWORD, Aggregation.of(a -> a
                            .terms(t -> t.field(NewsSearchDocument.FIELD_KEYWORDS).size(KEYWORD_BUCKETS))
                            .aggregations(AGG_BY_CATEGORY, Aggregation.of(s -> s
                                    .terms(t -> t.field(NewsSearchDocument.FIELD_CATEGORY).size(5))))))
                    .withMaxResults(0)
                    .build();

            Aggregate byKeyword = aggregate(operations.search(query, NewsSearchDocument.class), AGG_BY_KEYWORD);
            if (byKeyword == null) return CommonKeywordsResponse.empty(d, from);

            List<KeywordCount> common = new ArrayList<>();
            for (StringTermsBucket bucket : byKeyword.sterms().buckets().array()) {
                long tech = 0;
                long economy = 0;
                Aggregate byCategory = bucket.aggregations().get(AGG_BY_CATEGORY);
                if (byCategory != null) {
                    for (StringTermsBucket c : byCategory.sterms().buckets().array()) {
                        String category = c.key().stringValue();
                        if (CATEGORY_IT.equals(category)) tech = c.docCount();
                        else if (CATEGORY_ECONOMY.equals(category)) economy = c.docCount();
                    }
                }
                if (tech > 0 && economy > 0) {
                    common.add(new KeywordCount(bucket.key().stringValue(), tech, economy));
                }
            }
            common.sort(Comparator
                    .comparingLong((KeywordCount k) -> Math.min(k.tech(), k.economy())).reversed()
                    .thenComparing(Comparator.comparingLong(KeywordCount::total).reversed())
                    .thenComparing(KeywordCount::keyword));

            return new CommonKeywordsResponse(d, from, common.stream().limit(COMMON_LIMIT).toList());
        } catch (Exception e) {
            log.warn("공통 키워드 집계 실패: {}", e.getMessage());
            return CommonKeywordsResponse.empty(d, from);
        }
    }

    /** 키워드 일별 기사 수 (테크+경제). 기사가 없는 날은 0 으로 채운다. */
    public KeywordTrendResponse trend(String keyword, int days) {
        int d = Math.clamp(days, 3, 14);
        LocalDate today = LocalDate.now(KST);
        LocalDate fromDate = today.minusDays(d - 1L);
        String kw = keyword == null ? "" : keyword.strip();
        if (kw.isEmpty()) return KeywordTrendResponse.zeros(kw, fromDate, today);

        try {
            Query filter = Query.of(q -> q.bool(b -> b
                    .filter(rangeAndCategories(fromDate.atStartOfDay()))
                    .filter(f -> f.term(t -> t.field(NewsSearchDocument.FIELD_KEYWORDS).value(kw)))));

            NativeQuery query = NativeQuery.builder()
                    .withQuery(filter)
                    .withAggregation(AGG_PER_DAY, Aggregation.of(a -> a
                            .dateHistogram(h -> h
                                    .field(NewsSearchDocument.FIELD_PUBLISHED_AT)
                                    .calendarInterval(CalendarInterval.Day)
                                    .format("yyyy-MM-dd")
                                    .minDocCount(0))))
                    .withMaxResults(0)
                    .build();

            Aggregate perDay = aggregate(operations.search(query, NewsSearchDocument.class), AGG_PER_DAY);
            Map<LocalDate, Long> counts = new HashMap<>();
            if (perDay != null) {
                for (DateHistogramBucket bucket : perDay.dateHistogram().buckets().array()) {
                    String key = bucket.keyAsString();
                    if (key != null) counts.put(LocalDate.parse(key), bucket.docCount());
                }
            }

            List<DailyCount> series = new ArrayList<>();
            for (LocalDate day = fromDate; !day.isAfter(today); day = day.plusDays(1)) {
                series.add(new DailyCount(day, counts.getOrDefault(day, 0L)));
            }
            return new KeywordTrendResponse(kw, series);
        } catch (Exception e) {
            log.warn("키워드 추이 집계 실패. keyword={}, cause={}", kw, e.getMessage());
            return KeywordTrendResponse.zeros(kw, fromDate, today);
        }
    }

    // ───────────────────────── private ─────────────────────────

    /** publishedAt >= from AND category IN (IT, ECONOMY) */
    private Query rangeAndCategories(LocalDateTime from) {
        String gte = from.format(ES_DATE_TIME);
        return Query.of(q -> q.bool(b -> b
                .filter(f -> f.range(r -> r.date(dr -> dr
                        .field(NewsSearchDocument.FIELD_PUBLISHED_AT)
                        .gte(gte))))
                .filter(f -> f.terms(t -> t
                        .field(NewsSearchDocument.FIELD_CATEGORY)
                        .terms(v -> v.value(List.of(
                                FieldValue.of(CATEGORY_IT),
                                FieldValue.of(CATEGORY_ECONOMY))))))));
    }

    private Aggregate aggregate(SearchHits<NewsSearchDocument> hits, String name) {
        if (!(hits.getAggregations() instanceof ElasticsearchAggregations aggs)) return null;
        var agg = aggs.aggregationsAsMap().get(name);
        return agg == null ? null : agg.aggregation().getAggregate();
    }
}
