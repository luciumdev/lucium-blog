import { useState, type CSSProperties, type FormEvent, type ReactNode } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { cn } from '@/shared/lib/cn'
import { LuciumMascot } from '@/shared/components/LuciumLogo'
import { usePostsQuery } from '@/features/post/hooks/usePostsQuery'
import { useTagsQuery } from '@/features/tag/hooks/useTagsQuery'
import { useIndicesQuery } from '@/features/market/hooks/useIndicesQuery'
import { useForexQuery } from '@/features/market/hooks/useForexQuery'
import { useNewsQuery } from '@/features/market/hooks/useNewsQuery'
import { useNewsKeywordsQuery } from '@/features/market/hooks/useNewsKeywordsQuery'
import { useAiJournalQuery } from '@/features/ai-journal/hooks/useAiJournalQuery'
import { useSiteStatQuery } from '@/features/stat/hooks/useSiteStatQuery'
import { useCollectionStatusQuery } from '@/features/collection/hooks/useCollectionStatusQuery'
import type { IndexCode, NewsCategory } from '@/features/market/types'
import type { AiNewsImportance, AiNewsItem } from '@/features/ai-journal/types'
import { NO_RUN_CLASS, RUN_COLOR, RUN_LABEL } from '@/features/collection/runStatus'
import { RunLegend } from '@/features/collection/RunLegend'
import { splitSentences } from '@/features/ai-journal/lib/splitSentences'
import { getCategoryColor } from '@/shared/lib/categoryColor'

/**
 * 메인
 *  - 좌: 검색 → 최근 글 → 뉴스·마켓 | AI 릴리즈 노트
 *  - 우: 프로필 → 스케줄러 모니터링
 *  - lg 미만에서는 우측 열이 본문 아래로 내려감
 *  - 카드는 차례로 등장(.fx + animation-delay). 호버 효과는 실제로 클릭되는 요소(링크·버튼)에만
 */
export default function HomePage() {
    return (
        <div className="flex flex-col lg:flex-row gap-4 items-stretch">
            <div className="flex-1 min-w-0 flex flex-col gap-4">
                <SearchHero delay={0.05} />
                <RecentPosts delay={0.12} />
                {/* flex-1: 오른쪽 열이 더 길 때 이 줄이 늘어나 양쪽 카드 아래끝이 맞음 (그리드 칸은 기본 stretch) */}
                <div className="flex-1 grid grid-cols-1 md:grid-cols-2 gap-4">
                    <NewsMarketCard delay={0.19} />
                    <ReleaseNotesCard delay={0.26} />
                </div>
            </div>

            <aside className="w-full lg:w-[300px] shrink-0 flex flex-col gap-4">
                <ProfileCard delay={0.33} />
                <CollectionCard delay={0.4} />
            </aside>
        </div>
    )
}

/* ─────────────────────────── 공통 ─────────────────────────── */

function Card({ delay, className, children }: { delay: number; className?: string; children: ReactNode }) {
    const style: CSSProperties = { animationDelay: `${delay}s` }
    return (
        <section style={style} className={cn('fx bg-panel border border-border rounded-[10px]', className)}>
            {children}
        </section>
    )
}

/** 제목 + 오른쪽 링크. 밑줄 왼쪽에 짧은 선(.sec-h) — 메인 카드는 모두 기본 사이트 블루로 통일 (accent 는 예외용) */
function CardTitle({ children, to, linkLabel = '더 보기', accent }: { children: ReactNode; to?: string; linkLabel?: string; accent?: string }) {
    // accent → 제목 밑 짧은 선 색 (index.css .sec-h::after 가 --sec-accent 사용)
    const style = accent ? ({ '--sec-accent': accent } as CSSProperties) : undefined
    return (
        <div style={style} className="sec-h flex items-baseline justify-between gap-3 pb-2.5 border-b border-border">
            <h2 className="text-[17px] font-bold">{children}</h2>
            {to && (
                <Link to={to} className="text-sm text-ink-faint hover:text-primary-hover transition-colors">
                    {linkLabel}
                </Link>
            )}
        </div>
    )
}

