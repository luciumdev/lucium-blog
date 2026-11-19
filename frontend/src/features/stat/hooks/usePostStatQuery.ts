import { useQuery } from '@tanstack/react-query'
import { statApi } from '@/features/stat/api.ts'

/**
 * 대시보드 게시글 통계.
 * 자주 안 변하니 staleTime 넉넉히. 관리자 인증 필요 엔드포인트.
 */
export function usePostStatQuery() {
    return useQuery({
        queryKey: ['stat', 'posts', 'summary'],
        queryFn: () => statApi.postSummary(),
        staleTime: 60_000, // 1분
    })
}