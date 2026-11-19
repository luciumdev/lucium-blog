import { useQuery } from '@tanstack/react-query'
import { marketApi } from '@/features/market/api.ts'

/**
 * 지수 시계열 히스토리 조회 (index_bars 병합 결과).
 *
 * days 별로 queryKey 를 분리해 기간 전환 시 각각 캐싱. 히스토리는 하루 1회
 * 갱신(수집 사이클)이라 staleTime 을 길게 잡아 불필요한 재요청을 막는다.
 */
export function useIndicesHistoryQuery(days = 30) {
    return useQuery({
        queryKey: ['market', 'indices', 'history', days],
        queryFn: () => marketApi.history(days),
        staleTime: 10 * 60_000,
        refetchInterval: 30 * 60_000,
    })
}
