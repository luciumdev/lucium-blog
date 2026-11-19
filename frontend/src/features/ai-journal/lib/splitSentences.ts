/**
 * 요약 문단을 문장(주제) 단위로 분리.
 */
export function splitSentences(text: string | null | undefined): string[] {
    if (!text) return []
    const trimmed = text.trim()
    if (!trimmed) return []

    const byLine = trimmed
        .split(/\n+/)
        .map((s) => s.replace(/^[-•·*]\s*/, '').trim())
        .filter(Boolean)
    if (byLine.length > 1) return byLine

    return trimmed
        .split(/(?<=[.!?])\s+(?=\S)/)
        .map((s) => s.trim())
        .filter(Boolean)
}
