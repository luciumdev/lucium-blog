import { useRef, useState } from 'react'
import { flushSync } from 'react-dom'
import { IndicesMultiChart } from '@/features/market/components/IndicesMultiChart'
import { useForexQuery } from '@/features/market/hooks/useForexQuery'
import { useIndicesQuery } from '@/features/market/hooks/useIndicesQuery'
import { useKeywordTrendQuery } from '@/features/market/hooks/useKeywordTrendQuery'
import { useNewsKeywordsQuery } from '@/features/market/hooks/useNewsKeywordsQuery'
import { useNewsQuery } from '@/features/market/hooks/useNewsQuery'
import type {
    IndexCode,
    IndexSeries,
    IndicatorChip,
    KeywordCount,
    NewsCategory,
    NewsItem,
} from '@/features/market/types'
import { cn } from '@/shared/lib/cn'

/**
 * 뉴스·마켓 — IT·경제 뉴스 데스크 (P 시안 v2).
 *
 *  1행: 제목 + 날짜 + 수집 상태 / 지표 한 줄
 *  2행: 테크·경제 공통 키워드(ES terms 집계) | 선택 키워드 최근 5일 추이(ES date_histogram)
 *  3행: 테크(HackerNews, 번역 제목) | 경제(한국경제) — 같은 높이 4건씩, 게시일 지표 칩
 *  4행: 지표 차트 (토글)
 *
 *  보류: 기간 탭(오늘/어제/이번 주) — 뉴스 API에 기간 조건 추가 필요.
 */
const KEYWORD_DAYS = 1
const TREND_DAYS = 5

export default function MarketPage() {
    const [chartOpen, setChartOpen] = useState(false)
    const [selected, setSelected] = useState<string | null>(null)
    const chartRef = useRef<HTMLElement>(null)
    const keywordsQ = useNewsKeywordsQuery(KEYWORD_DAYS)
    const keywords = keywordsQ.data?.keywords ?? []
    // 차트가 렌더된 뒤에 스크롤해야 문서 높이가 확보됨 → flushSync로 커밋 후 스크롤
    const active =
        selected && keywords.some((k) => k.keyword === selected) ? selected : (keywords[0]?.keyword ?? null)

    const openChart = () => {
        flushSync(() => setChartOpen(true))
        chartRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }

    // ...기존 keywordsQ, active 로직 그대로

    return (
        <div className="grid grid-cols-1 gap-4">
            <DeskHeader onOpenChart={openChart} />

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
                <KeywordPanel
                    keywords={keywords}
                    active={active}
                    isPending={keywordsQ.isPending}
                    isError={keywordsQ.isError && keywords.length === 0}
                    onSelect={setSelected}
                />
                <TrendPanel keyword={active} />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                <NewsColumn category="IT" title="테크" source="HackerNews" keyword={active} />
                <NewsColumn category="ECONOMY" title="경제" source="한국경제" keyword={active} />
            </div>

            <section className="bg-panel rounded-xl border border-border">
                <button
                    type="button"
                    onClick={() => setChartOpen((v) => !v)}
                    aria-expanded={chartOpen}
                    aria-controls="indices-chart"
                    className="w-full flex items-center justify-between px-6 py-4 text-left"
                >
                    <span className="text-base font-bold">지표 차트</span>
                    <span className="text-sm text-ink-faint">
                        {chartOpen ? '접기' : '최근 30일 보기'}
                    </span>
                </button>
                {chartOpen && (
                    <div id="indices-chart" className="px-2 pb-2">
                        <IndicesMultiChart days={30} />
                    </div>
                )}
            </section>
            <section ref={chartRef} className="scroll-mt-4 bg-panel rounded-xl border border-border">
                {/* 내부 토글 버튼·차트 그대로 */}
            </section>
        </div>
    )
}

/* ─────────────────────────── 공통 키워드 ─────────────────────────── */

