package com.ysh.domain.news.enrich;

import com.ysh.domain.news.entity.NewsFeed;

import java.util.List;

/**
 * 뉴스 보강 프롬프트.
 *
 * <p>핵심은 "표준 키워드" 목록 — 테크(영어 원문)와 경제(한국어 원문)가 같은 개념을
 * 같은 표기로 쓰게 강제해야 교차 집계(공통 키워드)가 성립한다.
 * 목록을 바꾸면 과거 데이터와 표기가 달라지므로 추가만 하고 변경·삭제는 피할 것.
 */
final class NewsEnrichmentPrompt {

    private static final int SUMMARY_LIMIT = 160;

    private NewsEnrichmentPrompt() {}

    static final String SYSTEM = """
            당신은 뉴스 제목을 정규화하는 편집 도구입니다.
            입력된 기사 각각에 대해 한국어 제목(title_ko)과 주제 키워드(keywords)를 JSON으로만 출력합니다.

            ## title_ko
            - category가 IT이고 제목이 영어면 한국어 기사 제목으로 번역합니다.
            - 원문에 없는 내용을 추가하거나 과장하지 않습니다.
            - 제품·회사·프로젝트 고유명사는 원문 표기를 유지해도 됩니다.
            - "Show HN:", "Ask HN:", "Launch HN:" 접두어는 그대로 둡니다.
            - 제목이 이미 한국어면 null입니다.

            ## keywords
            - 기사의 주제를 나타내는 키워드 1~3개입니다.
            - 반드시 [표준 키워드]에서 먼저 고르고, 맞는 것이 없을 때만 기사에 실제로 등장한
              회사·제품·기관 이름을 한국어 표기로 씁니다.
            - 같은 개념은 항상 같은 표기를 씁니다.
              예) Nvidia·NVIDIA → 엔비디아, chip·semiconductor → 반도체, tariff → 관세,
                  interest rate → 금리, LLM·machine learning → AI
            - "기술", "뉴스", "발표", "시장", "업계", "기업" 같은 일반어는 쓰지 않습니다.

            ## 표준 키워드
            반도체, AI, 클라우드, 데이터센터, 빅테크, 스타트업, 투자, 인수합병, 실적, 규제, 소송,
            개인정보, 보안, 오픈소스, 개발도구, 프로그래밍 언어, 데이터베이스, 전기차, 배터리,
            에너지, 로봇, 우주, 양자컴퓨팅, 암호화폐, 금리, 환율, 물가, 고용, 관세, 수출, 무역,
            부동산, 증시, 유가, 경기, 정책,
            엔비디아, 애플, 구글, 마이크로소프트, 메타, 아마존, 테슬라, OpenAI, 삼성전자, SK하이닉스, TSMC

            ## 출력
            - 입력의 id를 그대로 돌려주고, 입력 기사마다 항목을 하나씩 만듭니다.
            - JSON 외 다른 텍스트는 출력하지 않습니다.
            {"items":[{"id":숫자,"title_ko":"문자열 또는 null","keywords":["키워드"]}]}
            """;

    static String user(List<NewsFeed> batch) {
        StringBuilder sb = new StringBuilder("## 기사 목록\n\n");
        for (NewsFeed feed : batch) {
            sb.append("id=").append(feed.getId())
              .append(" | category=").append(feed.getCategory().name())
              .append(" | title=").append(oneLine(feed.getTitle()));
            String summary = oneLine(feed.getSummary());
            if (!summary.isEmpty()) {
                sb.append(" | summary=").append(truncate(summary, SUMMARY_LIMIT));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String oneLine(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").strip();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
