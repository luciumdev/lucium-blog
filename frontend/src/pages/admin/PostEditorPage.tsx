import { useEffect, useState, lazy, Suspense } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'

import { Button, Input, Card } from '@/shared/components'
import { ApiError } from '@/shared/api/ApiError'
// BlogEditor 는 @mdxeditor(Prism 의존) 를 포함해 무겁고 관리자 전용이라 lazy 분리.
// 정적 import 시 홈/market 등 모든 페이지 초기 번들에 섞여 Prism 미로드로 앱 전체가 죽음.
const BlogEditor = lazy(() =>
    import('@/features/post/components/BlogEditor').then((m) => ({
        default: m.BlogEditor,
    })),
)
import { useAdminPostDetailQuery } from '@/features/post/hooks/useAdminPostDetailQuery.ts'
import {
    useCreatePostMutation,
    useUpdatePostMutation,
    useDeletePostMutation,
    useTogglePublishMutation,
} from '@/features/post/hooks/usePostMutations'
import type { PostCreateInput, PostUpdateInput, PostStatus, ContentFormat } from '@/features/post/types'
import { HtmlSourceEditor } from '@/features/post/components/HtmlSourceEditor'

import { useCategoriesQuery } from '@/features/category/hooks/useCategoriesQuery.ts'
import { uploadFileToS3 } from '@/features/files/api'
import { format } from 'date-fns'
import { useBlocker } from 'react-router-dom'
import { Controller } from 'react-hook-form'
import { TagsField } from '@/features/post/components/TagsField'
import { CategorySelect } from '@/features/category/components/CategorySelect'

// ─────────────────────────────────────────────────────────
// Schema
// ─────────────────────────────────────────────────────────
const AUTOSAVE_DEBOUNCE_MS = 30_000

const postFormSchema = z.object({
    title: z.string().trim().min(1, '제목을 입력하세요.').max(300, '제목은 300자 이하'),
    summary: z.string().max(500, '요약은 500자 이하').optional(),
    thumbnail: z.string().optional(),
    categoryId: z.number().int().positive().nullable(),
    tags: z.array(z.string()).optional(),
    contentFormat: z.enum(['MARKDOWN', 'HTML']),
    content: z
        .string()
        .refine((v) => v.trim().length > 0, '본문을 입력하세요.'),
})
type PostFormValues = z.infer<typeof postFormSchema>

// ─────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────

function buildPayload(values: PostFormValues): PostUpdateInput {
    return {
        title: values.title.trim(),
        content: values.content,
        contentFormat: values.contentFormat,
        summary: values.summary?.trim() ? values.summary.trim() : null,
        thumbnail: values.thumbnail?.trim() ? values.thumbnail.trim() : null,
        categoryId: values.categoryId,
        tags: values.tags ?? [],
    }
}

function formatApiError(err: unknown): string {
    if (err instanceof ApiError) {
        return err.problem?.detail ?? err.message
    }
    return '요청 처리 중 오류가 발생했습니다.'
}

