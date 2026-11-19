export type PostStatus = 'DRAFT' | 'PUBLISHED'

/** 본문 형식 — MARKDOWN: 에디터 작성 / HTML: 원본 파일 그대로 게시(iframe 렌더) */
export type ContentFormat = 'MARKDOWN' | 'HTML'

/**
 * GET /v1/posts/{publicId} 응답.
 * 백엔드 PostDetailResponse 매핑.
 */
export interface PostSummary {
    publicId: string
    title: string
    summary: string | null
    thumbnail: string | null
    categoryName: string | null
    tags: string[]
    viewCount: number
    status: PostStatus
    publishedAt: string | null
    createdAt: string
}

/**
 * GET /v1/posts/{publicId} 응답.
 * 백엔드 PostDetailResponse 매핑.
 */
export interface PostDetail {
    publicId: string
    title: string
    content: string
    /** V11 이전 캐시 응답에는 없을 수 있음 → 없으면 MARKDOWN 취급 */
    contentFormat?: ContentFormat
    summary: string | null
    thumbnail: string | null
    categoryId: number | null
    categoryName: string | null
    tags: string[]
    viewCount: number
    status: PostStatus
    publishedAt: string | null
    createdAt: string
    updatedAt: string
    alreadyRead: boolean
}

/**
 * POST /v1/posts 요청.
 * 백엔드 PostCreateRequest 매핑.
 *  - status null → DRAFT 저장
 *  - status 'PUBLISHED' → 발행 + PostPublishedEvent 발행
 *  - contentFormat null → MARKDOWN
 */
export interface PostCreateInput {
    title: string
    content: string
    contentFormat?: ContentFormat
    summary?: string | null
    thumbnail?: string | null
    categoryId?: number | null
    tags?: string[]
    status?: PostStatus | null
}

/**
 * PATCH /v1/posts/{publicId} 요청.
 * 백엔드 PostUpdateRequest 매핑 — status 필드 없음.
 * 발행 토글은 별도 endpoint(togglePublish) 사용.
 * contentFormat null → 기존 형식 유지
 */
export interface PostUpdateInput {
    title: string
    content: string
    contentFormat?: ContentFormat
    summary?: string | null
    thumbnail?: string | null
    categoryId?: number | null
    tags?: string[]
}

/**
 * 어드민 페이지네이션 파라미터 — DRAFT 포함.
 * 백엔드 기본: size=20, sort=createdAt DESC.
 */
export interface AdminListParams {
    page?: number
    size?: number
    status?: PostStatus
    keyword?: string
}
