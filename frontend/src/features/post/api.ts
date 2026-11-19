import { http } from '@/shared/api/http'
import type { Page } from '@/shared/api/types'
import type {
    PostSummary,
    PostDetail,
    PostCreateInput,
    PostUpdateInput,
    AdminListParams,
} from './types'

export interface GetPostParams {
    category?: string
    tag?: string
    page?: number
    size?: number
}

export const postApi = {
    /** GET /v1/posts — public, PUBLISHED만 */
    list: (params: GetPostParams = {}) =>
        http.get<Page<PostSummary>>('/v1/posts', {
            query: {
                category: params.category,
                tag: params.tag,
                page: params.page,
                size: params.size,
            },
        }),

    /** GET /v1/posts/admin/all — admin, DRAFT 포함 */
    listAdmin: (params: AdminListParams = {}) =>
        http.get<Page<PostSummary>>('/v1/posts/admin/all', {
            query: {
                page: params.page,
                size: params.size,
                status: params.status,
                keyword: params.keyword,
            },
        }),

    /** GET /v1/posts/{publicId} */
    detail: (publicId: string) =>
        http.get<PostDetail>(`/v1/posts/${publicId}`),

    /** POST /v1/posts — admin */
    create: (input: PostCreateInput) =>
        http.post<PostDetail>('/v1/posts', input),

    /** PATCH /v1/posts/{publicId} — admin (status 변경 X) */
    update: (publicId: string, input: PostUpdateInput) =>
        http.patch<PostDetail>(`/v1/posts/${publicId}`, input),

    /** DELETE /v1/posts/{publicId} — admin */
    remove: (publicId: string) =>
        http.del<void>(`/v1/posts/${publicId}`),

    /** PATCH /v1/posts/{publicId}/publish?isPublish=... — admin */
    togglePublish: (publicId: string, publish: boolean) =>
        http.patch<void>(
            `/v1/posts/${publicId}/publish`,
            undefined,
            { query: { isPublish: publish } },
        ),

    /** GET /v1/posts/admin/{publicId} — admin (DRAFT 포함) */
    detailAdmin: (publicId: string) =>
        http.get<PostDetail>(`/v1/posts/admin/${publicId}`),
}