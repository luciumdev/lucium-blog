import { useQuery } from '@tanstack/react-query'
import { marketApi } from '../api'
import type { NewsCategory } from '../types'

export function useNewsQuery(category: NewsCategory) {
    return useQuery({
        queryKey: ['market', 'news', category],
        queryFn: () => marketApi.news(category),
        staleTime: 60_000,
        refetchInterval: 5 * 60_000,    
    })
}