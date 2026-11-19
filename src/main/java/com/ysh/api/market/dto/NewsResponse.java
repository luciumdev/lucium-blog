package com.ysh.api.market.dto;

import com.ysh.domain.news.entity.NewsCategory;
import com.ysh.domain.news.entity.NewsFeed;

import java.time.LocalDateTime;
import java.util.List;

public record NewsResponse(
        NewsCategory category,
        int count,
        List<NewsItem> items
) {
    public static NewsResponse of(NewsCategory category, List<NewsItem> items) {
        return new NewsResponse(category, items.size(), items);
    }

    /** 보강 정보(키워드·지표) 없이 변환 — 기존 호출부 호환용 */
    public static NewsResponse from(NewsCategory category, List<NewsFeed> feeds) {
        return of(category, feeds.stream()
                .map(f -> NewsItem.of(f, List.of(), List.of()))
                .toList());
    }

    /**
     * id 는 노출하지 않음 (내부 PK — 기존 응답 계약 유지)
     * @param titleKo    영어 제목 번역 (IT, 보강 완료 시). 없으면 null → 프론트는 원문 표시
     * @param keywords   정규화 키워드 (보강 전이면 빈 배열)
     * @param indicators 게시일 기준 지표 등락 (데이터 없으면 빈 배열)
     */
    public record NewsItem(
            String source,
            String title,
            String titleKo,
            String url,
            String summary,
            LocalDateTime publishedAt,
            List<String> keywords,
            List<IndicatorChip> indicators
    ) {
        public static NewsItem of(NewsFeed feed, List<String> keywords, List<IndicatorChip> indicators) {
            return new NewsItem(
                    feed.getSource(),
                    feed.getTitle(),
                    feed.getTitleKo(),
                    feed.getUrl(),
                    feed.getSummary(),
                    feed.getPublishedAt(),
                    keywords,
                    indicators
            );
        }
    }

    /**
     * @param code       NASDAQ_PROXY | KOSPI | USD
     * @param label      화면 표시명 (QQQ, KOSPI, 원/달러)
     * @param changeRate 등락률(%) 소수 2자리
     */
    public record IndicatorChip(String code, String label, double changeRate) {}
}
