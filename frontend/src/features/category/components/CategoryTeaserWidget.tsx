import { Link } from 'react-router-dom'
import { useCategoriesQuery } from '../hooks/useCategoriesQuery'
import { CategoryIcon } from '@/shared/lib/categoryIcon'

/**
 * 사이드바 카테고리 목록.
 * 클릭 시 /posts?category={slug} 로 이동 — 백엔드 PostController가 slug로 필터.
 * 위젯 실패해도 사이드바는 살아있어야 함 (isError → null).
 */
export function CategoryTeaserWidget() {
    const { data, isPending, isError } = useCategoriesQuery()

    if (isError) return null

    return (
        <div className="mb-5">
            <p className="px-2 mb-1.5 text-[10px] font-mono font-semibold uppercase tracking-[0.2em] text-ink-muted">
                categories
            </p>
            <div className="flex flex-col gap-0.5">
                {isPending && <CategorySkeleton />}
                {data && data.length === 0 && (
                    <p className="px-2 py-1 text-xs text-ink-faint font-mono">
                        — 카테고리 없음
                    </p>
                )}
                {data &&
                    data.length > 0 &&
                    data.map((category) => (
                        <Link
                            key={category.id}
                            to={`/posts?category=${encodeURIComponent(category.slug)}`}
                            className="
                                flex items-center gap-2 px-2 py-1.5 rounded-md
                                font-mono text-sm text-ink-muted
                                hover:bg-panel/50 hover:text-primary
                                transition-colors group
                            "
                        >
                            <span className="text-ink-faint group-hover:text-primary">
                                <CategoryIcon slug={category.slug} className="w-3.5 h-3.5" />
                            </span>
                            <span className="truncate">{category.name}</span>
                        </Link>
                    ))}
            </div>
        </div>
    )
}

function CategorySkeleton() {
    return (
        <ul className="flex flex-col gap-0.5" aria-busy="true">
            {Array.from({ length: 3 }).map((_, i) => (
                <li key={i}>
                    <div className="flex items-center gap-2 px-2 py-1.5">
                        <div className="h-3 w-3 bg-panel rounded animate-pulse" />
                        <div className="h-3 w-20 bg-panel rounded animate-pulse" />
                    </div>
                </li>
            ))}
        </ul>
    )
}
