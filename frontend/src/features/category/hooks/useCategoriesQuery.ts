import { useQuery } from '@tanstack/react-query'
import { categoryApi } from '@/features/category/api.ts'

/**
 * 카테고리 전체 목록 — name → id 매핑에 사용.
 */
export function useCategoriesQuery() {
    return useQuery({
        queryKey: ['categories'],
        queryFn: categoryApi.list,
        staleTime: 10 * 60_000, // 10분
        gcTime: 30 * 60_000,
    })
}