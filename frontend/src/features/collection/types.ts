export type RunStatus = 'SUCCESS' | 'PARTIAL' | 'EMPTY' | 'FAILED'
export type CollectionJobCode = 'NEWS' | 'FOREX' | 'INDICES' | 'AI_JOURNAL' | 'COMMODITIES'
export type RunTrigger = 'SCHEDULED' | 'BOOTSTRAP'

/** GET /v1/stat/collection */
export interface CollectionStatus {
    /** 오늘(KST) 새로 저장된 테크·경제 기사 수 */
    todayArticles: number
    /** 오늘 번역된 기사 수 */
    todayTranslated: number
    /** 최근 7일 성공률(%) — EMPTY 제외. 기록이 없으면 null */
    successRate7d: number | null
    jobs: CollectionJobStatus[]
}

export interface CollectionJobStatus {
    job: CollectionJobCode
    label: string
    cycle: string
    lastRunAt: string | null
    lastStatus: RunStatus | null
    /** 과거 → 최신 */
    recent: RunStatus[]
}

/* ───── 실행 이력 (/status) ───── */

/** GET /v1/stat/collection/runs */
export interface CollectionRunPage {
    items: CollectionRunItem[]
    page: number
    size: number
    totalElements: number
    totalPages: number
}

export interface CollectionRunItem {
    id: number
    job: CollectionJobCode
    jobLabel: string
    trigger: RunTrigger
    status: RunStatus
    startedAt: string
    durationMs: number
    itemCount: number
    /** 공개용 사유 (분류·마스킹됨). 성공이면 없음 */
    reason?: string | null
    /** 원문 — 관리자 로그인 상태에서만 내려옴 */
    rawMessage?: string | null
}

export interface CollectionRunsParams {
    job?: CollectionJobCode
    status?: RunStatus
    page?: number
    size?: number
}

/** GET /v1/stat/collection/daily */
export interface CollectionDaily {
    days: number
    jobs: CollectionJobDaily[]
}

export interface CollectionJobDaily {
    job: CollectionJobCode
    label: string
    cycle: string
    totalRuns: number
    /** 기간 성공률(%) — EMPTY 제외. 집계 대상이 없으면 null */
    successRate: number | null
    avgDurationMs: number | null
    /** 과거 → 오늘 */
    daily: CollectionDay[]
}

export interface CollectionDay {
    date: string
    success: number
    partial: number
    empty: number
    failed: number
}
