import { useQuery } from '@tanstack/react-query'
import { postApi } from '../api'

export function usePostDetailQuery(publicId: string | undefined) {
    return useQuery({
        queryKey: ['posts', 'detail', publicId],
        queryFn: () => postApi.detail(publicId!),
        enabled: !!publicId,
        staleTime: 0,
    })
}