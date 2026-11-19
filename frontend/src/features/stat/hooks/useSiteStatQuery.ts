import { useQuery } from '@tanstack/react-query'
import { statApi } from '../api'

/** 메인 프로필 카드 — 조회수는 자주 변하지만 초 단위일 필요는 없음 */
export function useSiteStatQuery() {
    return useQuery({
        queryKey: ['stat', 'site'],
        queryFn: statApi.site,
        staleTime: 60_000,
    })
}
