import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { cn } from '@/shared/lib/cn'
import { ApiError } from '@/shared/api/ApiError'
import { usePostsQuery } from '@/features/post/hooks/usePostsQuery'
import { PostRow } from '@/features/post/components/PostRow'
import { useCategoriesQuery } from '@/features/category/hooks/useCategoriesQuery'
import { useTagsQuery } from '@/features/tag/hooks/useTagsQuery'
import { usePopularPostsQuery } from '@/features/stat/hooks/usePopularPostsQuery'

const PAGE_SIZE = 10

/**
 * 게시글 목록
 *  - 좌측 카테고리는 공통 레이아웃 사이드 메뉴가 담당
 *  - 본문: 제목 · 검색 · 글 목록 · 페이지 번호
 *  - 우측: 태그 · 많이 읽은 글
 *  - 쿼리스트링: category / tag / page(1부터)
 *  - 카드는 테두리(border-border)로 페이지 배경과 구분, 글 줄은 호버 시 배경색(.row)
 */
export default function PostListPage() {
    const [searchParams] = useSearchParams()
    const tag = searchParams.get('tag') ?? undefined
    const category = searchParams.get('category') ?? undefined
    const page = Math.max(1, Number(searchParams.get('page')) || 1)

    const { data, isPending, isError, error } = usePostsQuery({
        size: PAGE_SIZE,
        page: page - 1, // Spring Pageable은 0부터
        tag,
        category,
    })

    const { data: categories } = useCategoriesQuery()
    const categoryName = categories?.find((c) => c.slug === category)?.name

    const title = tag ? `#${tag}` : category ? (categoryName ?? category) : '전체 글'

    return (
        <div className="flex flex-col lg:flex-row gap-5 items-stretch">
            <section className="flex-1 min-w-0 bg-panel border border-border rounded-2xl px-5 sm:px-7 pt-6 pb-4 flex flex-col">
                <header className="flex flex-wrap items-end justify-between gap-3">
                    <div>
                        <h1 className="text-2xl sm:text-[26px]">{title}</h1>
                        <p className="mt-0.5 text-sm text-ink-faint">
                            {data ? `${data.totalElements.toLocaleString()}개의 글` : ' '}
                            {(tag || category) && (
                                <Link to="/posts" className="ml-2 text-ink-muted underline underline-offset-2 hover:text-primary">
                                    필터 해제
                                </Link>
                            )}
                        </p>
                    </div>
                    <PostSearchBox />
                </header>

                <MobileCategoryChips active={category} />

                <div className="mt-2 flex-1">
                    {isPending && <PostListSkeleton />}

                    {isError && (
                        <p className="my-6 p-4 rounded-xl bg-danger-tint text-[15px] text-danger">
                            {error instanceof ApiError ? `(${error.status}) ${error.message}` : '알 수 없는 오류가 발생했습니다.'}
                        </p>
                    )}

                    {data && data.content.length === 0 && (
                        <p className="py-12 text-center text-[15px] text-ink-faint">
                            {tag || category ? '해당 조건의 글이 없습니다.' : '아직 발행된 글이 없습니다.'}
                        </p>
                    )}

                    {data && data.content.length > 0 && (
                        <ul className="divide-y divide-border">
                            {data.content.map((post) => (
                                <li key={post.publicId}>
                                    <PostRow post={post} />
                                </li>
                            ))}
                        </ul>
                    )}
                </div>

                {data && data.totalPages > 1 && <Pagination current={page} total={data.totalPages} />}
            </section>

            <aside className="w-full lg:w-80 shrink-0 flex flex-col gap-5">
                <TagPanel activeTag={tag} />
                <PopularPanel />
            </aside>
        </div>
    )
}

/* ─────────────────────────── 검색 ─────────────────────────── */

function PostSearchBox() {
    const navigate = useNavigate()
    const [q, setQ] = useState('')

    const submit = (e: FormEvent) => {
        e.preventDefault()
        const trimmed = q.trim()
        if (trimmed.length >= 2) navigate(`/search?q=${encodeURIComponent(trimmed)}`)
    }

    return (
        <form role="search" onSubmit={submit} className="w-full sm:w-60">
            <label htmlFor="posts-q" className="sr-only">게시글 검색</label>
            <div className="flex items-center gap-2 h-11 px-3.5 bg-surface-muted rounded-xl focus-within:ring-2 focus-within:ring-primary/40">
                <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" className="text-ink-faint shrink-0" aria-hidden>
                    <circle cx="11" cy="11" r="6.5" />
                    <path d="m20 20-4-4" />
                </svg>
                <input
                    id="posts-q"
                    type="search"
                    value={q}
                    onChange={(e) => setQ(e.target.value)}
                    placeholder="글 검색 (2자 이상)"
                    className="flex-1 min-w-0 bg-transparent text-[15px] text-ink placeholder:text-ink-faint focus:outline-none"
                />
            </div>
        </form>
    )
}

/* ─────────────────────────── 모바일 카테고리 ─────────────────────────── */

/** 모바일은 사이드 메뉴가 없으므로 카테고리를 가로 칩으로 노출 */
function MobileCategoryChips({ active }: { active?: string }) {
    const { data } = useCategoriesQuery()
    if (!data || data.length === 0) return null

    const chip = (selected: boolean) =>
        cn(
            'shrink-0 h-9 px-4 flex items-center rounded-full text-sm transition-colors',
            selected ? 'bg-primary text-white font-semibold' : 'bg-surface-muted text-ink',
        )

    return (
        <nav aria-label="카테고리" className="md:hidden mt-4 -mx-5 px-5 flex gap-2 overflow-x-auto">
            <Link to="/posts" className={chip(!active)}>전체</Link>
            {data.map((c) => (
                <Link key={c.id} to={`/posts?category=${encodeURIComponent(c.slug)}`} className={chip(active === c.slug)}>
                    {c.name}
                </Link>
            ))}
        </nav>
    )
}

