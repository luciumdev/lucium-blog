import { http } from '@/shared/api/http'
import type { PostSummary } from '@/features/post/types'

export interface PostStats {
    published: number
    draft: number
    total: number
}

/** GET /v1/stat/site — 공개. 메인 프로필 카드용 */
export interface SiteStats {
    publishedPosts: number
    /** 오늘(KST) 순 방문자 */
    todayVisitors: number
    /** 누적 순 방문자 */
    totalVisitors: number
}

export const statApi = {
    /** 대시보드 게시글 통계 — 인증 필요(관리자) */
    postSummary: () => http.get<PostStats>('/v1/stat/posts/summary'),

    /** 사이트 공개 통계 — 발행 글 수 · 오늘/누적 방문자 */
    site: () => http.get<SiteStats>('/v1/stat/site'),

    /** 방문 기록 — visitor_id 쿠키 기준. 204 */
    visit: () => http.post<void>('/v1/stat/visit'),

    /** 많이 읽은 글 — 공개 */
    popularPosts: (size = 5) =>
        http.get<PostSummary[]>('/v1/stat/posts/popular', { query: { size } }),
}
