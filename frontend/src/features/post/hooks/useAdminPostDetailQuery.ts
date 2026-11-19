import { useQuery } from '@tanstack/react-query'
import { postApi } from '../api'

export function useAdminPostDetailQuery(publicId: string | undefined) {
    return useQuery({
        queryKey: ['posts', 'admin-detail', publicId],
        queryFn: () => postApi.detailAdmin(publicId!),
        enabled: !!publicId,
    })
}