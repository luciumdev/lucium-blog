import type { AiNewsItem } from '../types'
import { AiNewsCard } from './AiNewsCard'

export function AiNewsFeed({ items }: { items: AiNewsItem[] }) {
    if (items.length === 0) {
        return (
            <p className="font-mono text-sm text-ink-faint py-6 text-center">
                — 표시할 뉴스가 없습니다
            </p>
        )
    }

    return (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
            {items.map((item, i) => (
                <AiNewsCard key={`${item.url}-${i}`} item={item} />
            ))}
        </div>
    )
}