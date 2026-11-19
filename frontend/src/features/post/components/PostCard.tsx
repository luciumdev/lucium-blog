import { Link, useNavigate } from 'react-router-dom'
import { format } from 'date-fns'
import type { PostSummary } from '../types'
import { getReadAt } from '@/shared/lib/readTracker'
import { getTagColor, paletteIndex, getPaletteAt } from '@/shared/lib/tagColor'

const MAX_TAGS = 3

/**
 * "3d ago" 같은 콘솔 short 포맷으로 시간 표시.
 */
function shortAgo(ms: number): string {
    const diff = Date.now() - ms
    const m = Math.floor(diff / 60000)
    const h = Math.floor(m / 60)
    const d = Math.floor(h / 24)
    if (d > 0) return `${d}d ago`
    if (h > 0) return `${h}h ago`
    if (m > 0) return `${m}m ago`
    return 'just now'
}

export function PostCard({
                             post,
                             hideThumbnail = false,
                         }: {
    post: PostSummary
    hideThumbnail?: boolean
}) {
    const dateStr = post.publishedAt ?? post.createdAt
    const formattedDate = format(new Date(dateStr), 'yyyy년 M월 d일')

    const readAt = getReadAt(post.publicId)
    const isRead = readAt !== null

    const visibleTags = post.tags.slice(0, MAX_TAGS)
    const hiddenCount = post.tags.length - visibleTags.length

    return (
        <article
            className={`
        group relative flex flex-col
        bg-panel border border-border rounded-lg overflow-hidden
        transition-all duration-200
        hover:border-ink-muted hover:bg-panel-elevated hover:-translate-y-0.5
        ${isRead ? 'opacity-75 hover:opacity-100' : ''}
      `}
        >
            <Link
                to={`/posts/${post.publicId}`}
                aria-label={post.title}
                className="absolute inset-0 z-10 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary rounded-lg"
            />

            {!hideThumbnail && <Thumbnail post={post} />}

            {/* 읽은 글에만 표시되는 상태 박지 — 우상단 */}
            {isRead && (
                <div
                    className="
            absolute top-2 right-2 z-20
              flex items-center gap-1
              px-2 py-1 rounded
              bg-surface/95 backdrop-blur-sm
              border border-primary/40
              font-mono text-[10px] text-primary
              pointer-events-none
          "
                >
                    <CheckIcon />
                    <span>read · {shortAgo(readAt!)}</span>
                </div>
            )}

            <div className="flex flex-col gap-1.5 p-3">
                <h2 className={`font-serif text-base font-semibold text-ink leading-snug line-clamp-2 min-h-[2.6em] group-hover:text-primary transition-colors ${
                    hideThumbnail && isRead ? 'pr-32' : ''
                }`}>
                    {post.title}
                </h2>

                <div className="relative z-20 min-h-[22px] flex flex-wrap gap-1">
                    {visibleTags.map((tag) => (
                        <TagChip key={tag} tag={tag} />
                    ))}
                    {hiddenCount > 0 && (
                        <span
                            title={post.tags.slice(MAX_TAGS).join(', ')}
                            className="inline-flex items-center px-1.5 py-0.5 rounded-md text-xs text-ink-faint border border-border cursor-help"
                        >
    +{hiddenCount}
            </span>
                    )}
                </div>

                <div className="flex items-center gap-1.5 text-xs text-ink-faint">
                    <CalendarIcon />
                    <span>{formattedDate}</span>
                    <span aria-hidden>·</span>
                    <span>조회 {post.viewCount.toLocaleString()}</span>
                </div>
            </div>
        </article>
    )
}


function CheckIcon() {
    return (
        <svg
            width="10"
            height="10"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="3"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
        >
            <polyline points="20 6 9 17 4 12" />
        </svg>
    )
}

function TagChip({ tag }: { tag: string }) {
    const navigate = useNavigate()
    const color = getTagColor(tag)

    return (
        <button
            type="button"
            onClick={(e) => {
                e.stopPropagation()
                navigate(`/posts?tag=${encodeURIComponent(tag)}`)
            }}
            style={{
                color: color.text,
                borderColor: color.border,
                '--tag-hover-bg': color.hoverBg,
            } as React.CSSProperties}
            className="
        inline-flex items-center px-1.5 py-0.5 rounded font-mono
        text-xs border bg-transparent
        transition-colors
        hover:bg-[var(--tag-hover-bg)]
      "
        >
            #{tag}
        </button>
    )
}

/**
 * 썸네일 영역. 이미지가 없어도 영역은 유지한다 —
 * 빼면 그 카드만 짧아져 그리드 정렬이 깨진다.
 * 빈 상태는 시드 기반 2색 그라디언트로 채운다(Notion 커버 방식).
 */
function Thumbnail({ post }: { post: PostSummary }) {
    if (post.thumbnail) {
        return (
            <div className="relative aspect-video overflow-hidden bg-surface-muted border-b border-border">
                <img
                    src={post.thumbnail}
                    alt=""
                    loading="lazy"
                    className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
                />
            </div>
        )
    }

    // 카테고리 → 태그 → 제목 순으로 시드. 같은 글은 항상 같은 색.
    const seed = post.categoryName ?? post.tags[0] ?? post.title
    const i = paletteIndex(seed)

    // 인접색이 아닌 5칸 건너뛴 색을 짝으로 — 팔레트가 12색이라 확실히 다른 계열이 잡힌다.
    const a = getPaletteAt(i)
    const b = getPaletteAt(i + 5)

    return (
        <div
            className="relative aspect-video overflow-hidden border-b border-border"
            style={{
                backgroundImage: `linear-gradient(135deg, ${a.hoverBg}, ${b.hoverBg})`,
            }}
        />
    )
}

function CalendarIcon() {
    return (
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
            <rect x="3" y="5" width="18" height="16" rx="2" />
            <line x1="3" y1="9" x2="21" y2="9" />
            <line x1="8" y1="3" x2="8" y2="7" strokeLinecap="round" />
            <line x1="16" y1="3" x2="16" y2="7" strokeLinecap="round" />
        </svg>
    )
}