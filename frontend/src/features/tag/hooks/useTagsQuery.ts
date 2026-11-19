import { useQuery } from '@tanstack/react-query'
import { tagApi } from '@/features/tag/api.ts'

/**
 * 태그 전체 목록 — 사이드바 태그 클라우드용.
 * postCount 내림차순, 거의 변하지 않음 → staleTime 길게.
 */
export function useTagsQuery() {
    return useQuery({
        queryKey: ['tags'],
        queryFn: tagApi.list,
        staleTime: 10 * 60_000, // 10분
        gcTime: 30 * 60_000,
    })
}
