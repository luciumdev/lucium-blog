import { useRef, type CSSProperties, type ReactNode } from 'react'
import { useSearchParams } from 'react-router-dom'
import { cn } from '@/shared/lib/cn'
import { useCollectionDailyQuery, useCollectionRunsQuery } from '@/features/collection/hooks/useCollectionHistory'
import { NO_RUN_CLASS, RUN_COLOR, RUN_LABEL, RUN_STATUSES, dayStatus } from '@/features/collection/runStatus'
import { RunLegend } from '@/features/collection/RunLegend'
import type {
    CollectionJobCode,
    CollectionJobDaily,
    CollectionRunItem,
    RunStatus,
} from '@/features/collection/types'

/**
 * 실행 이력 (/status)
 *  - 위: 작업별 최근 30일 — 하루 한 칸(대표 상태), 성공률(EMPTY 제외), 평균 소요
 *  - 아래: 실행 로그 — 날짜별로 묶은 한 줄 목록, 작업·상태 필터, 페이지당 50건
 *    (필터·페이지는 URL 에 남겨 공유·뒤로가기 가능)
 */

const DAYS = 30
const PAGE_SIZE = 50

const JOB_OPTIONS: { value: CollectionJobCode; label: string }[] = [
    { value: 'NEWS', label: '뉴스' },
    { value: 'FOREX', label: '환율' },
    { value: 'INDICES', label: '지수' },
    { value: 'AI_JOURNAL', label: 'AI 릴리즈 노트' },
]

/** 상태 글자색 — 실패·일부 실패만 눈에 띄게, 나머지는 차분하게 */
const STATUS_TEXT: Record<RunStatus, string> = {
    SUCCESS: 'text-ink-muted',
    PARTIAL: 'text-[#8A5A00]',
    EMPTY: 'text-ink-faint',
    FAILED: 'text-danger',
}

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']