/* ─────────────────────────── 페이지네이션 ─────────────────────────── */

function Pagination({ current, total }: { current: number; total: number }) {
    const [searchParams] = useSearchParams()

    const hrefFor = (p: number) => {
        const next = new URLSearchParams(searchParams)
        if (p <= 1) next.delete('page')
        else next.set('page', String(p))
        const qs = next.toString()
        return qs ? `/posts?${qs}` : '/posts'
    }

    // 현재 페이지 기준 최대 5개 노출
    const start = Math.max(1, Math.min(current - 2, total - 4))
    const pages = Array.from({ length: Math.min(5, total) }, (_, i) => start + i)

    const base = 'min-w-11 h-11 px-2 flex items-center justify-center rounded-xl text-[15px] transition-colors'

    return (
        <nav aria-label="페이지" className="mt-4 flex justify-center gap-1">
            {current > 1 && (
                <Link to={hrefFor(current - 1)} aria-label="이전 페이지" className={cn(base, 'text-ink hover:bg-panel-elevated')}>
                    ←
                </Link>
            )}
            {pages.map((p) => (
                <Link
                    key={p}
                    to={hrefFor(p)}
                    aria-current={p === current ? 'page' : undefined}
                    className={cn(base, p === current ? 'bg-primary text-white font-semibold' : 'text-ink hover:bg-panel-elevated')}
                >
                    {p}
                </Link>
            ))}
            {current < total && (
                <Link to={hrefFor(current + 1)} aria-label="다음 페이지" className={cn(base, 'text-ink hover:bg-panel-elevated')}>
                    →
                </Link>
            )}
        </nav>
    )
}

/* ─────────────────────────── 우측 열 ─────────────────────────── */

function TagPanel({ activeTag }: { activeTag?: string }) {
    const { data, isPending, isError } = useTagsQuery()
    if (isError) return null

    return (
        <section className="bg-panel border border-border rounded-2xl p-6">
            <h2 className="text-[17px] font-bold">태그</h2>
            <div className="mt-3 flex flex-wrap gap-2">
                {isPending &&
                    Array.from({ length: 6 }).map((_, i) => (
                        <span key={i} className="h-8 w-16 bg-panel-elevated rounded-full animate-pulse" />
                    ))}
                {data?.slice(0, 15).map((tag) => {
                    const active = activeTag === tag.slug
                    return (
                        <Link
                            key={tag.id}
                            to={`/posts?tag=${encodeURIComponent(tag.slug)}`}
                            aria-current={active ? 'page' : undefined}
                            className={cn(
                                'px-3 py-1 rounded-full border text-sm transition-colors',
                                active
                                    ? 'border-primary bg-primary-tint text-primary-hover font-semibold'
                                    : 'border-border-strong text-ink hover:border-primary hover:text-primary',
                            )}
                        >
                            {tag.name}
                            <span className="ml-1 text-ink-faint">{tag.postCount}</span>
                        </Link>
                    )
                })}
            </div>
        </section>
    )
}

/** 많이 읽은 글 — 순위 숫자까지 한 줄 전체가 링크, 호버 시 배경색(.row) */
function PopularPanel() {
    const { data, isPending, isError } = usePopularPostsQuery(5)
    if (isError) return null

    return (
        <section className="flex-1 bg-panel border border-border rounded-2xl p-6">
            <h2 className="text-[17px] font-bold">많이 읽은 글</h2>
            {isPending && (
                <div className="mt-4 space-y-4" aria-busy="true">
                    {Array.from({ length: 5 }).map((_, i) => (
                        <div key={i} className="h-4 w-5/6 bg-panel-elevated rounded animate-pulse" />
                    ))}
                </div>
            )}
            {data && data.length === 0 && <p className="mt-3 text-[15px] text-ink-faint">아직 데이터가 없습니다.</p>}
            {data && data.length > 0 && (
                <ol className="mt-3 space-y-1">
                    {data.map((post, i) => (
                        <li key={post.publicId}>
                            <Link to={`/posts/${post.publicId}`} className="row -mx-2 px-2 py-2 flex gap-3.5">
                                <span className={cn('w-4 shrink-0 text-xl font-bold leading-tight tabular-nums', i === 0 ? 'text-primary' : 'text-ink')}>
                                    {i + 1}
                                </span>
                                <span className="min-w-0">
                                    <span className="block text-[15px] font-medium text-ink line-clamp-2">{post.title}</span>
                                    <span className="text-[13px] text-ink-faint">조회 {post.viewCount.toLocaleString()}</span>
                                </span>
                            </Link>
                        </li>
                    ))}
                </ol>
            )}
        </section>
    )
}

function PostListSkeleton() {
    return (
        <ul className="divide-y divide-border" aria-busy="true" aria-label="글 목록 불러오는 중">
            {Array.from({ length: 5 }).map((_, i) => (
                <li key={i} className="flex items-center gap-5 py-4">
                    <div className="w-[88px] h-16 sm:w-32 sm:h-[84px] bg-panel-elevated rounded-[10px] animate-pulse" />
                    <div className="flex-1 space-y-2">
                        <div className="h-3 w-20 bg-panel-elevated rounded animate-pulse" />
                        <div className="h-5 w-3/4 bg-panel-elevated rounded animate-pulse" />
                        <div className="h-4 w-1/2 bg-panel-elevated rounded animate-pulse" />
                    </div>
                </li>
            ))}
        </ul>
    )
}
