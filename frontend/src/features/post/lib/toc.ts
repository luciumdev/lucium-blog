export interface TocItem {
    id: string
    text: string
    level: number
    line: number // 원본 마크다운 1-based 줄 번호
}

/** GitHub 유사 slug. 한글(\p{L}) 보존. */
export function slugify(text: string): string {
    return text
        .toLowerCase()
        .trim()
        .replace(/[^\p{L}\p{N}\s-]/gu, '')
        .replace(/\s+/g, '-')
}

/** id 중복 해소기. extractToc 내부 전용 (순수 함수 컨텍스트에서만 사용). */
function createIdResolver() {
    const seen = new Map<string, number>()
    return (text: string): string => {
        const base = slugify(text)
        if (!base) return ''
        const n = seen.get(base) ?? 0
        seen.set(base, n + 1)
        return n === 0 ? base : `${base}-${n}`
    }
}

/**
 * 본문 마크다운에서 h2/h3 추출.
 * - YAML 프론트매터 스킵
 * - 코드 펜스 내부 '#' 제외
 * - 줄 번호 보존 (replace로 삭제하면 줄 번호가 무너지므로 상태 추적 방식 사용)
 */
export function extractToc(markdown: string): TocItem[] {
    const lines = markdown.split('\n')
    const resolveId = createIdResolver()
    const items: TocItem[] = []

    // 프론트매터(--- ... ---) 시작 인덱스 이동
    let start = 0
    if (lines[0]?.trim() === '---') {
        const end = lines.findIndex((l, i) => i > 0 && l.trim() === '---')
        if (end > 0) start = end + 1
    }

    let inFence = false
    for (let i = start; i < lines.length; i++) {
        const line = lines[i]

        if (/^\s*```/.test(line)) {
            inFence = !inFence
            continue
        }
        if (inFence) continue

        const m = /^(#{2,3})\s+(.+?)\s*#*\s*$/.exec(line)
        if (!m) continue

        const level = m[1].length
        const text = m[2].replace(/[*_`~]/g, '').trim()
        const id = resolveId(text)
        if (!id) continue

        items.push({ id, text, level, line: i + 1 })
    }
    return items
}

/**
 * 줄 번호 → id 조회기.
 */
export function createLineIdLookup(items: TocItem[]) {
    const byLine = new Map<number, string>()
    for (const item of items) byLine.set(item.line, item.id)
    return (line: number | undefined): string | undefined =>
        line === undefined ? undefined : byLine.get(line)
}