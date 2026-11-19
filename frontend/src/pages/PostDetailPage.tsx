import { useEffect, useMemo, useRef, type ComponentProps } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { format } from 'date-fns'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

import { Card } from '@/shared/components'
import { ApiError } from '@/shared/api/ApiError'
import { usePostDetailQuery } from '@/features/post/hooks/usePostDetailQuery'
import { markAsRead } from '@/shared/lib/readTracker'
import { getTagColor } from '@/shared/lib/tagColor'
import { extractToc, createLineIdLookup, type TocItem } from '@/features/post/lib/toc'
import { TableOfContents } from '@/features/post/components/TableOfContents'
import { RunnableCodeBlock } from '@/features/post/components/RunnableCodeBlock'
import { HtmlPostFrame } from '@/features/post/components/HtmlPostFrame'
import { HtmlToc, HtmlMobileToc, useHtmlToc } from '@/features/post/components/HtmlToc'
import { Watermark } from '@/features/post/components/Watermark'
import { useCopyGuard } from '@/shared/lib/useCopyGuard'
import type { PostDetail } from '@/features/post/types'
import rehypeHighlight from 'rehype-highlight'
import 'highlight.js/styles/github-dark.css'

export default function PostDetailPage() {
    const { publicId } = useParams<{ publicId: string }>()
    const { data, isPending, isError, error } = usePostDetailQuery(publicId)
    const queryClient = useQueryClient()

    const isHtml = data?.contentFormat === 'HTML'

    // 복사·우클릭·드래그·인쇄 단축키 차단 (early return보다 위)
    useCopyGuard()

    useEffect(() => {
        if (publicId) markAsRead(publicId)
    }, [publicId])

    // 상세를 벗어날 때 목록 캐시를 stale 처리 → 목록(홈/posts) 복귀 시 서버에서 최신 조회수 재요청.
    useEffect(() => {
        return () => {
            queryClient.invalidateQueries({ queryKey: ['posts', 'list'] })
        }
    }, [queryClient])

    // ⚠ early return보다 위 (Rules of Hooks — 렌더마다 훅 개수 고정)
    // React Compiler가 data?.content를 의존성으로 추론하도록 지역 변수로 분리
    const content = data?.content
    const toc = useMemo(
        () => (content && !isHtml ? extractToc(content) : []),
        [content, isHtml],
    )

    // 줄 번호 → id. 렌더 중 카운터를 굴리면 StrictMode 이중 렌더에서 id에 -1이 붙는다.
    const lookupId = useMemo(() => createLineIdLookup(toc), [toc])

    // HTML 원본 글: iframe이 postMessage로 보내는 목차 (early return보다 위)
    const htmlWrapRef = useRef<HTMLDivElement>(null)
    const htmlToc = useHtmlToc(htmlWrapRef)

    // 해시 직접 진입 시 렌더 완료 후 앵커 이동 (SPA — 초기 로드엔 heading이 아직 없음)
    useEffect(() => {
        if (!data?.content || isHtml) return
        const hash = window.location.hash
        if (!hash) return

        const id = decodeURIComponent(hash.slice(1)) // hash는 퍼센트 인코딩된 상태로 반환됨
        const raf = requestAnimationFrame(() => {
            document.getElementById(id)?.scrollIntoView({ behavior: 'auto', block: 'start' })
        })
        return () => cancelAnimationFrame(raf)
    }, [data?.content, isHtml])

    if (isPending) {
        return (
            <div className="max-w-3xl mx-auto space-y-3">
                <div className="h-8 bg-surface-muted animate-pulse rounded" />
                <div className="h-4 bg-surface-muted animate-pulse rounded w-1/3" />
                <div className="h-64 bg-surface-muted animate-pulse rounded mt-6" />
            </div>
        )
    }

    if (isError || !data) {
        return (
            <Card className="border-danger bg-danger-tint max-w-3xl mx-auto">
                <p className="text-sm text-danger">
                    {error instanceof ApiError
                        ? `(${error.status}) ${error.message}`
                        : '글을 불러올 수 없습니다.'}
                </p>
                <Link to="/posts" className="mt-3 inline-block text-sm text-primary">
                    ← 목록으로
                </Link>
            </Card>
        )
    }

    // ── HTML 원본 글: 마크다운과 같은 2단 구조. 본문은 카드·2단 그림이 있어 60rem으로 조금 넓게 ──
    if (isHtml) {
        return (
            <div className="grid gap-10 xl:grid-cols-[minmax(0,60rem)_minmax(0,1fr)] xl:justify-center">
                <article className="min-w-0 w-full">
                    <PostHeader data={data} />
                    <HtmlMobileToc items={htmlToc} wrapRef={htmlWrapRef} />
                    <div ref={htmlWrapRef} className="relative">
                        <HtmlPostFrame html={data.content} title={data.title} />
                        <Watermark />
                    </div>
                    <PostFooter />
                </article>

                <aside className="hidden xl:block xl:max-w-[220px]">
                    <HtmlToc items={htmlToc} wrapRef={htmlWrapRef} />
                </aside>
            </div>
        )
    }

    return (
        // minmax(0,48rem) — 긴 <pre>가 그리드 컬럼을 밀어내는 것 방지 (필수)
        <div className="grid gap-10 xl:grid-cols-[minmax(0,48rem)_minmax(0,1fr)] xl:justify-center">
            <article className="min-w-0 w-full max-w-3xl mx-auto xl:mx-0">
                <PostHeader data={data} />

                <MobileToc items={toc} />

                {/*
                  마크다운 본문 — react-markdown + remark-gfm
                  heading 크기/여백은 index.css의 .post-content 규칙이 담당한다.
                  (index.css의 전역 h1~h4 element 규칙이 layer 밖이라 Tailwind 유틸을 이김)
                  space-y-4를 쓰면 margin이 충돌하므로 제거하고 각 요소에 margin을 직접 준다.
                */}
                <div className="post-content text-ink relative">
                    <Watermark />
                    <ReactMarkdown
                        remarkPlugins={[remarkGfm]}
                        rehypePlugins={[rehypeHighlight]}
                        components={{
                            // react-markdown v10은 props에 hast `node`를 포함 → DOM 누출 방지 위해 제거
                            h1: ({ node: _n, ...p }) => <h1 {...p} />,
                            // node.position.start.line = 원본 마크다운 줄 번호 (결정적)
                            h2: ({ node, children, ...rest }) => <h2 id={lookupId(node?.position?.start.line)} className="scroll-mt-8" {...rest}>{children}</h2>,
                            h3: ({ node, children, ...rest }) => <h3 id={lookupId(node?.position?.start.line)} className="scroll-mt-8" {...rest}>{children}</h3>,
                            // h4/h5는 목차에 포함하지 않음(깊이 과다) → id 불필요
                            h4: ({ node: _n, ...p }) => <h4 {...p} />,
                            h5: ({ node: _n, ...p }) => <h5 {...p} />,
                            p:  ({ node: _n, ...p }) => <p className="text-base text-ink-muted leading-[1.8] my-4" {...p} />,
                            a:  ({ node: _n, ...p }) => <a className="text-primary font-medium underline decoration-primary/40 underline-offset-2 hover:decoration-primary transition-colors" target="_blank" rel="noopener noreferrer" {...p} />,
                            ul: ({ node: _n, ...p }) => <ul className="my-4 list-disc pl-6 space-y-1.5 marker:text-ink-faint" {...p} />,
                            ol: ({ node: _n, ...p }) => <ol className="my-4 list-decimal pl-6 space-y-1.5 marker:text-ink-faint marker:font-medium" {...p} />,
                            li: ({ node: _n, ...p }) => <li className="text-base text-ink-muted leading-[1.75] pl-1" {...p} />,
                            blockquote: ({ node: _n, ...p }) => <blockquote className="my-6 border-l-[3px] border-note-bar bg-note-bg pl-4 pr-4 py-3 rounded-r text-ink-muted [&>p]:my-0 [&>p:not(:first-child)]:mt-2" {...p} />,
                            hr: ({ node: _n, ...p }) => <hr className="my-12 border-border" {...p} />,
                            th: ({ node: _n, ...p }) => <th className="px-3.5 py-2.5 text-left border-b border-border-strong" {...p} />,
                            td: ({ node: _n, ...p }) => <td className="px-3.5 py-2.5 border-b border-border text-ink-muted" {...p} />,
                            code: ({ node: _n, ...p }) => <MdCode {...p} />,
                            // Python · JavaScript 코드 블록엔 [실행] 버튼 (브라우저에서 실행). node 는 코드 원문 추출용
                            pre: ({ node, ...p }) => <RunnableCodeBlock node={node} {...p} />,
                            img: ({ node: _n, ...p }) => <img className="rounded-lg max-w-full my-5 border border-border" loading="lazy" {...p} />,
                            table: ({ node: _n, ...p }) => <MdTable {...p} />,
                        }}
                    >
                        {data.content}
                    </ReactMarkdown>
                </div>
                <PostFooter />
            </article>

            {/* 왼쪽에 이미 사이드바가 있으므로 lg가 아닌 xl에서 노출 */}
            <aside className="hidden xl:block xl:max-w-[220px]">
                <TableOfContents items={toc} />
            </aside>
        </div>
    )
}

