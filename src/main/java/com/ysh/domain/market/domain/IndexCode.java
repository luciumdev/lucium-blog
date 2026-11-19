package com.ysh.domain.market.domain;

/**
 * 4개 시장 지수 식별자.
 *
 * KOSPI/KOSDAQ는 KIS 국내 일자별 지수 API의 FID_INPUT_ISCD로,
 * NASDAQ_PROXY/SP500_PROXY는 ETF 프록시(QQQ/SPY)로 KIS 해외 dailyprice API의 SYMB로 사용.
 *
 * 해외는 EXCD(거래소 코드)가 추가로 필요 — KIS는 종목-거래소 mismatch 시
 * silent fail(rt_cd "0" + 빈 output2)이라 정확히 매핑되어야 함:
 */
public enum IndexCode {
    KOSPI("0001", "코스피", Market.DOMESTIC, null),
    KOSDAQ("1001", "코스닥", Market.DOMESTIC, null),
    NASDAQ_PROXY("QQQ", "나스닥(QQQ ETF)", Market.OVERSEAS, "NAS"),
    SP500_PROXY("SPY", "S&P 500(SPY ETF)", Market.OVERSEAS, "AMS");

    private final String externalCode;
    private final String displayName;
    private final Market market;
    // OVERSEAS만 사용. DOMESTIC은 null
    private final String exchangeCode;

    IndexCode(String externalCode, String displayName, Market market, String exchangeCode) {
        this.externalCode = externalCode;
        this.displayName = displayName;
        this.market = market;
        this.exchangeCode = exchangeCode;
    }

    public String externalCode() { return externalCode; }
    public String displayName() { return displayName; }
    public Market market() { return market; }
    public String exchangeCode() { return exchangeCode; }

    public enum Market { DOMESTIC, OVERSEAS }
}