function KeywordPanel({
    keywords,
    active,
    isPending,
    isError,
    onSelect,
}: {
    keywords: KeywordCount[]
    active: string | null
    isPending: boolean
    isError: boolean
    onSelect: (keyword: string) => void
}) {
    const current = keywords.find((k) => k.keyword === active)

    return (
        <section className="lg:col-span-2 bg-panel rounded-xl border border-border px-6 py-5">
            <h2 className="flex items-baseline gap-2 text-base font-bold">
                테크·경제 공통 키워드
                <span className="text-[13px] font-normal text-ink-faint">최근 24시간</span>
            </h2>

            {isPending ? (
                <div className="mt-4 flex flex-wrap gap-3" aria-busy="true">
                    {Array.from({ length: 5 }).map((_, i) => (
                        <span key={i} className="h-5 w-16 bg-panel-elevated rounded animate-pulse" />
                    ))}
                </div>
            ) : isError ? (
                <p className="mt-4 text-[15px] text-danger">키워드를 불러오지 못했습니다.</p>
            ) : keywords.length === 0 ? (
                <p className="mt-4 text-[15px] text-ink-faint">
                    아직 테크와 경제에 함께 나온 키워드가 없습니다.
                </p>
            ) : (
                <>
                    <div className="mt-3.5 flex flex-wrap gap-x-5 gap-y-1.5" role="group" aria-label="공통 키워드">
                        {keywords.map((k) => {
                            const on = k.keyword === active
                            return (
                                <button
                                    key={k.keyword}
                                    type="button"
                                    aria-pressed={on}
                                    onClick={() => onSelect(k.keyword)}
                                    className={cn(
                                        'py-0.5 text-base underline-offset-[5px] decoration-2 transition-colors',
                                        on
                                            ? 'font-bold text-primary-hover underline'
                                            : 'text-ink hover:text-primary hover:underline',
                                    )}
                                >
                                    {k.keyword}
                                    <span className="ml-1 text-[13px] font-medium text-ink-faint">
                                        {k.tech + k.economy}
                                    </span>
                                </button>
                            )
                        })}
                    </div>
                    {current && (
                        <p className="mt-3.5 text-sm text-ink-muted">
                            {current.keyword} — 테크 {current.tech}건 · 경제 {current.economy}건
                        </p>
                    )}
                </>
            )}
        </section>
    )
}

/* ─────────────────────────── 키워드 추이 ─────────────────────────── */

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']

function TrendPanel({ keyword }: { keyword: string | null }) {
    const { data, isPending, isError } = useKeywordTrendQuery(keyword, TREND_DAYS)
    const series = data?.series ?? []   // keepPreviousData: 키워드 전환 중엔 이전 막대 유지
    const max = Math.max(1, ...series.map((d) => d.count))

    return (
        <section className="bg-panel rounded-xl border border-border px-6 py-5 flex flex-col">
            <h2 className="text-base font-bold truncate">
                {keyword ? `${keyword} · 최근 ${TREND_DAYS}일` : `키워드 추이`}
            </h2>

            {!keyword ? (
                <p className="mt-4 text-[15px] text-ink-faint">키워드를 고르면 일별 기사 수를 보여줍니다.</p>
            ) : isPending ? (
                <div className="mt-4 h-[96px] bg-panel-elevated rounded animate-pulse" aria-busy="true" />
            ) : isError && series.length === 0 ? (
                <p className="mt-4 text-[15px] text-danger">추이를 불러오지 못했습니다.</p>
            ) : (
                <>
                    <ol className="mt-3 flex items-end gap-2.5 h-[96px] border-b border-border" aria-label="일별 기사 수">
                        {series.map((d, i) => {
                            const last = i === series.length - 1
                            return (
                                <li key={d.date} className="flex-1 h-full flex flex-col justify-end items-center gap-1">
                                    <span className="text-xs tabular-nums text-ink-faint">{d.count}</span>
                                    <span
                                        className={cn('w-full rounded-t-sm', last ? 'bg-primary' : 'bg-[#BFD9EE]')}
                                        style={{ height: `${Math.max(d.count === 0 ? 0 : 6, (d.count / max) * 72)}px` }}
                                        aria-label={`${d.date} ${d.count}건`}
                                    />
                                </li>
                            )
                        })}
                    </ol>
                    <div className="mt-1 flex gap-2.5 text-xs text-ink-faint text-center">
                        {series.map((d) => (
                            <span key={d.date} className="flex-1">{weekdayLabel(d.date)}</span>
                        ))}
                    </div>
                    <p className="mt-2 text-[13px] text-ink-faint">하루 기사 수 (테크+경제)</p>
                </>
            )}
        </section>
    )
}

