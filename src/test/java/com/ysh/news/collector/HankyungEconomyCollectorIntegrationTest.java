package com.ysh.news.collector;

import com.ysh.IntegrationTest;
import com.ysh.domain.news.collector.HankyungEconomyCollector;
import com.ysh.domain.news.entity.NewsCategory;
import com.ysh.domain.news.entity.NewsFeed;
import com.ysh.domain.news.repository.NewsFeedRepository;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@ActiveProfiles("test")
@Testcontainers
class HankyungEconomyCollectorIntegrationTest extends IntegrationTest {

    @Container
    static MockServerContainer mockServer = new MockServerContainer(
            DockerImageName.parse("mockserver/mockserver:5.15.0"));

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry r) {
        String mockUrl = "http://" + mockServer.getHost() + ":" + mockServer.getServerPort();
        r.add("news.hankyung.base-url",   () -> mockUrl);
        r.add("news.hackernews.base-url", () -> mockUrl);
    }

    @Autowired
    HankyungEconomyCollector collector;
    @Autowired
    NewsFeedRepository newsFeedRepository;

    MockServerClient mock;

    @BeforeEach
    void setUp() {
        mock = new MockServerClient(mockServer.getHost(), mockServer.getServerPort());
        mock.reset();
        newsFeedRepository.deleteAll();
    }

    @Test
    void collect_정상RSS_파싱성공() {
        String rss = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <title>한국경제 경제</title>
                <item>
                  <title><![CDATA[한국은행 기준금리 동결]]></title>
                  <link>https://www.hankyung.com/article/1</link>
                  <description><![CDATA[<p>금융통화위원회는 만장일치로</p>]]></description>
                  <pubDate>Wed, 04 May 2026 12:00:00 +0900</pubDate>
                </item>
              </channel>
            </rss>
            """;

        mock.when(request().withPath("/feed/economy"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_XML_UTF_8)
                        .withBody(rss));

        List<NewsFeed> result = collector.collect();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("한국은행 기준금리 동결");
        assertThat(result.get(0).getCategory()).isEqualTo(NewsCategory.ECONOMY);
        assertThat(result.get(0).getSource()).isEqualTo("Hankyung");
        // HTML 정제 검증
        assertThat(result.get(0).getSummary()).doesNotContain("<p>");
        assertThat(result.get(0).getSummary()).contains("금융통화위원회");
    }

    @Test
    void collect_빈응답_빈결과반환() {
        mock.when(request().withPath("/feed/economy"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_XML_UTF_8)
                        .withBody(""));

        assertThat(collector.collect()).isEmpty();
    }

    @Test
    void collect_5xx_빈결과반환() {
        mock.when(request().withPath("/feed/economy"))
                .respond(response().withStatusCode(503));

        assertThat(collector.collect()).isEmpty();
    }

    @Test
    void collect_잘못된XML_빈결과반환() {
        mock.when(request().withPath("/feed/economy"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_XML_UTF_8)
                        .withBody("<not-valid-xml>"));

        assertThat(collector.collect()).isEmpty();
    }

    @Test
    void collect_pubDate없음_현재시각으로fallback() {
        String rss = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <item>
                  <title>제목</title>
                  <link>https://example.com/no-date</link>
                </item>
              </channel>
            </rss>
            """;

        mock.when(request().withPath("/feed/economy"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_XML_UTF_8)
                        .withBody(rss));

        List<NewsFeed> result = collector.collect();
        assertThat(result).hasSize(1);
        // 현재 시각 ±1분
        assertThat(result.get(0).getPublishedAt())
                .isAfter(LocalDateTime.now().minusMinutes(1));
    }
}