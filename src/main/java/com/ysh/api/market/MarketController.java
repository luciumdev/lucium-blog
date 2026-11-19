package com.ysh.api.market;

import com.ysh.api.market.dto.*;
import com.ysh.common.cache.CacheResult;
import com.ysh.domain.market.domain.Commodities;
import com.ysh.domain.market.domain.ForexRates;
import com.ysh.domain.market.domain.IndicesData;
import com.ysh.domain.market.service.CommoditiesService;
import com.ysh.domain.market.service.ForexService;
import com.ysh.domain.market.service.IndicesService;
import com.ysh.domain.news.entity.NewsCategory;
import com.ysh.domain.news.entity.NewsFeed;
import com.ysh.domain.news.service.NewsService;
import com.ysh.infra.search.news.NewsKeywordService;
import com.ysh.infra.search.news.dto.CommonKeywordsResponse;
import com.ysh.infra.search.news.dto.KeywordTrendResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/market")
@RequiredArgsConstructor
public class MarketController {
    private final ForexService forexService;
    private final NewsService newsService;
    private final NewsResponseAssembler newsResponseAssembler;
    private final NewsKeywordService newsKeywordService;
    private final IndicesService indicesService;
    private final CommoditiesService commoditiesService;
    /**
     * 어제, 오늘
     * @return
     */
    @GetMapping("/forex")
    public ResponseEntity<ForexResponse> getForex() {
        CacheResult<ForexRates> result = forexService.getLatestCacheable();
        ForexRates yesterday = forexService.getYesterday();
        return ResponseEntity.ok()
                .header("X-Cache", result.isHit() ? "HIT" : "MISS")
                .body(ForexResponse.from(result.value(), yesterday));
    }

    /**
     * 뉴스 목록 (Redis 캐시) + 번역 제목·키워드·게시일 지표 칩.
     * 칩은 요청 시 index_bars / FOREX 스냅샷에서 계산 (쿼리 2회).
     */
    @GetMapping("/news")
    public ResponseEntity<NewsResponse> getNews(@RequestParam NewsCategory category) {
        CacheResult<List<NewsFeed>> result =
                newsService.getLatestByCategoryCacheable(category);
        return ResponseEntity.ok()
                .header("X-Cache", result.isHit() ? "HIT" : "MISS")
                .body(newsResponseAssembler.assemble(category, result.value()));
    }

    /** 테크·경제 공통 키워드 (ES terms 집계). days 1~7, 기본 1 */
    @GetMapping("/news/keywords")
    public ResponseEntity<CommonKeywordsResponse> getCommonKeywords(
            @RequestParam(defaultValue = "1") int days) {
        return ResponseEntity.ok(newsKeywordService.commonKeywords(days));
    }

    /** 키워드 일별 기사 수 (ES date_histogram). days 3~14, 기본 5 */
    @GetMapping("/news/keywords/trend")
    public ResponseEntity<KeywordTrendResponse> getKeywordTrend(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "5") int days) {
        return ResponseEntity.ok(newsKeywordService.trend(keyword, days));
    }

    @GetMapping("/indices")
    public ResponseEntity<IndicesResponse> getIndices() {
        CacheResult<IndicesData> result = indicesService.getLatestCacheable();
        return ResponseEntity.ok()
                .header("X-Cache", result.isHit() ? "HIT" : "MISS")
                .body(IndicesResponse.from(result.value()));
    }

    @GetMapping("/commodities")
    public ResponseEntity<CommoditiesResponse> getCommodities() {
        CacheResult<Commodities> result = commoditiesService.getLatestCacheable();
        return ResponseEntity.ok()
                .header("X-Cache", result.isHit() ? "HIT" : "MISS")
                .body(CommoditiesResponse.from(result.value()));
    }

    /**
     * 지수 시계열 히스토리 — 스냅샷 병합(방식 C) 결과.
     * <p>리베이싱 멀티라인 차트용. days 미지정 시 30일. 캐시 헤더 없음(DB 직조회).
     * @param days 조회 기간(일), 기본 30, 서비스에서 1~365 클램프
     */
    @GetMapping("/indices/history")
    public ResponseEntity<IndicesHistoryResponse> getIndicesHistory(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(indicesService.getHistory(days));
    }

}
