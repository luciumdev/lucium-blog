import { Link } from 'react-router-dom'
import { useTagsQuery } from '../hooks/useTagsQuery'

/**
 * //// ////// 사용 안함 /////
 * 사이드바 태그 목록 — postCount 내림차순(백엔드 정렬).
 * 클릭 시 /posts?tag={slug} 로 이동 — 백엔드 PostController가 slug로 필터.
 * 위젯 실패해도 사이드바는 살아있어야 함 (isError → null).
 */
export function TagTeaserWidget() {
    const { data, isPending, isError } = useTagsQuery()

    if (isError) return null

    return (
        <div className="mb-5">
            <p className="px-2 mb-1.5 text-[10px] font-mono font-semibold uppercase tracking-[0.2em] text-ink-muted">
                tags
            </p>
            <div className="flex flex-col gap-0.5">
                {isPending && <TagSkeleton />}
                {data && data.length === 0 && (
                    <p className="px-2 py-1 text-xs text-ink-faint font-mono">
                        — 태그 없음
                    </p>
                )}
                {data && data.length > 0 && (
                    <div className="flex flex-wrap gap-1 px-2 py-1">
                        {data.slice(0, 10).map((tag) => (
                            <Link
                                key={tag.id}
                                to={`/posts?tag=${encodeURIComponent(tag.slug)}`}
                                className="
                                    inline-flex items-center
                                    px-2 py-0.5 rounded-md
                                    font-mono text-xs
                                    text-ink-muted bg-surface/40 border border-border
                                    hover:text-primary hover:border-border-strong
                                    transition-colors
                                "
                            >
                                #{tag.name}
                            </Link>
                        ))}
                    </div>
                )}
            </div>
        </div>
    )
}

function TagSkeleton() {
    return (
        <div className="flex flex-wrap gap-1 px-2 py-1" aria-busy="true">
            {Array.from({ length: 6 }).map((_, i) => (
                <div
                    key={i}
                    className="h-5 w-14 bg-panel rounded-md animate-pulse"
                />
            ))}
        </div>
    )
}