function PostHeader({ data }: { data: PostDetail }) {
    return (
        <header className="mb-6 pb-4 border-b border-border">
            <h1 className="font-serif text-3xl font-semibold text-ink leading-tight">
                {data.title}
            </h1>

            {data.summary && (
                <p className="mt-3 text-base text-ink-muted">{data.summary}</p>
            )}

            <div className="mt-4 flex items-center gap-3 text-xs text-ink-faint font-mono">
                {data.publishedAt && (
                    <span>{format(new Date(data.publishedAt), 'yyyy년 M월 d일')}</span>
                )}
                {data.categoryName && (
                    <>
                        <span>·</span>
                        <span>{data.categoryName}</span>
                    </>
                )}
                <span>·</span>
                <span>조회 {data.viewCount.toLocaleString()}</span>
            </div>

            {data.tags.length > 0 && (
                <div className="mt-4 flex flex-wrap gap-1.5">
                    {data.tags.map((tag) => (
                        <TagChip key={tag} tag={tag} />
                    ))}
                </div>
            )}
        </header>
    )
}

function PostFooter() {
    return (
        <footer className="mt-12 pt-6 border-t border-border">
            <Link
                to="/posts"
                className="inline-flex items-center gap-1.5 font-mono text-sm text-ink-muted hover:text-primary transition-colors"
            >
                ← 목록으로
            </Link>
        </footer>
    )
}

