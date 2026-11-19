/**
 * 카테고리 색 — 글 목록 배지 등.
 * 키는 slug(dev/infra/project/log)와 이름(개발/인프라/프로젝트/로그) 둘 다 받음
 * (PostSummary 는 categoryName 만 내려주기 때문).
 *
 * fg 는 흰 배경 위 작은 글씨(12px)로 읽힐 수 있게 원색보다 한 단계 진하게 맞춤.
 */
export interface CategoryColor {
    /** 글자 */
    fg: string
    /** 배지 배경 */
    bg: string
}

const DEV: CategoryColor = { fg: '#2563C9', bg: '#E8F0FC' }
const INFRA: CategoryColor = { fg: '#0E8A5A', bg: '#E3F5EC' }
const PROJECT: CategoryColor = { fg: '#A86A00', bg: '#FBF0DA' }
const LOG: CategoryColor = { fg: '#C0446A', bg: '#FBE8EE' }
const FALLBACK: CategoryColor = { fg: '#4D4D4D', bg: '#F2F2F2' }

const MAP: Record<string, CategoryColor> = {
    dev: DEV,
    개발: DEV,
    infra: INFRA,
    인프라: INFRA,
    project: PROJECT,
    프로젝트: PROJECT,
    log: LOG,
    로그: LOG,
}

export function getCategoryColor(slugOrName: string | null | undefined): CategoryColor {
    if (!slugOrName) return FALLBACK
    return MAP[slugOrName.trim().toLowerCase()] ?? MAP[slugOrName.trim()] ?? FALLBACK
}

/** 메인 섹션 포인트 색 (카드 제목 밑 짧은 선 · 라벨) */
export const SECTION_ACCENT = {
    posts: '#0077C8',
    market: '#0E8A5A',
    release: '#7B61D9',
} as const