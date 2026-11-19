import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { marketApi } from '../api'

/** 키워드 일별 기사 수. keyword 가 없으면 호출하지 않음 */
export function useKeywordTrendQuery(keyword: string | null, days = 5) {
    return useQuery({
        queryKey: ['market', 'news', 'keyword-trend', keyword, days],
        queryFn: () => marketApi.keywordTrend(keyword as string, days),
        enabled: !!keyword,
        staleTime: 5 * 60_000,
        placeholderData: keepPreviousData,
    })
}
