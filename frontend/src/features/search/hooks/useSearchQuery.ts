import { useQuery } from '@tanstack/react-query'
import { searchApi } from '../api'

export function useSearchQuery(keyword: string, page = 0) {
    const trimmed = keyword.trim()
    return useQuery({
        queryKey: ['search', trimmed, page],
        queryFn: () => searchApi.search(trimmed, page),
        enabled: trimmed.length >= 2,
        staleTime: 30 * 1000,
    })
}