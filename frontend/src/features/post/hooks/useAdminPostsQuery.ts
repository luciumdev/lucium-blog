import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { postApi } from '../api'
import type { AdminListParams } from '../types'

export function useAdminPostsQuery(params: AdminListParams = {}) {
    return useQuery({
        queryKey: ['posts', 'admin', params],
        queryFn: () => postApi.listAdmin(params),
        placeholderData: keepPreviousData, // 페이지 전환 시 깜빡임 방지
    })
}