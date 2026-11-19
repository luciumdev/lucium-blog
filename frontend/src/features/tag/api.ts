import { http } from '@/shared/api/http'

export interface Tag {
    id: number
    name: string
    slug: string
    postCount: number
}

export const tagApi = {
    /** 사이드바용 태그 목록 — postCount 내림차순 (백엔드 기준) */
    list: () => http.get<Tag[]>('/v1/tags'),
}