/** "YYYY-MM-DD" → 요일. Date 파싱 시 UTC 자정이 되므로 getUTCDay 로 날짜 밀림 방지 */
function weekdayLabel(date: string): string {
    const t = Date.parse(`${date}T00:00:00Z`)
    return Number.isNaN(t) ? date : WEEKDAYS[new Date(t).getUTCDay()]
}

/* ─────────────────────────── 헤더 + 지표 ─────────────────────────── */

const INDEX_ITEMS: { code: IndexCode; label: string }[] = [
    { code: 'NASDAQ_PROXY', label: '나스닥 (QQQ)' },
    { code: 'SP500_PROXY', label: 'S&P 500 (SPY)' },
    { code: 'KOSPI', label: 'KOSPI' },
    { code: 'KOSDAQ', label: 'KOSDAQ' },
]

interface Indicator {
    label: string
    value?: number
    rate?: number | null
}

function DeskHeader({ onOpenChart }: { onOpenChart: () => void }) {
    const indices = useIndicesQuery()
    const forex = useForexQuery()
    const usd = forex.data?.rates.find((r) => r.currencyCode === 'USD')

    const items: Indicator[] = [
        ...INDEX_ITEMS.slice(0, 2).map(({ code, label }) => toIndicator(indices.data?.series, code, label)),
        { label: '원/달러', value: usd?.rate, rate: usd?.changeRate },
        ...INDEX_ITEMS.slice(2).map(({ code, label }) => toIndicator(indices.data?.series, code, label)),
    ]

    const isPending = indices.isPending || forex.isPending

    return (
        <section className="bg-panel rounded-xl border border-border px-6 py-5">
            <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-2">
                <h1 className="text-2xl font-bold tracking-tight">
                    뉴스·마켓
                    <span className="ml-2 text-[15px] font-medium text-ink-faint">{todayLabel()}</span>
                </h1>
                <div className="flex items-center gap-3">
                    <CollectStatus />
                    <button
                        type="button"
                        onClick={onOpenChart}
                        className="rounded-md border border-border px-2.5 py-1 text-sm text-ink-muted hover:text-primary hover:border-primary transition-colors"
                    >
                        지표 차트 ↓
                    </button>
                </div>
            </div>

            <dl className="mt-4 pt-3 border-t border-border grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-x-4 gap-y-3">
                {items.map((item) => (
                    <div key={item.label} className="min-w-0">
                        <dt className="text-[13px] text-ink-faint truncate">{item.label}</dt>
                        <dd className="mt-0.5 flex flex-wrap items-baseline gap-x-1.5 tabular-nums">
                            {isPending ? (
                                <span className="block h-5 w-20 bg-panel-elevated rounded animate-pulse" />
                            ) : (
                                <>
                                    <span className="text-[15px] font-semibold">
                                        {item.value != null ? formatNumber(item.value) : '—'}
                                    </span>
                                    <SignedRate rate={item.rate} />
                                </>
                            )}
                        </dd>
                    </div>
                ))}
            </dl>
        </section>
    )
}

function toIndicator(
    series: IndexSeries[] | undefined,
    code: IndexCode,
    label: string,
): Indicator {
    const latest = series?.find((s) => s.code === code)?.latest
    return { label, value: latest?.close, rate: latest?.changeRate }
}

/** 국내 관례: 상승 빨강 + / 하락 파랑 − */
function SignedRate({ rate }: { rate?: number | null }) {
    if (rate == null || Number.isNaN(rate)) return null
    const up = rate > 0
    const down = rate < 0
    return (
        <span className={cn('text-sm font-medium', up ? 'text-up' : down ? 'text-down' : 'text-ink-faint')}>
            {up ? '+' : down ? '−' : ''}
            {Math.abs(rate).toFixed(2)}%
        </span>
    )
}

