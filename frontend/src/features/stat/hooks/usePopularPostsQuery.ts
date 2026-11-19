import { useQuery } from '@tanstack/react-query'
import { statApi } from '../api'

export function usePopularPostsQuery(size = 5) {
    return useQuery({
        queryKey: ['stat', 'popular', size],
        queryFn: () => statApi.popularPosts(size),
        staleTime: 5 * 60_000,
    })
}
