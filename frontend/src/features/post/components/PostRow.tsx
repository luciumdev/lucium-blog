import { Link } from 'react-router-dom'
import { format } from 'date-fns'
import type { PostSummary } from '../types'
import { paletteIndex } from '@/shared/lib/tagColor'
import { getCategoryColor } from '@/shared/lib/categoryColor'
import { cn } from '@/shared/lib/cn'

/** 카테고리 없는 글의 썸네일 — 채도 낮은 파스텔 4색 (P 시안) */
const THUMB_COLORS = ['#D2E7F6', '#E2E8F0', '#DCEBE4', '#EDE6DA'] as const

type Size = 'sm' | 'md'

/**
 * 글 목록 한 줄 (P 시안).
 *  - sm: 메인 "최근 글" — 썸네일 작게, 요약 숨김
 *  - md: 게시글 목록 — 요약 한 줄 + 메타
 *  - 카테고리 색: 배지(fg/bg)와 이미지 없는 썸네일 배경 — 메인 최근 글 배지·사이드 메뉴와 같은 categoryColor
 *  - 호버: 글자색 대신 줄 배경(.row, 메인과 같은 #F2F8FD). -mx-3 px-3 으로 배경이 글에 붙지 않게
 */
export function PostRow({ post, size = 'md' }: { post: PostSummary; size?: Size }) {
    const date = format(new Date(post.publishedAt ?? post.createdAt), 'yyyy.MM.dd')
    const firstTag = post.tags[0] ?? null

    return (
        <Link
            to={`/posts/${post.publicId}`}
            className="row -mx-3 px-3 flex items-center gap-4 sm:gap-5 py-4 text-ink focus-visible:outline-2 focus-visible:outline-primary"
        >
            <Thumbnail post={post} size={size} />

            <div className="flex-1 min-w-0">
                {post.categoryName ? (
                    <CategoryBadge name={post.categoryName} />
                ) : (
                    firstTag && <p className="text-[13px] font-medium text-ink-faint">{firstTag}</p>
                )}
                <h3
                    className={cn(
                        'font-semibold text-ink',
                        size === 'sm' ? 'text-base sm:text-[17px] truncate' : 'mt-1 text-[17px] sm:text-lg line-clamp-2',
                    )}
                >
                    {post.title}
                </h3>
                {size === 'md' && post.summary && (
                    <p className="mt-0.5 text-[15px] text-ink-muted truncate">{post.summary}</p>
                )}
                {/* 모바일에서는 메타를 제목 아래로 */}
                <p className="sm:hidden mt-0.5 text-[13px] text-ink-faint">
                    {date} · 조회 {post.viewCount.toLocaleString()}
                </p>
            </div>

            <p className="hidden sm:block shrink-0 text-right text-[13px] leading-relaxed text-ink-faint">
                {date}
                <br />
                조회 {post.viewCount.toLocaleString()}
            </p>
        </Link>
    )
}

/** 카테고리 배지 — 메인 "최근 글" 배지와 같은 모양 */
function CategoryBadge({ name }: { name: string }) {
    const c = getCategoryColor(name)
    return (
        <span
            style={{ color: c.fg, backgroundColor: c.bg }}
            className="inline-block px-2 py-px rounded-full text-xs font-semibold"
        >
            {name}
        </span>
    )
}

function Thumbnail({ post, size }: { post: PostSummary; size: Size }) {
    const box = size === 'sm' ? 'w-[72px] h-[54px] sm:w-[76px] sm:h-14' : 'w-[88px] h-16 sm:w-32 sm:h-[84px]'

    if (post.thumbnail) {
        return (
            <img
                src={post.thumbnail}
                alt=""
                loading="lazy"
                className={cn(box, 'shrink-0 rounded-[10px] object-cover bg-surface-muted')}
            />
        )
    }

    // 카테고리가 있으면 카테고리 배경색, 없으면 태그 → 제목 시드로 파스텔 색 (같은 글은 항상 같은 색)
    const color = post.categoryName
        ? getCategoryColor(post.categoryName).bg
        : THUMB_COLORS[paletteIndex(post.tags[0] ?? post.title) % THUMB_COLORS.length]

    return <span aria-hidden className={cn(box, 'shrink-0 rounded-[10px]')} style={{ backgroundColor: color }} />
}
