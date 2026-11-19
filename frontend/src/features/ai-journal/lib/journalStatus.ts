import { format } from 'date-fns'

/** KST 기준 오늘 (YYYY-MM-DD) — 사용자 PC 시간대와 무관하게 백엔드 수집 기준에 맞춤 */
export function todayKst(): string {
    return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Seoul' }).format(new Date())
}

/**
 * 오늘 수집본이 아닐 때 보여줄 안내 문구. 오늘 수집본이거나 날짜를 모르면 null.
 * 예) "10/8 수집본을 표시하고 있습니다"
 */
export function staleLabel(journalDate: string | null | undefined): string | null {
    if (!journalDate || journalDate === todayKst()) return null
    try {
        return `${format(new Date(`${journalDate}T00:00:00`), 'M/d')} 수집본을 표시하고 있습니다`
    } catch {
        return null
    }
}
