import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { collectionApi } from '../api'
import type { CollectionRunsParams } from '../types'

/** 실행 로그 한 페이지 — 필터·페이지를 바꿔도 이전 표를 유지한 채 교체 (깜빡임 방지) */
export function useCollectionRunsQuery(params: CollectionRunsParams) {
    return useQuery({
        queryKey: ['stat', 'collection', 'runs', params],
        queryFn: () => collectionApi.runs(params),
        staleTime: 60_000,
        placeholderData: keepPreviousData,
    })
}

/** 작업별 일간 요약 */
export function useCollectionDailyQuery(days = 30) {
    return useQuery({
        queryKey: ['stat', 'collection', 'daily', days],
        queryFn: () => collectionApi.daily(days),
        staleTime: 60_000,
    })
}