/**
 * 수집 상태 한 줄. 실제 수집 시각 API(collection_runs)가 생기기 전까지는
 * 가장 최근 기사 시각으로 대신 표시한다 (클라이언트 fetch 시각은 쓰지 않음).
 */
function CollectStatus() {
    const it = useNewsQuery('IT')
    const eco = useNewsQuery('ECONOMY')

    const hasError = it.isError || eco.isError
    const latest = latestPublishedAt([...(it.data?.items ?? []), ...(eco.data?.items ?? [])])

    if (it.isPending && eco.isPending) {
        return <span className="h-4 w-36 bg-panel-elevated rounded animate-pulse" />
    }

    return (
        <p className="flex items-center gap-1.5 text-sm text-ink-muted">
            <span
                aria-hidden
                className={cn('inline-block w-2 h-2 rounded-full', hasError ? 'bg-danger' : 'bg-primary')}
            />
            {hasError ? '일부 뉴스를 불러오지 못했습니다' : '수집 정상'}
            {latest && (
                <>
                    <span aria-hidden className="text-ink-faint">·</span>
                    <span>최신 기사 {relativeTime(latest)}</span>
                </>
            )}
        </p>
    )
}

function latestPublishedAt(items: NewsItem[]): string | null {
    let best: number | null = null
    let bestIso: string | null = null
    for (const item of items) {
        const t = Date.parse(item.publishedAt)
        if (Number.isNaN(t)) continue
        if (best == null || t > best) {
            best = t
            bestIso = item.publishedAt
        }
    }
    return bestIso
}

/* ─────────────────────────── 뉴스 칼럼 ─────────────────────────── */

const MAX_ITEMS = 4

function NewsColumn({
    category,
    title,
    source,
    keyword,
}: {
    category: NewsCategory
    title: string
    source: string
    keyword: string | null
}) {
    const { data, isPending, isError, refetch } = useNewsQuery(category)
    const items = data?.items.slice(0, MAX_ITEMS) ?? []

    return (
        <section className="bg-panel rounded-xl border border-border px-6 pt-5 pb-1.5 flex flex-col">
            <h2 className="pb-2 border-b-2 border-ink text-[19px] font-bold">
                {title}
                <span className="ml-1.5 text-[13px] font-normal text-ink-faint">{source}</span>
            </h2>

            {/* 데이터 우선: 재조회 실패여도 이전 데이터가 있으면 그대로 보여준다 */}
            {items.length > 0 ? (
                <ul className="divide-y divide-border">
                    {items.map((item, i) => (
                        <NewsRow key={`${item.url}-${i}`} item={item} category={category} keyword={keyword} />
                    ))}
                </ul>
            ) : isPending ? (
                <NewsSkeleton />
            ) : isError ? (
                <div className="py-8 text-center">
                    <p className="text-[15px] text-danger">뉴스를 불러오지 못했습니다.</p>
                    <button
                        type="button"
                        onClick={() => void refetch()}
                        className="mt-2 text-sm text-primary hover:text-primary-hover underline underline-offset-4"
                    >
                        다시 시도
                    </button>
                </div>
            ) : (
                <p className="py-8 text-center text-[15px] text-ink-faint">아직 수집된 뉴스가 없습니다.</p>
            )}
        </section>
    )
}

/**
 * 한 칸 높이 고정(122px) — 제목 2줄 / 보조 1줄 / 메타 1줄. 양쪽 칼럼 행이 같은 높이로 맞는다.
 *  - 테크: 번역 제목 → 원문 제목(보조) / 번역 전이면 원문 + "번역 전"
 *  - 경제: 제목 → 요약(보조)
 */
