package com.ysh.api.market;

import com.ysh.api.market.dto.NewsResponse;
import com.ysh.api.market.dto.NewsResponse.IndicatorChip;
import com.ysh.api.market.dto.NewsResponse.NewsItem;
import com.ysh.domain.market.service.MarketContextService;
import com.ysh.domain.news.enrich.NewsKeywordCodec;
import com.ysh.domain.news.entity.NewsCategory;
import com.ysh.domain.news.entity.NewsFeed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 뉴스 목록 → 응답 조립. 키워드(JSONB) 파싱 + 기사별 지표 칩.
 *
 * <p>칩 매핑 (기사 성격에 맞는 지표만):
 * <ul>
 *   <li>IT(HackerNews) → QQQ (나스닥 100 ETF)</li>
 *   <li>ECONOMY(한국경제) → KOSPI, 원/달러</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class NewsResponseAssembler {

    private record ChipSpec(String code, String label) {}

    private static final Map<NewsCategory, List<ChipSpec>> CHIPS = Map.of(
            NewsCategory.IT, List.of(new ChipSpec("NASDAQ_PROXY", "QQQ")),
            NewsCategory.ECONOMY, List.of(
                    new ChipSpec("KOSPI", "KOSPI"),
                    new ChipSpec(MarketContextService.USD, "원/달러"))
    );

    private final MarketContextService marketContextService;
    private final NewsKeywordCodec keywordCodec;

    public NewsResponse assemble(NewsCategory category, List<NewsFeed> feeds) {
        List<ChipSpec> specs = CHIPS.getOrDefault(category, List.of());
        MarketContextService.Lookup lookup = specs.isEmpty() || feeds.isEmpty()
                ? null
                : marketContextService.load(minDate(feeds), maxDate(feeds));

        List<NewsItem> items = feeds.stream()
                .map(feed -> NewsItem.of(
                        feed,
                        keywordCodec.fromJson(feed.getKeywords()),
                        chips(lookup, specs, feed.getPublishedAt().toLocalDate())))
                .toList();
        return NewsResponse.of(category, items);
    }

    private List<IndicatorChip> chips(MarketContextService.Lookup lookup, List<ChipSpec> specs, LocalDate date) {
        if (lookup == null) return List.of();
        List<IndicatorChip> chips = new ArrayList<>(specs.size());
        for (ChipSpec spec : specs) {
            lookup.changeRate(spec.code(), date)
                    .ifPresent(rate -> chips.add(new IndicatorChip(spec.code(), spec.label(), rate)));
        }
        return chips;
    }

    private static LocalDate minDate(List<NewsFeed> feeds) {
        return feeds.stream().map(f -> f.getPublishedAt().toLocalDate())
                .min(Comparator.naturalOrder()).orElseThrow();
    }

    private static LocalDate maxDate(List<NewsFeed> feeds) {
        return feeds.stream().map(f -> f.getPublishedAt().toLocalDate())
                .max(Comparator.naturalOrder()).orElseThrow();
    }
}
