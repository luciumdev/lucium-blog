import type { IndexCode, IndexSeries } from '@/features/market/types.ts'
import type { IndicesHistoryResponse } from '@/features/market/types.ts'

/**
 * 멀티라인 차트 전용 공통 시계열 형태.
 *
 * 데이터 소스(KIS 5일 bars / index_bars 히스토리 API)를 어댑터로 흡수해서
 * 차트 컴포넌트는 이 타입에만 의존한다. 소스가 바뀌어도 차트 코드는 불변.
 *
 * points 는 항상 과거 → 최신 오름차순(왼쪽이 과거). rebase 계산이
 * "기준일(가장 과거) = 100" 을 전제하므로 정렬 방향을 어댑터에서 고정한다.
 */
export interface IndexTimePoint {
    date: string   // YYYY-MM-DD
    close: number
}

export interface IndexTimeSeries {
    code: IndexCode
    displayName: string
    points: IndexTimePoint[]   // 과거 → 최신 (오름차순)
}

/**
 * 폴백 소스: KIS 실시간 응답의 5일 bars.
 *
 * types.ts 주석대로 IndexSeries.bars 는 최신순(내림차순)이므로 reverse 해서
 * 과거→최신으로 뒤집는다. latest === null(부분 실패) 시리즈는 그대로 통과하되
 * points 가 비어 차트 쪽 rebase100 에서 걸러진다.
 */
export function fromBars(series: IndexSeries[]): IndexTimeSeries[] {
    return series.map((s) => ({
        code: s.code,
        displayName: s.displayName,
        points: [...s.bars]
            .reverse()
            .map((b) => ({ date: b.date, close: b.close })),
    }))
}

/**
 * 주 소스: index_bars 히스토리 API 응답.
 *
 * 백엔드가 이미 (date, close) 로 정규화 + 과거→최신 정렬해서 내려주므로
 * 필드 매핑만 한다. code 는 서버가 문자열로 주지만 IndexCode 유니온과 값이 1:1이라
 * 그대로 캐스팅(서버 enum name === 프론트 IndexCode 리터럴).
 */
export function fromSnapshot(res: IndicesHistoryResponse): IndexTimeSeries[] {
    return res.series.map((s) => ({
        code: s.code as IndexCode,
        displayName: s.displayName,
        points: s.points.map((p) => ({ date: p.date, close: p.close })),
    }))
}

/**
 * 기준일(가장 과거 = points[0]) 을 100 으로 리베이싱.
 *
 * 절대값 스케일이 다른 지수(KOSPI ~2500 vs KOSDAQ ~700)를 한 차트에 겹칠 때
 * 등락률 기준으로 비교 가능하게 만든다. 기울기 = 누적 등락률.
 *
 * 방어: 빈 시리즈, 기준값 0/음수(휴장/결측)는 rebase 불가 → 빈 배열로 축소해
 * 차트에서 자연히 제외된다.
 */
export function rebase100(series: IndexTimeSeries[]): IndexTimeSeries[] {
    return series
        .map((s) => {
            if (s.points.length < 2) return { ...s, points: [] }
            const base = s.points[0].close
            if (!(base > 0)) return { ...s, points: [] }
            return {
                ...s,
                points: s.points.map((p) => ({
                    date: p.date,
                    close: (p.close / base) * 100,
                })),
            }
        })
        .filter((s) => s.points.length >= 2)
}