/** xl 미만 — 본문 상단 접이식 목차. aside와 배타적으로 동작. */
function MobileToc({ items }: { items: TocItem[] }) {
    if (items.length < 2) return null

    return (
        <details className="xl:hidden mb-6 rounded-md border border-border bg-panel">
            <summary className="cursor-pointer select-none px-4 py-2.5 font-mono text-xs text-ink-muted">
                contents ({items.length})
            </summary>
            <ul className="border-t border-border px-4 py-3">
                {items.map((item) => (
                    <li key={item.id} style={{ paddingLeft: (item.level - 2) * 12 }}>
                        <MobileTocLink item={item} />
                    </li>
                ))}
            </ul>
        </details>
    )
}

function MobileTocLink({ item }: { item: TocItem }) {
    const cls = 'block py-1 font-mono text-xs text-ink-faint hover:text-primary transition-colors'
    return <a href={`#${item.id}`} className={cls}>{item.text}</a>
}

function TagChip({ tag }: { tag: string }) {
    const color = getTagColor(tag)
    return (
        <Link
            to={`/posts?tag=${encodeURIComponent(tag)}`}
            style={{ color: color.text, borderColor: color.border }}
            className="inline-flex items-center px-1.5 py-0.5 rounded font-mono text-xs border hover:bg-surface-muted transition-colors"
        >
            #{tag}
        </Link>
    )
}
/** 가로 스크롤 + 라운드 컨테이너로 감싼 표. */
function MdTable(p: ComponentProps<'table'>) {
    return (
        <div className="my-5 overflow-x-auto rounded-lg border border-border">
            <table className="border-collapse" {...p} />
        </div>
    )
}

/** 블록 코드는 <pre> 안에  */
function MdCode({ className, children, ...rest }: ComponentProps<'code'>) {
    // hljs 클래스가 붙었거나 language- 접두사가 있으면 블록 코드
    const isBlock = className?.includes('language-') || className?.includes('hljs')
    if (isBlock) {
        return <code className={className} {...rest}>{children}</code>
    }
    const cls = 'mx-0.5 px-1.5 py-0.5 rounded border border-code-border bg-code-bg font-mono text-[0.8125em] font-medium text-code'
    return <code className={cls} {...rest}>{children}</code>
}
