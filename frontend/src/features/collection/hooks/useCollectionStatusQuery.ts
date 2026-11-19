import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { collectionApi } from '../api'

/** 메인 "데이터 수집" 카드 */
export function useCollectionStatusQuery() {
    return useQuery({
        queryKey: ['stat', 'collection'],
        queryFn: collectionApi.status,
        staleTime: 60_000,
        refetchInterval: 5 * 60_000,
        placeholderData: keepPreviousData,
    })
}