function Skeleton({ className }: { className?: string }) {
    return <span className={cn('block bg-panel-elevated rounded animate-pulse', className)} />
}

/* ─────────────────────────── 검색 ─────────────────────────── */

const POPULAR_TAG_COUNT = 3

function SearchHero({ delay }: { delay: number }) {
    const navigate = useNavigate()
    const [q, setQ] = useState('')
    const [error, setError] = useState(false)
    const { data: tags } = useTagsQuery()

    const submit = (e: FormEvent) => {
        e.preventDefault()
        const trimmed = q.trim()
        if (trimmed.length < 2) {
            setError(true)
            return
        }
        navigate(`/search?q=${encodeURIComponent(trimmed)}`)
    }

    const popular = (tags ?? []).filter((t) => t.postCount > 0).slice(0, POPULAR_TAG_COUNT)

    return (
        <Card delay={delay} className="px-5 sm:px-7 py-4">
            <form role="search" onSubmit={submit} className="max-w-[760px] mx-auto">
                <label htmlFor="home-q" className="sr-only">글 검색</label>
                <div className="search-pill flex items-center gap-3 h-[50px] pl-5 pr-2 rounded-full">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" className="shrink-0 text-primary" aria-hidden>
                        <circle cx="11" cy="11" r="6.5" />
                        <path d="m20 20-4-4" />
                    </svg>
                    <input
                        id="home-q"
                        type="search"
                        value={q}
                        onChange={(e) => {
                            setQ(e.target.value)
                            setError(false)
                        }}
                        placeholder="lucium.dev 글 검색"
                        aria-invalid={error}
                        aria-describedby={error ? 'home-q-error' : undefined}
                        className="flex-1 min-w-0 bg-transparent text-base sm:text-[17px] text-ink placeholder:text-ink-faint focus:outline-none"
                    />
                    <kbd className="hidden sm:inline font-sans text-xs text-primary-hover bg-panel border border-[#BFDDF2] rounded-md px-2 py-0.5">/</kbd>
                    <button
                        type="submit"
                        aria-label="검색"
                        className="search-go w-10 h-10 shrink-0 flex items-center justify-center rounded-full bg-primary text-white hover:bg-primary-hover"
                    >
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
                            <path d="M5 12h14M13 6l6 6-6 6" />
                        </svg>
                    </button>
                </div>
                {error && (
                    <p id="home-q-error" className="mt-2 text-center text-sm text-danger">2자 이상 입력해 주세요.</p>
                )}
            </form>

            {popular.length > 0 && (
                <p className="mt-3.5 flex flex-wrap items-center justify-center gap-2 text-sm text-ink-faint">
                    인기 태그
                    {popular.map((tag) => (
                        <Link
                            key={tag.id}
                            to={`/posts?tag=${encodeURIComponent(tag.slug)}`}
                            className="px-3 py-0.5 rounded-full bg-primary-subtle text-primary-hover hover:bg-primary-tint transition-colors"
                        >
                            {tag.name}
                        </Link>
                    ))}
                </p>
            )}
        </Card>
    )
}

/* ─────────────────────────── 뉴스·마켓 ─────────────────────────── */

const INDICATORS: { code: IndexCode; label: string }[] = [
    { code: 'NASDAQ_PROXY', label: 'QQQ' },
    { code: 'KOSPI', label: 'KOSPI' },
]

