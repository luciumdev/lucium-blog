import { useState } from 'react'
import { Link } from 'react-router-dom'
import { format } from 'date-fns'

import { Button, Card } from '@/shared/components'
import { ApiError } from '@/shared/api/ApiError'
import { useDebouncedValue } from '@/shared/hooks/useDebouncedValue'
import { useAdminPostsQuery } from '@/features/post/hooks/useAdminPostsQuery'
import {
    useDeletePostMutation,
    useTogglePublishMutation,
} from '@/features/post/hooks/usePostMutations'
import type { PostSummary, PostStatus } from '@/features/post/types'

const PAGE_SIZE = 20

type StatusFilter = PostStatus | 'ALL'

const STATUS_TABS: { value: StatusFilter; label: string }[] = [
    { value: 'ALL', label: '전체' },
    { value: 'PUBLISHED', label: '발행됨' },
    { value: 'DRAFT', label: '임시저장' },
]

export default function PostManagePage() {
    const [page, setPage] = useState(0)
    const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL')
    const [keyword, setKeyword] = useState('')
    const debouncedKeyword = useDebouncedValue(keyword, 300)

    const { data, isPending, isError, error } = useAdminPostsQuery({
        page,
        size: PAGE_SIZE,
        status: statusFilter === 'ALL' ? undefined : statusFilter,
        keyword: debouncedKeyword.trim() || undefined,
    })

    // 필터 변경 시 page 0 리셋 — totalPages 초과 방지
    const onStatusChange = (s: StatusFilter) => {
        setStatusFilter(s)
        setPage(0)
    }
    const onKeywordChange = (k: string) => {
        setKeyword(k)
        setPage(0)
    }

    const hasFilter = statusFilter !== 'ALL' || debouncedKeyword.trim().length > 0

    return (
        <section>
            <header className="mb-6 flex items-center justify-between">
                <h1 className="font-serif text-2xl font-semibold text-ink">게시글 관리</h1>
                <Link to="/admin/posts/new">
                    <Button variant="primary" size="sm">새 글 작성</Button>
                </Link>
            </header>

            <div className="mb-4 flex flex-col sm:flex-row gap-3 sm:items-center">
                <StatusTabs value={statusFilter} onChange={onStatusChange} />
                <input
                    type="search"
                    placeholder="제목으로 검색..."
                    value={keyword}
                    onChange={(e) => onKeywordChange(e.target.value)}
                    className="
                        flex-1 sm:max-w-xs h-10 px-3
                        rounded-md border border-border bg-surface text-sm text-ink
                        placeholder:text-ink-faint
                        focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary
                    "
                />
            </div>

            {isPending && (
                <div className="space-y-2">
                    {Array.from({ length: 5 }).map((_, i) => (
                        <div key={i} className="h-14 rounded-md bg-surface-muted animate-pulse" />
                    ))}
                </div>
            )}

            {isError && (
                <Card className="border-danger bg-danger-tint">
                    <p className="text-sm text-danger">
                        {error instanceof ApiError
                            ? `(${error.status}) ${error.message}`
                            : '목록을 불러올 수 없습니다.'}
                    </p>
                </Card>
            )}

            {data && data.content.length === 0 && (
                <Card>
                    <p className="text-sm text-ink-faint">
                        {hasFilter ? '조건에 맞는 글이 없습니다.' : '아직 등록된 글이 없습니다.'}
                    </p>
                </Card>
            )}

            {data && data.content.length > 0 && (
                <>
                    <p className="mb-2 font-mono text-xs text-ink-faint">
                        총 {data.totalElements.toLocaleString()}건
                    </p>
                    <div className="border border-border rounded-md overflow-hidden divide-y divide-border">
                        {data.content.map((post) => (
                            <PostRow key={post.publicId} post={post} />
                        ))}
                    </div>

                    <Pagination
                        page={page}
                        totalPages={data.totalPages}
                        onPrev={() => setPage((p) => Math.max(0, p - 1))}
                        onNext={() => setPage((p) => p + 1)}
                    />
                </>
            )}
        </section>
    )
}

