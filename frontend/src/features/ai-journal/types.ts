export type AiNewsImportance = 'HIGH' | 'MEDIUM' | 'LOW'

export interface AiJournalDailyResponse {
    briefing: AiJournalBriefing
    failedSources: SourceFailure[]
    /** 브리핑 수집일 (YYYY-MM-DD, KST). 오늘이 아니면 이전 수집본 */
    journalDate: string | null
}

export interface SourceFailure {
    source: string
    reason: string
    failedAt: string
}

export interface AiNewsItem {
    source: string
    title: string
    summary: string
    important: AiNewsImportance
    category: string
    tags: string[]
    url: string
}

export interface AiHighlightCount {
    total: number
    high: number
    medium: number
    low: number
}

/** 백엔드는 snake_case (LLM 응답 형태 그대로 + @JsonProperty) */
export interface AiJournalBriefing {
    daily_summary: string
    highlight_count: AiHighlightCount
    items: AiNewsItem[]
    top_keywords: string[]
}

/** 백엔드 응답 wrapper — ApiResponse<T> */
export interface ApiResponse<T> {
    success: boolean
    data: T
}