function NewsMarketCard({ delay }: { delay: number }) {
    const indices = useIndicesQuery()
    const forex = useForexQuery()
    const keywords = useNewsKeywordsQuery(1)
    const usd = forex.data?.rates.find((r) => r.currencyCode === 'USD')

    const items = [
        ...INDICATORS.map(({ code, label }) => {
            const latest = indices.data?.series.find((s) => s.code === code)?.latest
            return { label, value: latest?.close, rate: latest?.changeRate }
        }),
        { label: '원/달러', value: usd?.rate, rate: usd?.changeRate },
    ]
    const pending = indices.isPending || forex.isPending
    const top = (keywords.data?.keywords ?? []).slice(0, 3)

    return (
        <Card delay={delay} className="px-[22px] pt-[18px] pb-2 flex flex-col">
            <CardTitle to="/market">뉴스·마켓</CardTitle>

            <dl className="mt-3.5 grid grid-cols-3 gap-x-4 tabular-nums">
                {items.map((item) => (
                    <div key={item.label} className="min-w-0">
                        <dt className="text-[13px] text-ink-faint">{item.label}</dt>
                        <dd className="mt-0.5">
                            {pending ? (
                                <Skeleton className="h-5 w-20" />
                            ) : (
                                <span className="text-base font-semibold">
                                    {item.value != null ? formatNumber(item.value) : '—'}{' '}
                                    <SignedRate rate={item.rate} />
                                </span>
                            )}
                        </dd>
                    </div>
                ))}
            </dl>

            {top.length > 0 && (
                <p className="mt-3 mb-2.5 text-sm text-ink-muted">
                    테크·경제에 함께 나온 말
                    {top.map((k, i) => (
                        <Link
                            key={k.keyword}
                            to="/market"
                            className={cn('ml-2 transition-colors', i === 0 ? 'hl font-semibold text-ink' : 'hover:text-primary-hover')}
                        >
                            {k.keyword}
                        </Link>
                    ))}
                </p>
            )}

            <div className={cn(top.length === 0 && 'mt-3')}>
                <Headline category="IT" label="테크" labelClass="text-primary" />
                <Headline category="ECONOMY" label="경제" labelClass="text-[#8A5A00]" />
            </div>
        </Card>
    )
}

/** 분야별 최신 기사 1건 — 테크는 번역 제목 우선 */
function Headline({ category, label, labelClass }: { category: NewsCategory; label: string; labelClass: string }) {
    const { data, isPending } = useNewsQuery(category)
    const item = data?.items[0]

    if (isPending) {
        return (
            <div className="py-2.5 border-t border-border space-y-1.5">
                <Skeleton className="h-4 w-4/5" />
                <Skeleton className="h-3 w-24" />
            </div>
        )
    }
    if (!item) return null

    return (
        <a
            href={item.url}
            target="_blank"
            rel="noopener noreferrer"
            className="row block -mx-2 px-2 py-2.5 border-t border-border"
        >
            <span className="block text-[15px] font-semibold leading-normal truncate">{item.titleKo ?? item.title}</span>
            <span className="block mt-0.5 text-[13px] text-ink-faint">
                <b className={cn('font-semibold', labelClass)}>{label}</b> · {relativeTime(item.publishedAt)}
            </span>
        </a>
    )
}

/** 국내 관례: 상승 빨강 + / 하락 파랑 − */
function SignedRate({ rate }: { rate?: number | null }) {
    if (rate == null || Number.isNaN(rate)) return null
    const up = rate > 0
    const down = rate < 0
    return (
        <span className={cn(up ? 'text-up' : down ? 'text-down' : 'text-ink-faint')}>
            {up ? '+' : down ? '−' : ''}
            {Math.abs(rate).toFixed(2)}%
        </span>
    )
}

/* ─────────────────────────── AI 릴리즈 노트 ─────────────────────────── */

const IMPORTANCE_ORDER: Record<AiNewsImportance, number> = { HIGH: 0, MEDIUM: 1, LOW: 2 }
const RELEASE_COUNT = 3

/** 중요도 높은 순 상위 N건 — 같은 중요도는 수집 순서 유지 */
function pickTopItems(items: AiNewsItem[], count: number): AiNewsItem[] {
    return items
        .map((item, index) => ({ item, index }))
        .sort((a, b) => IMPORTANCE_ORDER[a.item.important] - IMPORTANCE_ORDER[b.item.important] || a.index - b.index)
        .slice(0, count)
        .map(({ item }) => item)
}

/**
 * 메인에서는 수집일 안내("M/d 수집본…")를 표시하지 않음 — AI 릴리즈 노트 페이지에서만.
 * 오류여도 이전 데이터(재조회 실패 · localStorage 마지막 성공본)가 있으면 그대로 표시.
 */
