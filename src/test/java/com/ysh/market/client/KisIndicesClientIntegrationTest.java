package com.ysh.market.client;

import com.ysh.domain.market.client.kis.KisDomesticIndicesClient;
import com.ysh.domain.market.client.kis.KisOverseasIndicesClient;
import com.ysh.domain.market.domain.IndexBar;
import com.ysh.domain.market.domain.IndexCode;
import com.ysh.domain.market.domain.IndexSeries;
import com.ysh.domain.market.client.kis.KisAuthService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KIS 국내/해외 지수 클라이언트 단발 검증 — 실제 KIS API 호출.
 *
 * <p>두 클라이언트가 {@code @Profile("prod")}로 local 프로필에서 빈 등록 안 되므로,
 * 테스트 안에서 직접 인스턴스를 생성. 의존성(RestClient, KisAuthService)은 Spring 컨텍스트에서 가져옴.
 *
 * <p>실행 조건:
 * <ul>
 *   <li>환경변수 {@code KIS_APP_KEY}, {@code KIS_APP_SECRET}</li>
 *   <li>Redis 가동 (KisAuthService 토큰 캐시용)</li>
 *   <li>한국 영업일 — 휴장일에는 시계열 길이가 짧을 수 있음</li>
 *   <li>해외(QQQ/SPY)는 미국 시장 종료 후 수집되므로 한국 시간 06:00 이후 권장</li>
 * </ul>
 */
@Disabled("실제 KIS API 호출 — 로컬 수동 실측 전용 (QQQ=NAS, SPY=AMS 확정). CI 제외.")
@SpringBootTest
@ActiveProfiles("local")
//@Disabled("KIS 실제 API 호출 — 수동 실행 전용")
class KisIndicesClientIntegrationTest {

    private static final long INTER_TEST_DELAY_MS = 1100L;

    @Autowired
    @Qualifier("kisRestClient")
    private RestClient kisRestClient;

    @Autowired
    private KisAuthService kisAuthService;

    @Value("${market.kis.app-key}")
    private String appKey;

    @Value("${market.kis.app-secret}")
    private String appSecret;

    @Value("${market.kis.indices.series-depth:7}")
    private int seriesDepth;

    private KisDomesticIndicesClient domesticClient;
    private KisOverseasIndicesClient overseasClient;


    @BeforeEach
    void setUp() {
        domesticClient = new KisDomesticIndicesClient(
                kisRestClient, kisAuthService, appKey, appSecret, seriesDepth);
        overseasClient = new KisOverseasIndicesClient(
                kisRestClient, kisAuthService, appKey, appSecret, seriesDepth);
    }
    @AfterEach
    void throttle() throws InterruptedException {
        // KIS 모의 환경 시세 조회는 초당 ~1회 (EGW00201). 다음 테스트 호출 안전 거리 확보.
        Thread.sleep(INTER_TEST_DELAY_MS);
    }
    @Test
    @DisplayName("KOSPI (0001) 시계열 fetch")
    void kospi() {
        verifySeries(domesticClient.fetch(IndexCode.KOSPI), IndexCode.KOSPI);
    }

    @Test
    @DisplayName("KOSDAQ (1001) 시계열 fetch")
    void kosdaq() {
        verifySeries(domesticClient.fetch(IndexCode.KOSDAQ), IndexCode.KOSDAQ);
    }



    @Test
    @DisplayName("QQQ (NASDAQ 프록시, EXCD=NAS) 시계열 fetch")
    void qqq() {
        verifySeries(overseasClient.fetch(IndexCode.NASDAQ_PROXY), IndexCode.NASDAQ_PROXY);
    }

    @Test
    @DisplayName("SPY (S&P 500 프록시, EXCD=AMS) 시계열 fetch — KIS 명시 X 실측 확정")
    void spy() {
        verifySeries(overseasClient.fetch(IndexCode.SP500_PROXY), IndexCode.SP500_PROXY);
    }

    /**
     * 시계열 응답 합리성 검증.
     *
     * <ol>
     *   <li>비어있지 않음 (rt_cd:"0" + 빈 output2 debug trap 차단)</li>
     *   <li>code 일치 + bars 길이 1~seriesDepth</li>
     *   <li>latest == bars[0]</li>
     *   <li>OHLC 관계: low ≤ open/close ≤ high</li>
     *   <li>시계열 정렬: 최신순 (bars[i-1].date > bars[i].date)</li>
     *   <li>부호 일관성: 종가 상승이면 changeRate 양수, 하락이면 음수</li>
     * </ol>
     */
    private void verifySeries(IndexSeries series, IndexCode expectedCode) {
        // 1. 비어있지 않음
        assertThat(series).isNotNull();
        assertThat(series.code()).isEqualTo(expectedCode);
        assertThat(series.isEmpty())
                .as("[%s] 빈 응답이면 안됨 — auth/quota/EXCD mismatch 의심", expectedCode)
                .isFalse();

        // 2. 길이
        assertThat(series.bars()).hasSizeBetween(1, seriesDepth);

        // 3. latest alias
        assertThat(series.latest()).isEqualTo(series.bars().get(0));

        IndexBar latest = series.latest();
        System.out.println("코드 = " + expectedCode + " 최근일 = " + latest.date() + "O= " + latest.open() + "H= " + latest.high() +
                "L= " + latest.low() + "C= " + latest.close() + "V= " + latest.volume());

        // 4. OHLC 관계 — 가격이 양수이고 high ≥ low 등
        assertThat(latest.close()).as("[%s] close > 0", expectedCode).isPositive();
        assertThat(latest.high()).as("[%s] high ≥ low", expectedCode)
                .isGreaterThanOrEqualTo(latest.low());
        assertThat(latest.high()).as("[%s] high ≥ close", expectedCode)
                .isGreaterThanOrEqualTo(latest.close());
        assertThat(latest.low()).as("[%s] low ≤ close", expectedCode)
                .isLessThanOrEqualTo(latest.close());

        // 5. 시계열 정렬 (최신순)
        for (int i = 1; i < series.bars().size(); i++) {
            assertThat(series.bars().get(i - 1).date())
                    .as("[%s] 최신순 정렬", expectedCode)
                    .isAfter(series.bars().get(i).date());
        }

        // 6. 부호 일관성 — bars 2개 이상 있을 때
        if (series.bars().size() >= 2) {
            IndexBar today = series.bars().get(0);
            IndexBar yesterday = series.bars().get(1);

            if (today.close() > yesterday.close() * 1.0001) {
                // 0.01% 이상 상승
                assertThat(today.changeRate())
                        .as("[%s] 종가 상승(%s→%s) 시 changeRate 양수여야",
                                expectedCode, yesterday.close(), today.close())
                        .isPositive();
            } else if (today.close() < yesterday.close() * 0.9999) {
                // 0.01% 이상 하락
                assertThat(today.changeRate())
                        .as("[%s] 종가 하락(%s→%s) 시 changeRate 음수여야",
                                expectedCode, yesterday.close(), today.close())
                        .isNegative();
            }
            // 보합 구간(±0.01%)은 0이거나 미세 → 검증 생략
        }
    }
}