export default function StatusPage() {
    return (
        <div className="flex flex-col gap-4">
            <header className="fx px-1 pt-1">
                <h1 className="text-2xl font-bold tracking-tight">실행 이력</h1>
                <p className="mt-1 text-[15px] text-ink-muted [text-wrap:pretty]">
                    스케줄러가 외부 API에서 데이터를 모으는 작업의 실행 기록입니다. 매 실행의 결과·건수·소요 시간을 DB에 남기고, 최근 90일을 보관합니다.
                </p>
            </header>

            <DailySection />
            <RunLogSection />
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

function Skeleton({ className }: { className?: string }) {
    return <span className={cn('block bg-panel-elevated rounded animate-pulse', className)} />
}

/* ─────────────────────────── 작업별 30일 ─────────────────────────── */

function DailySection() {
    const { data, isPending, isError } = useCollectionDailyQuery(DAYS)

    return (
        <Card delay={0.08} className="px-5 sm:px-6 pt-5 pb-4">
            <div className="sec-h flex flex-wrap items-baseline justify-between gap-2 pb-2.5 border-b border-border">
                <h2 className="text-[17px] font-bold">작업별 최근 {DAYS}일</h2>
                <RunLegend />
            </div>

            {isPending ? (
                <div className="mt-4 grid gap-5 md:grid-cols-2" aria-busy="true">
                    {Array.from({ length: 4 }).map((_, i) => (
                        <Skeleton key={i} className="h-16 w-full" />
                    ))}
                </div>
            ) : isError || !data ? (
                <p className="mt-4 text-[15px] text-ink-faint">요약을 불러오지 못했습니다.</p>
            ) : (
                <ul className="mt-1 grid gap-x-8 md:grid-cols-2">
                    {data.jobs.map((job) => (
                        <JobDailyRow key={job.job} job={job} />
                    ))}
                </ul>
            )}
        </Card>
    )
}

function JobDailyRow({ job }: { job: CollectionJobDaily }) {
    return (
        <li className="py-3.5 border-b border-border last:border-b-0">
            <div className="flex items-baseline justify-between gap-3">
                <p className="text-[15px] font-semibold">
                    {job.label}
                    <span className="ml-1.5 text-xs font-normal text-ink-faint">{job.cycle}</span>
                </p>
                <p className="text-sm tabular-nums">
                    {job.successRate != null ? (
                        <>
                            <b className="font-semibold">{job.successRate}%</b>
                            <span className="ml-1 text-xs text-ink-faint">성공률</span>
                        </>
                    ) : (
                        <span className="text-xs text-ink-faint">집계 없음</span>
                    )}
                </p>
            </div>

            <div className="mt-2 flex gap-[3px]" role="img" aria-label={`${job.label} 최근 ${job.daily.length}일 실행 상태`}>
                {job.daily.map((d) => {
                    const st = dayStatus(d)
                    const tip = st
                        ? `${formatDay(d.date)} · 성공 ${d.success} · 일부 실패 ${d.partial} · 데이터 없음 ${d.empty} · 실패 ${d.failed}`
                        : `${formatDay(d.date)} · 실행 없음`
                    return (
                        <span
                            key={d.date}
                            title={tip}
                            className={cn(
                                'flex-1 h-6 rounded-[2px]',
                                st ? RUN_COLOR[st] : NO_RUN_CLASS,
                            )}
                        />
                    )
                })}
            </div>

            <div className="mt-1.5 flex justify-between text-[11px] text-ink-faint tabular-nums">
                <span>{job.daily.length > 0 ? formatDay(job.daily[0].date) : ''}</span>
                <span>
                    실행 {job.totalRuns.toLocaleString()}회
                    {job.avgDurationMs != null && ` · 평균 ${formatDuration(job.avgDurationMs)}`}
                </span>
                <span>오늘</span>
            </div>
        </li>
    )
}

/* ─────────────────────────── 실행 로그 ─────────────────────────── */

function RunLogSection() {
    const [params, setParams] = useSearchParams()
    const job = toJob(params.get('job'))
    const status = toStatus(params.get('status'))
    const page = Math.max(0, Number(params.get('page') ?? 0) || 0)

    const { data, isPending, isError, isFetching } = useCollectionRunsQuery({ job, status, page, size: PAGE_SIZE })

    const topRef = useRef<HTMLDivElement>(null)

    /**
     * 필터를 바꾸면 첫 페이지로.
     * → 작업·상태 필터는 보던 위치 그대로, 페이지 이동만 실행 로그 카드 머리로 이동
     */
    const update = (next: { job?: CollectionJobCode | null; status?: RunStatus | null; page?: number }) => {
        const p = new URLSearchParams(params)
        if (next.job !== undefined) {
            if (next.job) p.set('job', next.job)
            else p.delete('job')
            p.delete('page')
        }
        if (next.status !== undefined) {
            if (next.status) p.set('status', next.status)
            else p.delete('status')
            p.delete('page')
        }
        if (next.page !== undefined) {
            if (next.page > 0) p.set('page', String(next.page))
            else p.delete('page')
        }
        setParams(p, { replace: true, preventScrollReset: true })
        if (next.page !== undefined) {
            topRef.current?.scrollIntoView({ block: 'start' })
        }
    }

    return (
        // scroll-mt: 고정 헤더(56px)에 가리지 않게
        <div ref={topRef} className="scroll-mt-[68px]">
        <Card delay={0.16} className="px-5 sm:px-6 pt-5 pb-3">
            <div className="sec-h flex flex-wrap items-center justify-between gap-3 pb-2.5 border-b border-border">
                <h2 className="text-[17px] font-bold">실행 로그</h2>
                <div className="flex flex-wrap items-center gap-2">
                    <label htmlFor="run-job" className="sr-only">작업</label>
                    <select
                        id="run-job"
                        value={job ?? ''}
                        onChange={(e) => update({ job: toJob(e.target.value) ?? null })}
                        className="h-8 pl-2.5 pr-7 rounded-md border border-border bg-panel text-sm text-ink"
                    >
                        <option value="">전체 작업</option>
                        {JOB_OPTIONS.map((o) => (
                            <option key={o.value} value={o.value}>{o.label}</option>
                        ))}
                    </select>

                    <div role="radiogroup" aria-label="상태" className="inline-flex p-0.5 rounded-lg bg-surface-muted">
                        <StatusChip active={!status} onClick={() => update({ status: null })}>전체</StatusChip>
                        {RUN_STATUSES.map((s) => (
                            <StatusChip key={s} active={status === s} onClick={() => update({ status: s })}>
                                {RUN_LABEL[s]}
                            </StatusChip>
                        ))}
                    </div>
                </div>
            </div>

            {isPending ? (
                <div className="mt-3 space-y-2" aria-busy="true">
                    {Array.from({ length: 8 }).map((_, i) => (
                        <Skeleton key={i} className="h-7 w-full" />
                    ))}
                </div>
            ) : isError || !data ? (
                <p className="py-6 text-[15px] text-ink-faint">실행 로그를 불러오지 못했습니다.</p>
            ) : data.items.length === 0 ? (
                <p className="py-6 text-[15px] text-ink-faint">조건에 맞는 실행 기록이 없습니다.</p>
            ) : (
                <>
                    <div className={cn('transition-opacity', isFetching && 'opacity-60')}>
                        {groupByDay(data.items).map((group) => (
                            <DayGroup key={group.date} date={group.date} runs={group.runs} />
                        ))}
                    </div>

                    <Pager
                        page={data.page}
                        totalPages={data.totalPages}
                        total={data.totalElements}
                        onChange={(p) => update({ page: p })}
                    />
                </>
            )}
        </Card>
        </div>
    )
}

function StatusChip({ active, onClick, children }: { active: boolean; onClick: () => void; children: ReactNode }) {
    return (
        <button
            type="button"
            role="radio"
            aria-checked={active}
            onClick={onClick}
            className={cn(
                'h-7 px-2.5 rounded-md text-[13px] transition-colors',
                active ? 'bg-panel text-ink font-semibold shadow-xs' : 'text-ink-muted hover:text-ink',
            )}
        >
            {children}
        </button>
    )
}

/** 날짜 머리줄 + 그날의 실행들 */
function DayGroup({ date, runs }: { date: string; runs: CollectionRunItem[] }) {
    const failed = runs.filter((r) => r.status === 'FAILED').length
    return (
        <section className="mt-3 first:mt-2">
            <h3 className="flex items-baseline justify-between gap-3 px-2 py-1.5 rounded-md bg-surface-muted text-[13px]">
                <span className="font-semibold text-ink">{formatDayHeader(date)}</span>
                <span className="text-ink-faint tabular-nums">
                    {runs.length}건
                    {failed > 0 && <span className="ml-1.5 text-danger">실패 {failed}</span>}
                </span>
            </h3>
            <ul>
                {runs.map((run) => (
                    <RunRow key={run.id} run={run} />
                ))}
            </ul>
        </section>
    )
}

/**
 * 한 줄: 시각 · 상태 · 작업 · 사유 · (건수 · 소요)
 * sm 미만에서는 사유가 아랫줄로 내려감 — 가로 스크롤 없음
 */
function RunRow({ run }: { run: CollectionRunItem }) {
    return (
        <li className="px-2 py-2 border-b border-border last:border-b-0 text-sm">
            <div className="flex flex-wrap sm:flex-nowrap items-baseline gap-x-3 gap-y-0.5">
                <span className="w-[60px] shrink-0 tabular-nums text-ink-faint">{formatTime(run.startedAt)}</span>

                <span className={cn('sm:w-[84px] shrink-0 inline-flex items-center gap-1.5 whitespace-nowrap', STATUS_TEXT[run.status])}>
                    <span aria-hidden className={cn('w-2 h-2 rounded-[2px]', RUN_COLOR[run.status])} />
                    {RUN_LABEL[run.status]}
                </span>

                <span className="sm:w-[120px] shrink-0 whitespace-nowrap font-medium text-ink">
                    {run.jobLabel}
                    {run.trigger === 'BOOTSTRAP' && (
                        <span title="서버 기동 직후 실행" className="ml-1.5 text-[11px] font-normal text-ink-faint">
                            기동
                        </span>
                    )}
                </span>

                <span
                    title={run.reason ?? undefined}
                    className={cn(
                        'order-last sm:order-none basis-full sm:basis-auto flex-1 min-w-0 sm:truncate pl-[72px] sm:pl-0',
                        run.status === 'FAILED' ? 'text-ink' : 'text-ink-muted',
                    )}
                >
                    {run.reason ?? ''}
                </span>

                <span className="ml-auto shrink-0 whitespace-nowrap tabular-nums text-[13px] text-ink-faint">
                    {run.itemCount > 0 && <span className="text-ink-muted">{run.itemCount.toLocaleString()}건 · </span>}
                    {run.durationMs > 0 ? formatDuration(run.durationMs) : '—'}
                </span>
            </div>

            {run.rawMessage && (
                <details className="mt-1 sm:pl-[300px]">
                    <summary className="cursor-pointer select-none text-xs text-ink-faint">원문 (관리자)</summary>
                    <pre className="mt-1 whitespace-pre-wrap break-all rounded bg-surface-muted p-2 text-xs text-ink-muted">
                        {run.rawMessage}
                    </pre>
                </details>
            )}
        </li>
    )
}

function Pager({ page, totalPages, total, onChange }: {
    page: number
    totalPages: number
    total: number
    onChange: (page: number) => void
}) {
    const btn = 'h-8 px-3 rounded-md border border-border text-sm text-ink-muted hover:bg-surface-muted hover:text-ink disabled:opacity-40 disabled:pointer-events-none transition-colors'
    return (
        <div className="mt-2 pt-3 border-t border-border flex items-center justify-between gap-3 text-[13px] text-ink-faint tabular-nums">
            <span>총 {total.toLocaleString()}건</span>
            <div className="flex items-center gap-2">
                <button type="button" className={btn} disabled={page <= 0} onClick={() => onChange(page - 1)}>
                    이전
                </button>
                <span>
                    {totalPages === 0 ? 0 : page + 1} / {totalPages}
                </span>
                <button type="button" className={btn} disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
                    다음
                </button>
            </div>
        </div>
    )
}

/* ─────────────────────────── utils ─────────────────────────── */

function toJob(v: string | null): CollectionJobCode | undefined {
    return JOB_OPTIONS.some((o) => o.value === v) ? (v as CollectionJobCode) : undefined
}

function toStatus(v: string | null): RunStatus | undefined {
    return RUN_STATUSES.includes(v as RunStatus) ? (v as RunStatus) : undefined
}

/** 최신순 목록을 순서 그대로 날짜별로 묶음 */
function groupByDay(items: CollectionRunItem[]): { date: string; runs: CollectionRunItem[] }[] {
    const groups: { date: string; runs: CollectionRunItem[] }[] = []
    for (const run of items) {
        const date = run.startedAt.split('T')[0]
        const last = groups[groups.length - 1]
        if (last && last.date === date) last.runs.push(run)
        else groups.push({ date, runs: [run] })
    }
    return groups
}

/** "2026-10-09" → "10/9" */
function formatDay(date: string) {
    const [, m, d] = date.split('-')
    return `${Number(m)}/${Number(d)}`
}

/** "2026-10-18" → "10월 18일 (일)" — 문자열 날짜 기준, 시간대 변환 없음 */
function formatDayHeader(date: string) {
    const [y, m, d] = date.split('-').map(Number)
    const weekday = WEEKDAYS[new Date(y, m - 1, d).getDay()]
    return `${m}월 ${d}일 (${weekday})`
}

/** "2026-10-09T09:00:12.345" → "09:00:12" (서버 기록 시각 그대로) */
function formatTime(iso: string) {
    return (iso.split('T')[1] ?? '').slice(0, 8)
}

function formatDuration(ms: number) {
    if (ms < 1000) return `${ms}ms`
    if (ms < 60_000) return `${(ms / 1000).toFixed(1)}s`
    const m = Math.floor(ms / 60_000)
    const s = Math.round((ms % 60_000) / 1000)
    return `${m}분 ${s}초`
}