function ReleaseNotesCard({ delay }: { delay: number }) {
    const { data, isPending, isError } = useAiJournalQuery()
    const briefing = data?.briefing
    const items = pickTopItems(briefing?.items ?? [], RELEASE_COUNT)
    const summary = briefing ? splitSentences(briefing.daily_summary)[0] : undefined

    return (
        <Card delay={delay} className="px-[22px] pt-[18px] pb-2 flex flex-col">
            <CardTitle to="/ai-journal">AI 릴리즈 노트</CardTitle>

            {isPending && !briefing ? (
                <div className="mt-3.5 space-y-2" aria-busy="true">
                    <Skeleton className="h-4 w-full" />
                    <Skeleton className="h-4 w-4/5" />
                    <Skeleton className="h-4 w-3/5" />
                </div>
            ) : items.length === 0 ? (
                <p className="mt-3.5 text-[15px] text-ink-faint">
                    {isError ? 'AI 릴리즈 노트를 불러오지 못했습니다.' : '아직 수집된 소식이 없습니다.'}
                </p>
            ) : (
                <>
                    {summary && (
                        <p className="mt-3 mb-2.5 text-sm leading-relaxed text-ink-muted line-clamp-2 [text-wrap:pretty]">{summary}</p>
                    )}
                    <ol>
                        {items.map((item) => {
                            const important = item.important === 'HIGH'
                            return (
                                <li key={item.url} className="border-t border-border">
                                    {/* 줄 전체가 링크 — 호버 배경이 깔리는 영역 = 클릭되는 영역 */}
                                    <a
                                        href={item.url}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        className="row block -mx-2 px-2 py-2.5"
                                    >
                                        <span className="block text-[15px] font-semibold leading-normal truncate">
                                            {/* 중요도는 테두리 대신 작은 배지 — 선택 상태(배경)와 의미를 분리 */}
                                            {important && (
                                                <span className="mr-1.5 px-1.5 py-px rounded text-[11px] font-bold text-primary-hover bg-primary-tint align-[1px]">
                                                    중요
                                                </span>
                                            )}
                                            {item.title}
                                        </span>
                                        <span className="block mt-0.5 text-[13px] text-ink-faint">{formatSource(item.source)}</span>
                                    </a>
                                </li>
                            )
                        })}
                    </ol>
                </>
            )}
        </Card>
    )
}

/** "github:openai/openai-python" → "GitHub · openai/openai-python" */
function formatSource(source: string) {
    const [kind, ...rest] = source.split(':')
    if (kind === 'github' && rest.length > 0) return `GitHub · ${rest.join(':')}`
    return source
}

/* ─────────────────────────── 최근 글 ─────────────────────────── */

const RECENT_COUNT = 3

function RecentPosts({ delay }: { delay: number }) {
    const { data, isPending, isError } = usePostsQuery({ size: RECENT_COUNT })

    return (
        <Card delay={delay} className="px-[22px] pt-[18px] pb-1.5">
            <CardTitle to="/posts" linkLabel="전체 보기">최근 글</CardTitle>

            {isPending ? (
                <ul aria-busy="true">
                    {Array.from({ length: RECENT_COUNT }).map((_, i) => (
                        <li key={i} className="flex justify-between gap-4 py-3">
                            <Skeleton className="h-5 w-2/3" />
                            <Skeleton className="h-4 w-12" />
                        </li>
                    ))}
                </ul>
            ) : isError ? (
                <p className="py-6 text-[15px] text-danger">글을 불러오지 못했습니다.</p>
            ) : !data || data.content.length === 0 ? (
                <p className="py-6 text-[15px] text-ink-faint">아직 발행된 글이 없습니다.</p>
            ) : (
                <ul className="divide-y divide-border">
                    {data.content.map((post) => (
                        <li key={post.publicId}>
                            <Link
                                to={`/posts/${post.publicId}`}
                                className="post row flex items-baseline justify-between gap-4 -mx-2 px-2 py-2.5"
                            >
                                <span className="min-w-0 flex items-baseline gap-2">
                                    {post.categoryName && <CategoryBadge name={post.categoryName} />}
                                    <span className="min-w-0 pr-1.5 truncate text-base font-medium">
                                        <span className="t">{post.title}</span>
                                    </span>
                                </span>
                                <span className="shrink-0 flex gap-2 text-[13px] text-ink-faint tabular-nums">
                                    <span className="arr" aria-hidden>→</span>
                                    {formatDate(post.publishedAt ?? post.createdAt)}
                                </span>
                            </Link>
                        </li>
                    ))}
                </ul>
            )}
        </Card>
    )
}