function NewsRow({ item, category, keyword }: { item: NewsItem; category: NewsCategory; keyword: string | null }) {
    const translated = category === 'IT' && !!item.titleKo
    const heading = translated ? (item.titleKo as string) : item.title
    const sub = translated ? item.title : item.summary?.trim() || hostname(item.url)
    const pendingTranslation = category === 'IT' && !item.titleKo

    return (
        <li>
            <a
                href={item.url}
                target="_blank"
                rel="noopener noreferrer"
                className="row h-[122px] -mx-2 px-2 py-3 flex flex-col overflow-hidden"
            >
                <span className="text-base font-semibold leading-normal line-clamp-2">
                    <Highlight text={heading} keyword={keyword} />
                </span>
                {sub && (
                    <span className="mt-1 text-[13px] text-ink-faint line-clamp-1" lang={translated ? 'en' : undefined}>
                        {sub}
                    </span>
                )}
                <span className="mt-auto flex items-center gap-1.5 text-[13px] text-ink-faint whitespace-nowrap overflow-hidden">
                    <span>{relativeTime(item.publishedAt)}</span>
                    {(item.indicators ?? []).map((chip) => (
                        <ChipText key={chip.code} chip={chip} />
                    ))}
                    {pendingTranslation && (
                        <>
                            <span aria-hidden>·</span>
                            <span>번역 전</span>
                        </>
                    )}
                </span>
            </a>
        </li>
    )
}

/** 게시일 지표: "QQQ +1.20%" — 상승 빨강 / 하락 파랑 */
function ChipText({ chip }: { chip: IndicatorChip }) {
    const up = chip.changeRate > 0
    const down = chip.changeRate < 0
    return (
        <>
            <span aria-hidden>·</span>
            <span title="기사 게시일 지표 등락">
                {chip.label}{' '}
                <span className={cn('tabular-nums', up ? 'text-up' : down ? 'text-down' : '')}>
                    {up ? '+' : down ? '−' : ''}
                    {Math.abs(chip.changeRate).toFixed(2)}%
                </span>
            </span>
        </>
    )
}

/**
 * 제목 안 키워드 첫 등장 위치를 <mark>로 강조.
 * 영문 키워드(AI 등)는 단어 경계를 요구해 "Airbnb" 같은 오탐을 막는다.
 */
function Highlight({ text, keyword }: { text: string; keyword: string | null }) {
    if (!keyword) return <>{text}</>
    const escaped = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    const pattern = /^[\x00-\x7F]+$/.test(keyword)
        ? new RegExp(`(?<![A-Za-z0-9])${escaped}(?![A-Za-z0-9])`, 'i')
        : new RegExp(escaped, 'i')
    const match = pattern.exec(text)
    if (!match) return <>{text}</>
    const start = match.index
    const end = start + match[0].length
    return (
        <>
            {text.slice(0, start)}
            <mark>{text.slice(start, end)}</mark>
            {text.slice(end)}
        </>
    )
}

function NewsSkeleton() {
    return (
        <ul className="divide-y divide-border" aria-busy="true">
            {Array.from({ length: MAX_ITEMS }).map((_, i) => (
                <li key={i} className="h-[122px] py-3 flex flex-col gap-2">
                    <span className="h-4 w-11/12 bg-panel-elevated rounded animate-pulse" />
                    <span className="h-4 w-2/3 bg-panel-elevated rounded animate-pulse" />
                    <span className="mt-auto h-3 w-16 bg-panel-elevated rounded animate-pulse" />
                </li>
            ))}
        </ul>
    )
}

/* ─────────────────────────── utils ─────────────────────────── */

function todayLabel(): string {
    return new Intl.DateTimeFormat('ko-KR', {
        timeZone: 'Asia/Seoul',
        month: 'long',
        day: 'numeric',
        weekday: 'short',
    }).format(new Date())
}

/** 7일 이내는 상대 시각, 그 이상은 날짜(M/d). 파싱 실패 시 빈 문자열. */
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

function hostname(url: string): string {
    try {
        return new URL(url).hostname.replace(/^www\./, '')
    } catch {
        return ''
    }
}

function formatNumber(n: number) {
    return n.toLocaleString('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
