import type { AiNewsItem, AiNewsImportance } from '../types'

const IMPORTANCE: Record<AiNewsImportance, { label: string; cls: string }> = {
    HIGH: { label: '중요', cls: 'bg-danger-tint text-danger' },
    MEDIUM: { label: '보통', cls: 'bg-primary-tint text-primary-hover' },
    LOW: { label: '참고', cls: 'bg-surface-muted text-ink-faint' },
}

/** "github:anthropic-sdk-python" → "GitHub · anthropic-sdk-python" (출처 접두어를 읽기 쉽게) */
function formatSource(source: string): string {
    const [prefix, ...rest] = source.split(':')
    if (rest.length === 0) return source
    const name = prefix.toLowerCase() === 'github' ? 'GitHub' : prefix
    return `${name} · ${rest.join(':')}`
}

/**
 * 뉴스 카드 — 카드 전체가 링크.
 * 호버는 글자색 대신 배경색(#F2F8FD, 메인 .row 와 같은 색) — 클릭되는 영역 = 색이 바뀌는 영역.
 * 테두리(border-border)로 페이지 배경과 구분.
 */
export function AiNewsCard({ item }: { item: AiNewsItem }) {
    const importance = IMPORTANCE[item.important] ?? IMPORTANCE.LOW
    const tags = item.tags ?? []

    return (
        <a
            href={item.url}
            target="_blank"
            rel="noopener noreferrer"
            className="h-full flex flex-col bg-panel border border-border rounded-2xl p-5 sm:p-6 hover:bg-[#F2F8FD] transition-colors"
        >
            <article className="contents">
                <header className="flex items-center justify-between gap-3">
                    <span className="min-w-0 truncate text-[13px] text-ink-faint">{formatSource(item.source)}</span>
                    <span className={`shrink-0 px-2 py-0.5 rounded-full text-xs font-semibold ${importance.cls}`}>
                        {importance.label}
                    </span>
                </header>

                <h3 className="mt-2.5 text-[17px] font-semibold leading-snug text-ink [text-wrap:balance]">
                    {item.title}
                </h3>

                {item.summary && (
                    <p className="mt-2 text-[15px] leading-[1.75] text-ink-muted [text-wrap:pretty] [overflow-wrap:anywhere]">
                        {item.summary}
                    </p>
                )}

                {tags.length > 0 && (
                    <div className="mt-auto pt-4 flex flex-wrap gap-1.5">
                        {tags.slice(0, 5).map((tag) => (
                            <span key={tag} className="px-2 py-0.5 rounded-full bg-surface-muted text-xs text-ink-muted">
                                {tag}
                            </span>
                        ))}
                    </div>
                )}
            </article>
        </a>
    )
}
