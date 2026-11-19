package com.ysh.market.client;


import com.ysh.IntegrationTest;
import com.ysh.domain.market.client.KoreaEximForexClient;
import com.ysh.domain.market.domain.ForexRates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@ActiveProfiles("test")  // KoreaEximForexClient 빈 활성화
@Testcontainers
class KoreaEximForexClientIntegrationTest extends IntegrationTest {

    @Container
    static MockServerContainer mockServer = new MockServerContainer(
            DockerImageName.parse("mockserver/mockserver:5.15.0"));

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry r) {
        r.add("market.forex.base-url",
                () -> "http://" + mockServer.getHost() + ":" + mockServer.getServerPort());
        r.add("market.forex.api-key", () -> "test-key");
    }

    @Autowired
    KoreaEximForexClient client;

    MockServerClient mock;

    @BeforeEach
    void setUp() {
        mock = new MockServerClient(mockServer.getHost(), mockServer.getServerPort());
        mock.reset();
    }

    @Test
    void fetch_정상응답_파싱성공() {
        // given
        mock.when(request().withPath("/site/program/financial/exchangeJSON"))
                .respond(response()
                        .withStatusCode(200)
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody("""
                    [
                      {"result":1,"cur_unit":"USD","cur_nm":"미국 달러","deal_bas_r":"1,380.50"},
                      {"result":1,"cur_unit":"JPY(100)","cur_nm":"일본 옌(100엔)","deal_bas_r":"920.15"},
                      {"result":1,"cur_unit":"GBP","cur_nm":"영국 파운드","deal_bas_r":"1,750.00"}
                    ]
                    """));

        // when
        ForexRates result = client.fetch();

        // then — TARGET_CURRENCIES 필터로 GBP 제외, USD/JPY만
        assertThat(result.rates()).hasSize(2);
        assertThat(result.rates())
                .extracting("currencyCode")
                .containsExactlyInAnyOrder("USD", "JPY(100)");
        assertThat(result.rates().get(0).dealBasR()).isEqualTo(1380.50);
    }

    @Test
    void fetch_주말빈응답_빈결과반환() {
        mock.when(request().withPath("/site/program/financial/exchangeJSON"))
                .respond(response().withStatusCode(200).withBody("[]"));

        ForexRates result = client.fetch();

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void fetch_인증실패_빈결과반환() {
        // result=3 = 인증코드 오류
        mock.when(request().withPath("/site/program/financial/exchangeJSON"))
                .respond(response()
                        .withStatusCode(200)
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody("""
                    [{"result":3,"cur_unit":"","cur_nm":"","deal_bas_r":"0"}]
                    """));

        ForexRates result = client.fetch();

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void fetch_5xx_빈결과반환() {
        mock.when(request().withPath("/site/program/financial/exchangeJSON"))
                .respond(response().withStatusCode(503));

        ForexRates result = client.fetch();

        assertThat(result.isEmpty()).isTrue();  // 예외 안 던지고 fallback
    }
}