// ─────────────────────────────────────────────────────────
// Page
// ─────────────────────────────────────────────────────────
export default function PostEditorPage() {
    const { publicId } = useParams<{ publicId: string }>()
    const isEditMode = !!publicId
    const navigate = useNavigate()
    const [isAutoSaving, setIsAutoSaving] = useState(false)
    const { data: detail, isPending: isDetailPending, isError: isDetailError } = useAdminPostDetailQuery(publicId)
    const { data: categories } = useCategoriesQuery()

    const createMutation = useCreatePostMutation()
    const updateMutation = useUpdatePostMutation()
    const deleteMutation = useDeletePostMutation()
    const togglePublishMutation = useTogglePublishMutation()
    const [lastSavedAt, setLastSavedAt] = useState<Date | null>(null)
    const form = useForm<PostFormValues>({
        resolver: zodResolver(postFormSchema),
        defaultValues: {
            title: '',
            summary: '',
            thumbnail: '',
            categoryId: null,
            tags: [],
            contentFormat: 'MARKDOWN',
            content: '',
        },
    })

    // edit 모드: detail 로드되면 form 초기화 + editor mount 허용
    const [editorReady, setEditorReady] = useState(!isEditMode)
    useEffect(() => {
        if (detail) {
            form.reset({
                title: detail.title,
                summary: detail.summary ?? '',
                thumbnail: detail.thumbnail ?? '',
                categoryId: detail.categoryId,
                tags: detail.tags,
                contentFormat: detail.contentFormat ?? 'MARKDOWN',
                content: detail.content,
            })
            setEditorReady(true)
        }
    }, [detail, form])

    const isDirty = form.formState.isDirty

    // 본문 형식 — 바꾸면 에디터를 새로 mount (MDXEditor 는 초기 마크다운을 mount 때만 읽음)
    const contentFormat = form.watch('contentFormat')
    const [editorKey, setEditorKey] = useState(0)
    const onChangeFormat = (next: ContentFormat) => {
        if (next === contentFormat) return
        const hasBody = form.getValues('content').trim().length > 0
        if (hasBody && !window.confirm('형식을 바꾸면 지금 본문이 비워집니다. 계속할까요?')) return
        form.setValue('contentFormat', next, { shouldDirty: true })
        form.setValue('content', '', { shouldDirty: true })
        setEditorKey((k) => k + 1)
    }

    // 파일에서 제목을 찾았을 때 — 제목 칸이 비어 있을 때만 채움 (직접 쓴 제목은 덮어쓰지 않음)
    const onImportTitle = (title: string) => {
        if (!form.getValues('title').trim()) {
            form.setValue('title', title.slice(0, 300), { shouldDirty: true, shouldValidate: true })
        }
    }

    // SPA 내부 라우트 이동 차단
    const blocker = useBlocker(
        ({ currentLocation, nextLocation }) =>
            isDirty && currentLocation.pathname !== nextLocation.pathname,
    )
    useEffect(() => {
        if (blocker.state === 'blocked') {
            if (window.confirm('저장하지 않은 변경사항이 있습니다. 페이지를 떠나시겠습니까?')) {
                blocker.proceed()
            } else {
                blocker.reset()
            }
        }
    }, [blocker])

    // 브라우저 새로고침/탭 닫기 차단
    useEffect(() => {
        if (!isDirty) return
        const handler = (e: BeforeUnloadEvent) => {
            e.preventDefault()
            e.returnValue = ''
        }
        window.addEventListener('beforeunload', handler)
        return () => window.removeEventListener('beforeunload', handler)
    }, [isDirty])

    // ── 액션 핸들러 ──────────────────────────────────────
    const submitCreate = (status: PostStatus) =>
        form.handleSubmit((values) => {
            const payload: PostCreateInput = { ...buildPayload(values), status }
            createMutation.mutate(payload, {
                onSuccess: (post) => {
                    setLastSavedAt(new Date())
                    form.reset(form.getValues(), { keepDirty: false })  // dirty 해제 → useBlocker 통과
                    if (status === 'PUBLISHED') navigate(`/posts/${post.publicId}`)
                    else navigate('/admin/posts')
                },
            })
        })
    const submitUpdate = form.handleSubmit((values) => {
        if (!publicId) return
        updateMutation.mutate(
            { publicId, input: buildPayload(values) },
            {
                onSuccess: () => {
                    setLastSavedAt(new Date())
                    form.reset(form.getValues(), { keepDirty: false })
                },
            },
        )
    })
    // DRAFT 편집 모드 + dirty 일 때만 자동 저장 활성
    const isDraftEdit = isEditMode && detail?.status === 'DRAFT'

    useEffect(() => {
        if (!isDraftEdit || !publicId) return

        let timer: ReturnType<typeof setTimeout> | null = null

        const subscription = form.watch(() => {
            if (timer) clearTimeout(timer)

            timer = setTimeout(() => {
                // 시점 검증 — timer 발화 순간의 latest 상태로
                if (!form.formState.isDirty) return
                const result = postFormSchema.safeParse(form.getValues())
                if (!result.success) return  // 검증 실패 글은 자동 저장 안 함

                setIsAutoSaving(true)
                updateMutation.mutate(
                    { publicId, input: buildPayload(form.getValues()) },
                    {
                        onSuccess: () => {
                            setLastSavedAt(new Date())
                            form.reset(form.getValues(), { keepDirty: false })
                        },
                        onSettled: () => setIsAutoSaving(false),
                    },
                )
            }, AUTOSAVE_DEBOUNCE_MS)
        })

        return () => {
            subscription.unsubscribe()
            if (timer) clearTimeout(timer)
        }
    }, [isDraftEdit, publicId, form, updateMutation])
    const onTogglePublish = () => {
        if (!publicId || !detail) return
        togglePublishMutation.mutate(
            { publicId, publish: detail.status !== 'PUBLISHED' },
            { onSuccess: () => setLastSavedAt(new Date()) },
        )
    }

    const onDelete = () => {
        if (!publicId) return
        if (!window.confirm('정말 삭제하시겠습니까? 이 동작은 되돌릴 수 없습니다.')) return
        deleteMutation.mutate(publicId, {
            onSuccess: () => navigate('/admin/posts'),
        })
    }

    // 썸네일 업로드
    const [thumbProgress, setThumbProgress] = useState<number | null>(null)  // null = idle
    const [thumbError, setThumbError] = useState<string | null>(null)
    const thumbUploading = thumbProgress !== null
    const onThumbUpload = async (file: File) => {
        setThumbError(null)
        setThumbProgress(0)
        try {
            const url = await uploadFileToS3(file, {
                onProgress: setThumbProgress,
            })
            form.setValue('thumbnail', url, { shouldDirty: true, shouldValidate: true })
        } catch (e) {
            setThumbError(e instanceof Error ? e.message : '업로드 실패')
        } finally {
            setThumbProgress(null)
        }
    }

    // ── 로딩 / 에러 게이트 ────────────────────────────
    if (isEditMode && isDetailPending) {
        return <p className="text-ink-muted">불러오는 중...</p>
    }
    if (isEditMode && (isDetailError || !detail)) {
        return (
            <Card className="border-danger bg-danger-tint">
                <p className="text-sm text-danger">글을 찾을 수 없습니다.</p>
                <Link to="/admin/posts" className="mt-3 inline-block text-sm text-primary">
                    ← 목록으로
                </Link>
            </Card>
        )
    }

    const errorMessage =
        formatMutationError(createMutation.error) ??
        formatMutationError(updateMutation.error) ??
        formatMutationError(deleteMutation.error) ??
        formatMutationError(togglePublishMutation.error)

    const isAnyMutating =
        createMutation.isPending ||
        updateMutation.isPending ||
        deleteMutation.isPending ||
        togglePublishMutation.isPending

    // ── 렌더 ─────────────────────────────────────────────
    return (
        <section className="max-w-4xl">
            <header className="mb-6 flex items-center justify-between">
                <h1 className="font-serif text-2xl font-semibold text-ink">
                    {isEditMode ? '글 수정' : '새 글 작성'}
                </h1>
                <div className="flex items-center gap-4">
                    <AutoSaveStatus
                        isAutoSaving={isAutoSaving}
                        isDirty={form.formState.isDirty}
                        isDraftEdit={isDraftEdit}
                        lastSavedAt={lastSavedAt}
                    />
                    <Link
                        to="/admin/posts"
                        className="font-mono text-xs text-ink-faint hover:text-primary transition-colors"
                    >
                        ← 목록
                    </Link>
                </div>
            </header>

            <form className="space-y-5" onSubmit={(e) => e.preventDefault()}>
                <Input
                    label="제목 *"
                    placeholder="제목을 입력하세요"
                    {...form.register('title')}
                    error={form.formState.errors.title?.message}
                />

                <Input
                    label="요약"
                    placeholder="목록과 메타에 노출 (선택)"
                    {...form.register('summary')}
                    error={form.formState.errors.summary?.message}
                />

                {/* 썸네일 — URL 입력 + 업로드 버튼 */}
                <div className="flex flex-col gap-1.5">
                    <label className="text-sm font-medium text-ink">썸네일</label>
                    <div className="flex gap-2">
                        <Input
                            placeholder="https://... 또는 업로드"
                            {...form.register('thumbnail')}
                            error={undefined}
                            className="flex-1"
                        />
                        <label className="inline-flex">
                            <input
                                type="file"
                                accept="image/*"
                                className="hidden"
                                onChange={(e) => {
                                    const f = e.target.files?.[0]
                                    if (f) onThumbUpload(f)
                                    e.target.value = ''
                                }}
                            />
                            <span
                                className={`inline-flex items-center justify-center h-10 px-4 rounded-md text-sm font-medium cursor-pointer
                                    bg-primary-tint text-primary hover:bg-primary-mid
                                    ${thumbUploading ? 'opacity-50 cursor-not-allowed' : ''}`}
                            >
                                {thumbUploading ? `${thumbProgress}%` : '업로드'}
                            </span>
                        </label>
                    </div>
                    {thumbError && (
                        <p className="text-xs text-danger">{thumbError}</p>
                    )}
                    {thumbUploading && (
                        <div
                            role="progressbar"
                            aria-valuenow={thumbProgress ?? 0}
                            aria-valuemin={0}
                            aria-valuemax={100}
                            aria-label="썸네일 업로드 진행률"
                            className="flex items-center gap-2"
                        >
                            <div className="flex-1 h-1.5 rounded-full bg-surface-muted overflow-hidden">
                                <div
                                    className="h-full bg-primary transition-all duration-150"
                                    style={{ width: `${thumbProgress}%` }}
                                />
                            </div>
                            <span className="font-mono text-xs text-ink-faint tabular-nums w-10 text-right">
                            {thumbProgress}%
                        </span>
                        </div>
                    )}
                    {form.watch('thumbnail') && (
                        <p className="text-xs text-ink-faint break-all">
                            {form.watch('thumbnail')}
                        </p>
                    )}
                </div>

                {/* 카테고리 */}
                <div className="flex flex-col gap-1.5">
                    <Controller
                        name="categoryId"
                        control={form.control}
                        render={({ field, fieldState }) => (
                            <CategorySelect
                                value={field.value}
                                onChange={field.onChange}
                                categories={categories}
                                error={fieldState.error?.message}
                            />
                        )}
                    />
                </div>

                {/* 태그 */}
                <Controller
                    name="tags"
                    control={form.control}
                    render={({ field, fieldState }) => (
                        <TagsField
                            value={field.value ?? []}
                            onChange={field.onChange}
                            error={fieldState.error?.message}
                        />
                    )}
                />

                {/* 본문 — 마크다운(MDXEditor) / HTML 원본 */}
                <div className="flex flex-col gap-1.5">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                        <label className="text-sm font-medium text-ink">본문 *</label>
                        <FormatSwitch value={contentFormat} onChange={onChangeFormat} />
                    </div>
                    {contentFormat === 'HTML' ? (
                        <HtmlSourceEditor
                            key={editorKey}
                            value={form.watch('content')}
                            onChange={(html) => form.setValue('content', html, { shouldDirty: true })}
                            onImportTitle={onImportTitle}
                        />
                    ) : editorReady ? (
                        <Suspense
                            fallback={
                                <div className="h-[540px] border border-border rounded-lg bg-surface-muted animate-pulse" />
                            }
                        >
                            <BlogEditor
                                key={editorKey}
                                initialMarkdown={form.getValues('content')}
                                onChange={(md) =>
                                    form.setValue('content', md, { shouldDirty: true })
                                }
                                onImportTitle={onImportTitle}
                            />
                        </Suspense>
                    ) : (
                        <div className="h-[540px] border border-border rounded-lg bg-surface-muted animate-pulse" />
                    )}
                    {form.formState.errors.content && (
                        <p className="text-xs text-danger">
                            {form.formState.errors.content.message}
                        </p>
                    )}
                </div>

                {errorMessage && (
                    <Card className="border-danger bg-danger-tint">
                        <p className="text-sm text-danger">{errorMessage}</p>
                    </Card>
                )}

                {/* 액션 */}
                <div className="flex items-center justify-between pt-4 border-t border-border">
                    {isEditMode ? (
                        <>
                            <div className="flex gap-2">
                                <Button
                                    type="button"
                                    variant="primary"
                                    onClick={submitUpdate}
                                    isLoading={updateMutation.isPending}
                                    disabled={isAnyMutating}
                                >
                                    저장
                                </Button>
                                <Button
                                    type="button"
                                    variant="secondary"
                                    onClick={onTogglePublish}
                                    isLoading={togglePublishMutation.isPending}
                                    disabled={isAnyMutating}
                                >
                                    {detail?.status === 'PUBLISHED' ? '미발행으로' : '발행'}
                                </Button>
                            </div>
                            <Button
                                type="button"
                                variant="danger"
                                onClick={onDelete}
                                isLoading={deleteMutation.isPending}
                                disabled={isAnyMutating}
                            >
                                삭제
                            </Button>
                        </>
                    ) : (
                        <div className="flex gap-2">
                            <Button
                                type="button"
                                variant="secondary"
                                onClick={submitCreate('DRAFT')}
                                isLoading={
                                    createMutation.isPending &&
                                    createMutation.variables?.status === 'DRAFT'
                                }
                                disabled={isAnyMutating}
                            >
                                임시저장
                            </Button>
                            <Button
                                type="button"
                                variant="primary"
                                onClick={submitCreate('PUBLISHED')}
                                isLoading={
                                    createMutation.isPending &&
                                    createMutation.variables?.status === 'PUBLISHED'
                                }
                                disabled={isAnyMutating}
                            >
                                발행
                            </Button>
                        </div>
                    )}
                </div>
            </form>
        </section>
    )
}