function StatusTabs({
                        value,
                        onChange,
                    }: {
    value: StatusFilter
    onChange: (v: StatusFilter) => void
}) {
    return (
        <div
            role="tablist"
            aria-label="상태 필터"
            className="inline-flex p-1 rounded-md bg-surface-muted gap-0.5"
        >
            {STATUS_TABS.map((tab) => {
                const active = value === tab.value
                return (
                    <button
                        key={tab.value}
                        type="button"
                        role="tab"
                        aria-selected={active}
                        onClick={() => onChange(tab.value)}
                        className={`
                            px-3 py-1.5 rounded font-mono text-xs transition-colors
                            ${
                            active
                                ? 'bg-panel text-primary shadow-xs'
                                : 'text-ink-muted hover:text-ink'
                        }
                        `}
                    >
                        {tab.label}
                    </button>
                )
            })}
        </div>
    )
}

function PostRow({ post }: { post: PostSummary }) {
    const deleteMutation = useDeletePostMutation()
    const toggleMutation = useTogglePublishMutation()

    const onDelete = () => {
        if (!window.confirm(`"${post.title}" 삭제할까요?`)) return
        deleteMutation.mutate(post.publicId)
    }

    const onToggle = () => {
        toggleMutation.mutate({
            publicId: post.publicId,
            publish: post.status !== 'PUBLISHED',
        })
    }

    const dateStr = post.publishedAt ?? post.createdAt

    return (
        <div className="flex items-center gap-3 px-4 py-3 bg-panel hover:bg-panel-elevated transition-colors">
            <StatusBadge status={post.status} />

            <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-ink truncate">{post.title}</p>
                <p className="text-xs text-ink-faint font-mono">
                    {format(new Date(dateStr), 'yyyy-MM-dd HH:mm')}
                    {post.categoryName && <> · {post.categoryName}</>}
                </p>
            </div>

            <div className="flex items-center gap-1.5 shrink-0">
                <Link to={`/admin/posts/${post.publicId}/edit`}>
                    <Button variant="ghost" size="sm">편집</Button>
                </Link>
                <Button
                    variant="ghost"
                    size="sm"
                    onClick={onToggle}
                    isLoading={toggleMutation.isPending && toggleMutation.variables?.publicId === post.publicId}
                >
                    {post.status === 'PUBLISHED' ? '미발행' : '발행'}
                </Button>
                <Button
                    variant="ghost"
                    size="sm"
                    onClick={onDelete}
                    isLoading={deleteMutation.isPending && deleteMutation.variables === post.publicId}
                    className="text-danger hover:bg-danger-tint"
                >
                    삭제
                </Button>
            </div>
        </div>
    )
}

function StatusBadge({ status }: { status: 'DRAFT' | 'PUBLISHED' }) {
    if (status === 'PUBLISHED') {
        return (
            <span className="inline-flex items-center px-2 py-0.5 rounded font-mono text-[10px] text-accent border border-accent/40">
                published
            </span>
        )
    }
    return (
        <span className="inline-flex items-center px-2 py-0.5 rounded font-mono text-[10px] text-ink-faint border border-border">
            draft
        </span>
    )
}

function Pagination({
                        page,
                        totalPages,
                        onPrev,
                        onNext,
                    }: {
    page: number
    totalPages: number
    onPrev: () => void
    onNext: () => void
}) {
    if (totalPages <= 1) return null
    return (
        <div className="mt-4 flex items-center justify-center gap-3 font-mono text-sm text-ink-muted">
            <Button variant="ghost" size="sm" onClick={onPrev} disabled={page === 0}>
                ← prev
            </Button>
            <span>
                {page + 1} / {totalPages}
            </span>
            <Button variant="ghost" size="sm" onClick={onNext} disabled={page + 1 >= totalPages}>
                next →
            </Button>
        </div>
    )
}