/** 카테고리 배지 — 색은 사이드바 하위 메뉴 점과 같은 categoryColor */
function CategoryBadge({ name }: { name: string }) {
    const c = getCategoryColor(name)
    return (
        <span
            style={{ color: c.fg, backgroundColor: c.bg }}
            className="shrink-0 px-2 py-px rounded-full text-xs font-semibold"
        >
            {name}
        </span>
    )
}

/* ─────────────────────────── 프로필 ─────────────────────────── */

function ProfileCard({ delay }: { delay: number }) {
    const { data, isPending } = useSiteStatQuery()

    return (
        <Card delay={delay} className="p-[22px]">
            <div className="flex items-center gap-3">
                <span className="w-14 h-14 shrink-0 rounded-full bg-primary-tint shadow-[inset_0_0_0_1px_#CFE5F5] flex items-center justify-center overflow-hidden">
                    <LuciumMascot size={44} />
                </span>
                <div className="min-w-0">
                    <p className="text-[17px] font-bold">lucium</p>
                    <p className="text-sm text-ink-faint">풀스택 개발자</p>
                </div>
            </div>

            {/* 방문자 = visitor_id 쿠키 기준 순 방문자 (서버 Redis HyperLogLog) */}
            <dl className="mt-[18px] pt-3.5 border-t border-border grid grid-cols-3 gap-2">
                <Stat label="게시글" value={data?.publishedPosts} pending={isPending} />
                <Stat label="오늘 방문" value={data?.todayVisitors} pending={isPending} dot />
                <Stat label="누적 방문" value={data?.totalVisitors} pending={isPending} />
            </dl>
        </Card>
    )
}

function Stat({ label, value, pending, dot }: { label: string; value?: number; pending: boolean; dot?: boolean }) {
    return (
        <div className="min-w-0">
            <dt className="flex items-center gap-1.5 text-xs text-ink-faint whitespace-nowrap">
                {dot && <span aria-hidden className="w-1.5 h-1.5 rounded-full bg-primary" />}
                {label}
            </dt>
            <dd className="text-[19px] font-semibold tabular-nums">
                {pending ? <Skeleton className="mt-1 h-6 w-12" /> : (value ?? 0).toLocaleString()}
            </dd>
        </div>
    )
}

function MiniStat({ label, value }: { label: string; value: string }) {
    return (
        <div className="min-w-0">
            <dt className="text-xs text-ink-faint truncate">{label}</dt>
            <dd className="text-[19px] font-semibold tabular-nums">{value}</dd>
        </div>
    )
}

/* ─────────────────────────── 데이터 수집 ─────────────────────────── */

/** 실행 기록이 없는 작업에 보여줄 빈 칸 수 (하루 1번 작업의 recentSize 와 같게) */
const EMPTY_SLOTS = 14