function formatMutationError(err: unknown): string | null {
    if (!err) return null
    return formatApiError(err)
}
function AutoSaveStatus({
                            isAutoSaving,
                            isDirty,
                            isDraftEdit,
                            lastSavedAt,
                        }: {
    isAutoSaving: boolean
    isDirty: boolean
    isDraftEdit: boolean
    lastSavedAt: Date | null
}) {
    if (isAutoSaving) {
        return (
            <span className="flex items-center gap-1.5 font-mono text-xs text-primary">
                <span className="w-1.5 h-1.5 rounded-full bg-primary animate-pulse" />
                자동 저장 중...
            </span>
        )
    }

    if (isDirty && isDraftEdit) {
        return (
            <span className="flex items-center gap-1.5 font-mono text-xs text-warning">
                <span className="w-1.5 h-1.5 rounded-full bg-warning" />
                변경사항 있음
            </span>
        )
    }

    if (lastSavedAt) {
        return (
            <span className="flex items-center gap-1.5 font-mono text-xs text-success">
                <span className="w-1.5 h-1.5 rounded-full bg-success" />
                저장됨 · {format(lastSavedAt, 'HH:mm:ss')}
            </span>
        )
    }

    return null
}

/** 본문 형식 전환 — 마크다운 / HTML 원본 */
function FormatSwitch({ value, onChange }: { value: ContentFormat; onChange: (v: ContentFormat) => void }) {
    const options: { v: ContentFormat; label: string }[] = [
        { v: 'MARKDOWN', label: '마크다운' },
        { v: 'HTML', label: 'HTML 원본' },
    ]
    return (
        <div role="radiogroup" aria-label="본문 형식" className="inline-flex p-0.5 rounded-lg bg-surface-muted">
            {options.map((o) => (
                <button
                    key={o.v}
                    type="button"
                    role="radio"
                    aria-checked={value === o.v}
                    onClick={() => onChange(o.v)}
                    className={
                        value === o.v
                            ? 'h-8 px-3 rounded-md bg-panel text-sm font-medium text-ink shadow-xs'
                            : 'h-8 px-3 rounded-md text-sm text-ink-muted hover:text-ink'
                    }
                >
                    {o.label}
                </button>
            ))}
        </div>
    )
}
