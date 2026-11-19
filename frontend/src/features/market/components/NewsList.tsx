import { useState } from 'react'
import { Card } from '@/shared/components'
import { useNewsQuery } from '../hooks/useNewsQuery'
import type { NewsCategory, NewsItem } from '../types'

const TABS: { key: NewsCategory; label: string }[] = [
    { key: 'IT', label: 'tech' },
    { key: 'ECONOMY', label: 'economy' },
]

const MAX_ITEMS = 4

export function NewsList() {
    const [active, setActive] = useState<NewsCategory>('IT')
    const { data, isPending, isError } = useNewsQuery(active)

    return (
        <Card padding="none" className="h-full flex flex-col">
            <header className="px-4 py-2 border-b border-border flex items-center gap-1">
                <h2 className="font-mono text-xs uppercase tracking-[0.2em] text-ink-muted font-semibold mr-3">
                    news
                </h2>
                {TABS.map((tab) => (
                    <button
                        key={tab.key}
                        type="button"
                        onClick={() => setActive(tab.key)}
                        className={`
                            px-2.5 py-1 rounded font-mono text-[11px] uppercase tracking-wider
                            transition-colors
                            ${active === tab.key
                            ? 'bg-panel-elevated text-accent'
                            : 'text-ink-muted hover:text-ink'
                        }
                        `}
                    >
                        {tab.label}
                    </button>
                ))}
            </header>

            {isPending && <NewsListSkeleton />}

            {isError && (
                <p className="px-4 py-6 text-sm text-danger font-mono">
                    뉴스를 불러오지 못했습니다.
                </p>
            )}

            {data && data.items.length === 0 && (
                <p className="px-4 py-6 text-sm text-ink-faint font-mono">— 뉴스 없음</p>
            )}

            {data && data.items.length > 0 && (
                <ul className="flex-1 divide-y divide-border">
                    {data.items.slice(0, MAX_ITEMS).map((item, i) => (
                        <NewsRow key={`${item.url}-${i}`} item={item} />
                    ))}
                </ul>
            )}
        </Card>
    )
}

function NewsRow({ item }: { item: NewsItem }) {
    return (
        <li>
            <a
            href={item.url}
            target="_blank"
            rel="noopener noreferrer"
            className="block px-4 py-3 hover:bg-panel-elevated transition-colors group"
            >
            <p className="text-sm text-ink leading-snug line-clamp-2 group-hover:text-primary transition-colors">
                {item.title}
            </p>
            <div className="mt-1.5 flex items-center gap-1.5 font-mono text-[11px] text-ink-faint">
                {item.source && (
                    <>
                        <span>{item.source}</span>
                        <span aria-hidden>·</span>
                    </>
                )}
                <span>{shortAgo(item.publishedAt)}</span>
            </div>
        </a>
</li>
)
}

function shortAgo(iso: string): string {
    try {
        const diff = Date.now() - new Date(iso).getTime()
        const m = Math.floor(diff / 60_000)
        const h = Math.floor(m / 60)
        const d = Math.floor(h / 24)
        if (d > 0) return `${d}d ago`
        if (h > 0) return `${h}h ago`
        if (m > 0) return `${m}m ago`
        return 'just now'
    } catch {
        return ''
    }
}

function NewsListSkeleton() {
    return (
        <ul className="divide-y divide-border" aria-busy="true">
            {Array.from({ length: 5 }).map((_, i) => (
                <li key={i} className="px-4 py-3">
                    <div className="h-4 w-3/4 bg-surface rounded animate-pulse" />
                    <div className="mt-1.5 h-3 w-1/3 bg-surface rounded animate-pulse" />
                </li>
            ))}
        </ul>
    )
}