import {useQuery} from "@tanstack/react-query";
import { postApi, type GetPostParams } from '../api'

export function usePostsQuery(params: GetPostParams = {}) {
    return useQuery({
        queryKey: ['posts', 'list', params],
        queryFn: () => postApi.list(params),
    })
}