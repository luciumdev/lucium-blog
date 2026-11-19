
export type NewsCategory = 'IT' | 'ECONOMY' | 'AI'


export interface ForexRate {
    currencyCode: string;
    currencyName: string;
    rate: number;
    previousRate?: number | null
    change?: number | null
    changeRate?: number | null
}

export interface ForexResponse {
    snapshotAt: string;
    rates: ForexRate[];
}

export interface BriefingResponse {
    briefing: string;
}

/**
 * 기사 게시일(KST) 기준 지표 등락 — 인과가 아니라 맥락.
 * code: NASDAQ_PROXY | KOSPI | USD, label: QQQ | KOSPI | 원/달러
 */
export interface IndicatorChip {
    code: string
    label: string
    changeRate: number
}

export interface NewsItem {
    source: string
    title: string
    /** HN 영어 제목 번역. 보강 전·경제 뉴스는 null */
    titleKo: string | null
    url: string
    summary: string | null
    publishedAt: string
    /** 정규화 키워드 (보강 전이면 빈 배열) */
    keywords: string[]
    indicators: IndicatorChip[]
}

export interface NewsResponse {
    category: NewsCategory
    count: number
    items: NewsItem[]
}

/** 테크·경제 공통 키워드 (ES terms 집계) */
export interface KeywordCount {
    keyword: string
    tech: number
    economy: number
}

export interface CommonKeywordsResponse {
    days: number
    from: string
    keywords: KeywordCount[]
}

/** 키워드 일별 기사 수 (ES date_histogram). date = YYYY-MM-DD, 과거→오늘 */
export interface KeywordTrendResponse {
    keyword: string
    series: { date: string; count: number }[]
}

/**
 * Indices (코스피, 코스닥, 나스닥, sp500)
 */
export type IndexCode = 'KOSPI' | 'KOSDAQ' | 'NASDAQ_PROXY' | 'SP500_PROXY'

/**
 * date = YYYY-MM-DD
 */
export interface IndexBar {
    date: string
    open: number
    high: number
    low: number
    close: number
    change: number
    changeRate: number
    volume: number
}

/**
 * displayName = "코스피", "나스닥(QQQ ETF)" 등
 * bars = 최신순정렬
 */
export interface IndexSeries {
    code: IndexCode
    displayName: string
    latest: IndexBar | null
    bars: IndexBar[]
}

/**
 * fetchedAt = ISO datetime
 */
export interface IndicesData {
    fetchedAt: string
    series: IndexSeries[]
}

////////////////////////////
// 원자재
////////////////////////////
export type CommodityCode = 'GOLD' | 'SILVER' | 'WTI' | 'COPPER'

export interface CommodityPrice {
    code: string        // "GOLD" | "SILVER" | "WTI" | "COPPER"
    name: string
    price: number
    changeValue: number
    changePercent: number
    currency: string
    unit: string
}

export interface CommoditiesResponse {
    snapshotAt: string
    prices: CommodityPrice[]
}
/**
 * 지수 시계열 히스토리 응답 (index_bars 정규화 테이블 조회).
 * points 는 과거→최신 오름차순. 리베이싱 차트용.
 */
export interface IndexHistoryPoint {
    date: string   // YYYY-MM-DD
    close: number
}

export interface IndexHistorySeries {
    code: IndexCode          // 서버 enum name === IndexCode 리터럴
    displayName: string
    points: IndexHistoryPoint[]
}

export interface IndicesHistoryResponse {
    from: string             // YYYY-MM-DD (조회 하한)
    series: IndexHistorySeries[]
}