/** 배포가 뜸해도 항상 최신인 정보 — 스케줄러 실행 기록(collection_runs) */
function CollectionCard({ delay }: { delay: number }) {
    const { data, isPending, isError } = useCollectionStatusQuery()

    return (
        <Card delay={delay} className="flex-1 flex flex-col px-5 pt-[18px] pb-3">
            <CardTitle>스케줄러 모니터링</CardTitle>

            {isPending && !data ? (
                <div className="mt-4 space-y-3" aria-busy="true">
                    <Skeleton className="h-10 w-full" />
                    {Array.from({ length: 4 }).map((_, i) => (
                        <Skeleton key={i} className="h-8 w-full" />
                    ))}
                </div>
            ) : !data ? (
                <p className="mt-4 text-[15px] text-ink-faint">{isError ? '수집 현황을 불러오지 못했습니다.' : '아직 기록이 없습니다.'}</p>
            ) : (
                <>
                    {/* 요약 3칸 — 프로필 카드의 통계와 같은 모양 */}
                    <dl className="mt-3.5 mb-3 grid grid-cols-3 gap-2">
                        <MiniStat label="수집한 기사" value={data.todayArticles.toLocaleString()} />
                        <MiniStat label="번역" value={data.todayTranslated.toLocaleString()} />
                        <MiniStat label="7일 성공률" value={data.successRate7d != null ? `${data.successRate7d}%` : '—'} />
                    </dl>

                    <ul>
                        {data.jobs.map((job) => (
                            <li key={job.job} className="py-1.5 border-t border-border">
                                <div className="flex justify-between gap-2 text-sm">
                                    <span className="font-semibold">
                                        {job.label}
                                        <span className="ml-1.5 text-xs font-normal text-ink-faint">{job.cycle}</span>
                                    </span>
                                    <span className="text-[13px] text-ink-faint">{job.lastRunAt ? relativeTime(job.lastRunAt) : '첫 실행 대기'}</span>
                                </div>
                                {job.recent.length > 0 ? (
                                    <div className="bars flex gap-0.5 mt-1.5" role="img" aria-label={`최근 ${job.recent.length}회 실행 상태`}>
                                        {job.recent.map((status, i) => (
                                            <span
                                                key={i}
                                                title={RUN_LABEL[status]}
                                                className={cn('flex-1 h-2.5 rounded-[2px]', RUN_COLOR[status])}
                                                style={{ animationDelay: `${0.6 + i * 0.015}s` }}
                                            />
                                        ))}
                                    </div>
                                ) : (
                                    // 아직 한 번도 안 돈 작업 — 빈 칸 자리만 점선으로 (줄이 비어 보이지 않게)
                                    <div className="flex gap-0.5 mt-1.5" role="img" aria-label="아직 실행 기록 없음">
                                        {Array.from({ length: EMPTY_SLOTS }).map((_, i) => (
                                            <span key={i} className={cn('flex-1 h-2.5 rounded-[2px]', NO_RUN_CLASS)} />
                                        ))}
                                    </div>
                                )}
                            </li>
                        ))}
                    </ul>

                    {/* 범례 — 자세한 설명은 /status 페이지에 */}
                    <RunLegend className="mt-auto pt-3" />
                    {/* DB 실행 로그 상세 — /status */}
                    <div className="mt-3 pt-2 border-t border-border">
                        <Link
                            to="/status"
                            className="-mx-2 px-2 py-1.5 flex items-center justify-between rounded-md text-[13px] font-medium text-ink-muted hover:bg-surface-muted hover:text-ink transition-colors"
                        >
                            실행 이력 자세히 보기
                            <span aria-hidden>→</span>
                        </Link>
                    </div>
                </>
            )}
        </Card>
    )
}

/* ─────────────────────────── utils ─────────────────────────── */

function formatNumber(n: number) {
    return n.toLocaleString('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** "YYYY-MM-DDTHH:mm:ss" → "MM.DD" */
function formatDate(iso: string) {
    const d = new Date(iso)
    if (Number.isNaN(d.getTime())) return ''
    return `${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
}

/** 7일 이내는 상대 시각, 그 이상은 M/d */
function relativeTime(iso: string): string {
    const t = Date.parse(iso)
    if (Number.isNaN(t)) return ''
    const diffMin = Math.floor((Date.now() - t) / 60_000)
    if (diffMin < 1) return '방금 전'
    if (diffMin < 60) return `${diffMin}분 전`
    const h = Math.floor(diffMin / 60)
    if (h < 24) return `${h}시간 전`
    const d = Math.floor(h / 24)
    if (d < 7) return `${d}일 전`
    const date = new Date(t)
    return `${date.getMonth() + 1}/${date.getDate()}`
}
