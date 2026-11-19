package com.ysh.api.market;


import com.ysh.common.cache.CacheResult;
import com.ysh.config.JacksonConfig;
import com.ysh.config.security.JwtAuthenticationFilter;
import com.ysh.domain.market.domain.ForexRate;
import com.ysh.domain.market.domain.ForexRates;
import com.ysh.domain.market.service.CommoditiesService;
import com.ysh.domain.market.service.ForexService;
import com.ysh.domain.market.service.IndicesService;
import com.ysh.domain.market.service.MarketContextService;
import com.ysh.domain.news.enrich.NewsKeywordCodec;
import com.ysh.domain.news.entity.NewsCategory;
import com.ysh.domain.news.entity.NewsFeed;
import com.ysh.domain.news.service.NewsService;
import com.ysh.infra.search.news.NewsKeywordService;
import com.ysh.infra.search.news.dto.CommonKeywordsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = MarketController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
// 응답 조립기는 실제 빈 사용 (키워드 파싱·칩 매핑까지 검증). Jackson 2 ObjectMapper 는 JacksonConfig 에서.
@Import({NewsResponseAssembler.class, NewsKeywordCodec.class, JacksonConfig.class})
class MarketControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    ForexService forexService;
    @MockitoBean
    NewsService newsService;
    @MockitoBean
    IndicesService indicesService;
    @MockitoBean
    CommoditiesService commoditiesService;
    @MockitoBean
    MarketContextService marketContextService;
    @MockitoBean
    NewsKeywordService newsKeywordService;

    // ───────────────── Forex ─────────────────

    @Test
    @WithMockUser
    void getForex_HIT_캐시값과_HIT헤더() throws Exception {
        ForexRates rates = new ForexRates(LocalDateTime.now(), List.of(
                new ForexRate("USD", "달러", 1380.0)
        ));
        when(forexService.getLatestCacheable()).thenReturn(CacheResult.hit(rates));
        when(forexService.getYesterday()).thenReturn(ForexRates.empty());

        mockMvc.perform(get("/v1/market/forex"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "HIT"))
                .andExpect(jsonPath("$.rates[0].currencyCode").value("USD"))
                .andExpect(jsonPath("$.rates[0].rate").value(1380.0));
    }

    @Test
    @WithMockUser
    void getForex_MISS_DB값과_MISS헤더() throws Exception {
        ForexRates rates = new ForexRates(LocalDateTime.now(), List.of(
                new ForexRate("EUR", "유로", 1500.0)
        ));
        when(forexService.getLatestCacheable()).thenReturn(CacheResult.miss(rates));
        when(forexService.getYesterday()).thenReturn(ForexRates.empty());

        mockMvc.perform(get("/v1/market/forex"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "MISS"));
    }

    // ───────────────── News ─────────────────

    @Test
    @WithMockUser
    void getNews_IT카테고리_정상응답() throws Exception {
        List<NewsFeed> feeds = List.of(
                NewsFeed.builder()
                        .source("HackerNews")
                        .category(NewsCategory.IT)
                        .title("test")
                        .url("https://example.com/1")
                        .summary(null)
                        .publishedAt(LocalDateTime.now())
                        .build()
        );
        when(newsService.getLatestByCategoryCacheable(NewsCategory.IT))
                .thenReturn(CacheResult.hit(feeds));

        mockMvc.perform(get("/v1/market/news?category=IT"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "HIT"))
                .andExpect(jsonPath("$.category").value("IT"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.items[0].source").value("HackerNews"))
                .andExpect(jsonPath("$.items[0].id").doesNotExist())
                .andExpect(jsonPath("$.items[0].fetchedAt").doesNotExist())
                // 보강 전: 번역 없음, 키워드·칩 빈 배열 (null 아님)
                .andExpect(jsonPath("$.items[0].titleKo").isEmpty())
                .andExpect(jsonPath("$.items[0].keywords").isEmpty())
                .andExpect(jsonPath("$.items[0].indicators").isEmpty());
    }

    @Test
    @WithMockUser
    void getNews_보강완료_번역_키워드_지표칩() throws Exception {
        LocalDate day = LocalDate.of(2026, 10, 7);
        NewsFeed feed = NewsFeed.builder()
                .source("HackerNews")
                .category(NewsCategory.IT)
                .title("Nvidia expands chip supply")
                .url("https://example.com/2")
                .publishedAt(day.atTime(10, 0))
                .build();
        ReflectionTestUtils.setField(feed, "titleKo", "엔비디아, 반도체 공급 확대");
        ReflectionTestUtils.setField(feed, "keywords", "[\"반도체\",\"엔비디아\"]");

        when(newsService.getLatestByCategoryCacheable(NewsCategory.IT))
                .thenReturn(CacheResult.hit(List.of(feed)));
        when(marketContextService.load(any(), any())).thenReturn(MarketContextService.Lookup.of(Map.of(
                "NASDAQ_PROXY", Map.of(day.minusDays(1), 500.0, day, 505.0))));

        mockMvc.perform(get("/v1/market/news?category=IT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].titleKo").value("엔비디아, 반도체 공급 확대"))
                .andExpect(jsonPath("$.items[0].keywords[0]").value("반도체"))
                .andExpect(jsonPath("$.items[0].indicators[0].label").value("QQQ"))
                .andExpect(jsonPath("$.items[0].indicators[0].changeRate").value(1.0));
    }

    @Test
    @WithMockUser
    void getNews_잘못된카테고리_400() throws Exception {
        mockMvc.perform(get("/v1/market/news?category=INVALID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getNews_카테고리누락_400() throws Exception {
        mockMvc.perform(get("/v1/market/news"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getCommonKeywords_기본1일() throws Exception {
        when(newsKeywordService.commonKeywords(1)).thenReturn(new CommonKeywordsResponse(
                1, LocalDateTime.of(2026, 10, 8, 12, 0),
                List.of(new CommonKeywordsResponse.KeywordCount("반도체", 3, 2))));

        mockMvc.perform(get("/v1/market/news/keywords"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keywords[0].keyword").value("반도체"))
                .andExpect(jsonPath("$.keywords[0].tech").value(3))
                .andExpect(jsonPath("$.keywords[0].economy").value(2));
    }

    @Test
    @WithMockUser
    void getKeywordTrend_키워드누락_400() throws Exception {
        mockMvc.perform(get("/v1/market/news/keywords/trend"))
                .andExpect(status().isBadRequest());
    }

}
