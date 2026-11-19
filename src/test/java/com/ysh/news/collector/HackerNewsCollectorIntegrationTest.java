package com.ysh.news.collector;

import com.ysh.IntegrationTest;
import com.ysh.domain.news.collector.HackerNewsCollector;
import com.ysh.domain.news.entity.NewsCategory;
import com.ysh.domain.news.entity.NewsFeed;
import com.ysh.domain.news.repository.NewsFeedRepository;
import com.ysh.domain.news.service.NewsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
@ActiveProfiles({"test"})
@Testcontainers
class HackerNewsCollectorIntegrationTest extends IntegrationTest {

    @Container
    static MockServerContainer mockServer = new MockServerContainer(
            DockerImageName.parse("mockserver/mockserver:5.15.0"));

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry r) {
        String mockUrl = "http://" + mockServer.getHost() + ":" + mockServer.getServerPort();
        r.add("news.hackernews.base-url", () -> mockUrl);
        r.add("news.hackernews.freshness-window", () -> "5000d");
        r.add("news.hankyung.base-url", () -> mockUrl);
    }

    @Autowired
    HackerNewsCollector collector;
    @Autowired NewsService newsService;

    @Autowired
    NewsFeedRepository newsFeedRepository;

    MockServerClient mock;

    @BeforeEach
    void setUp() {
        mock = new MockServerClient(mockServer.getHost(), mockServer.getServerPort());
        mock.reset();
        newsFeedRepository.deleteAll();
        // Hankyung 빈 응답 default
        mock.when(request().withPath("/feed/economy"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_XML_UTF_8)
                        .withBody("<rss version=\"2.0\"><channel></channel></rss>"));
    }

    @Test
    void collect_정상응답_필터링성공() {
        mock.when(request().withPath("/v0/topstories.json"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("[101, 102, 103]"));

        // 101: 정상 story
        mock.when(request().withPath("/v0/item/101.json"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("""
                {"id":101,"type":"story","title":"정상 글","url":"https://ok.com",
                 "score":100,"time":1700000000,"dead":false,"deleted":false}
                """));

        // 102: dead 글 — 필터됨
        mock.when(request().withPath("/v0/item/102.json"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("""
                {"id":102,"type":"story","title":"dead","url":"https://dead.com",
                 "score":200,"time":1700000000,"dead":true}
                """));

        // 103: 점수 미달 (threshold 50) — 필터됨
        mock.when(request().withPath("/v0/item/103.json"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("""
                {"id":103,"type":"story","title":"낮은점수","url":"https://low.com",
                 "score":10,"time":1700000000}
                """));

        List<NewsFeed> result = collector.collect();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("정상 글");
        assertThat(result.get(0).getCategory()).isEqualTo(NewsCategory.IT);
        assertThat(result.get(0).getSource()).isEqualTo("HackerNews");
    }

    @Test
    void collect_AskHN_url없음_필터됨() {
        mock.when(request().withPath("/v0/topstories.json"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("[401]"));
        mock.when(request().withPath("/v0/item/201.json"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("""
                {"id":201,"type":"story","title":"Ask HN: ...",
                 "score":300,"time":1700000000}
                """));

        assertThat(collector.collect()).isEmpty();
    }

    @Test
    void collect_topstories_5xx_빈결과반환() {
        mock.when(request().withPath("/v0/topstories.json"))
                .respond(response().withStatusCode(503));

        assertThat(collector.collect()).isEmpty();
    }

    @Test
    void collect_개별item_실패해도_나머지진행() {
        mock.when(request().withPath("/v0/topstories.json"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("[301, 302]"));
        // 301: 5xx 실패
        mock.when(request().withPath("/v0/item/301.json"))
                .respond(response().withStatusCode(503));

        // 302: 정상
        mock.when(request().withPath("/v0/item/302.json"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("""
                {"id":302,"type":"story","title":"정상","url":"https://ok.com",
                 "score":100,"time":1700000000}
                """));

        List<NewsFeed> result = collector.collect();
        assertThat(result).hasSize(1);
    }

    @Test
    void newsService_같은URL_두번수집_한번만INSERT() {
        mock.when(request().withPath("/v0/topstories.json"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("[401]"));
        mock.when(request().withPath("/v0/item/401.json"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("""
            {"id":401,"type":"story","title":"멱등성 테스트","url":"https://idem.com",
             "score":100,"time":1700000000}
            """));

        // 트랜잭션-외부API 분리 패턴 그대로. self-invocation trap 회피.
        // 1차: 새 URL INSERT
        List<NewsFeed> feeds1 = newsService.fetchAllFeeds();
        NewsService.CollectResult r1 = newsService.saveAll(feeds1);

        // 2차: 같은 URL → ON CONFLICT (url) DO NOTHING으로 skip
        List<NewsFeed> feeds2 = newsService.fetchAllFeeds();
        NewsService.CollectResult r2 = newsService.saveAll(feeds2);

        assertThat(r1.inserted()).isEqualTo(1);
        assertThat(r2.inserted()).isEqualTo(0);
        assertThat(newsFeedRepository.count()).isEqualTo(1);
    }
}
