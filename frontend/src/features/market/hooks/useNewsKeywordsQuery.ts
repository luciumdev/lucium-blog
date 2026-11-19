import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { marketApi } from '../api'

/** 테크·경제 공통 키워드. 뉴스 파이프라인 주기(30분)에 맞춰 갱신 */
export function useNewsKeywordsQuery(days = 1) {
    return useQuery({
        queryKey: ['market', 'news', 'keywords', days],
        queryFn: () => marketApi.newsKeywords(days),
        staleTime: 5 * 60_000,
        refetchInterval: 30 * 60_000,
        placeholderData: keepPreviousData,
    })
}
