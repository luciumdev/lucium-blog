import { http } from '@/shared/api/http'
import type { Page } from '@/shared/api/types'
import type { SearchHit } from './types'

export const searchApi = {
    /**
     * GET /v1/search?keyword=...
     * 빈 검색어는 백엔드가 빈 페이지 반환하므로 클라이언트에서 enabled로 막음.
     */
    search: (keyword: string, page = 0, size = 10) =>
        http.get<Page<SearchHit>>('/v1/search', {
            query: { q: keyword, page, size },
        }),
}