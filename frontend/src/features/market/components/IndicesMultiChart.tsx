import { useMemo } from 'react'
import {
    CartesianGrid,
    Line,
    LineChart,
    ReferenceLine,
    ResponsiveContainer,
    Tooltip,
    XAxis,
    YAxis,
} from 'recharts'
import type { IndexCode } from '@/features/market/types.ts'
import { Card } from '@/shared/components'
import { useIndicesQuery } from '../hooks/useIndicesQuery'
import { useIndicesHistoryQuery } from '../hooks/useIndicesHistoryQuery'
import {
    fromBars,
    fromSnapshot,
    rebase100,
    type IndexTimeSeries,
} from '../lib/indexTimeSeries'

/** 지수 정의 순서 — 범례/라인 순서를 카드와 일치시키기 위한 기준. */
const CODE_ORDER: IndexCode[] = ['KOSPI', 'KOSDAQ', 'NASDAQ_PROXY', 'SP500_PROXY']

const CODE_COLOR: Record<IndexCode, string> = {
    KOSPI: '#224997',        // deep indigo (accent 계열)
    KOSDAQ: '#0d9488',       // teal
    NASDAQ_PROXY: '#d97706',  // amber
    SP500_PROXY: '#be123c',   // rose
}

const CODE_LABEL: Record<IndexCode, string> = {
    KOSPI: 'KOSPI',
    KOSDAQ: 'KOSDAQ',
    NASDAQ_PROXY: 'NASDAQ',
    SP500_PROXY: 'S&P 500',
}

// 히스토리가 이 미만이면 빈약 → bars 폴백.
const MIN_HISTORY_POINTS = 3

interface Props {
    /** 조회 기간(일). 기본 30. */
    days?: number
}

/** recharts wide-format 한 행: { date, KOSPI?, KOSDAQ?, ... } */
type ChartRow = { date: string } & Partial<Record<IndexCode, number>>

/**
 * rebase100 결과(지수별 long-format)를 recharts 용 wide-format 으로 pivot.
 * 날짜를 키로 모아 한 행에 여러 지수 값을 담는다.
 */
function toChartRows(series: IndexTimeSeries[]): { rows: ChartRow[]; codes: IndexCode[] } {
    const byDate = new Map<string, ChartRow>()
    const present = new Set<IndexCode>()

    for (const s of series) {
        present.add(s.code)
        for (const p of s.points) {
            const row = byDate.get(p.date) ?? { date: p.date }
            row[s.code] = p.close
            byDate.set(p.date, row)
        }
    }

    const rows = [...byDate.values()].sort((a, b) => a.date.localeCompare(b.date))
    // 라인 렌더 순서를 정의 순서로 고정
    const codes = CODE_ORDER.filter((c) => present.has(c))
    return { rows, codes }
}

/**
 * 여러 지수를 기준일=100 리베이싱해 한 차트에 겹쳐 그린다 (recharts).
 *
 * 데이터 소스: index_bars 히스토리 API 우선, 얕으면 KIS 5일 bars 폴백.
 * 두 소스 모두 IndexTimeSeries 로 흡수 → rebase100 → wide-format pivot → LineChart.
 */
export function IndicesMultiChart({ days = 30 }: Props) {
    const historyQ = useIndicesHistoryQuery(days)
    const latestQ = useIndicesQuery()   // 폴백용 (캐시 공유 — 중복 요청 없음)

    const { rows, codes, sourceLabel, isPending } = useMemo(() => {
        const historySeries = historyQ.data ? fromSnapshot(historyQ.data) : []
        const historyDeep = historySeries.some(
            (s) => s.points.length >= MIN_HISTORY_POINTS,
        )

        const source: IndexTimeSeries[] = historyDeep
            ? historySeries
            : latestQ.data
                ? fromBars(latestQ.data.series)
                : []

        const rebased = rebase100(source)
        const { rows, codes } = toChartRows(rebased)

        return {
            rows,
            codes,
            sourceLabel: historyDeep ? `최근 ${days}일` : '최근 5일',
            isPending:
                rows.length === 0 && (historyQ.isPending || latestQ.isPending),
        }
    }, [historyQ.data, historyQ.isPending, latestQ.data, latestQ.isPending, days])

    if (isPending) return <ChartSkeleton />
    if (rows.length === 0) return null

    return (
        <Card padding="md">
            <div className="flex flex-col gap-3">
                <header className="flex items-center justify-between gap-2">
                    <span className="font-mono text-xs text-ink-muted uppercase tracking-[0.15em] font-semibold">
                        지수 비교
                    </span>
                    <span className="font-mono text-[10px] text-ink-faint">
                        기준일=100 · {sourceLabel}
                    </span>
                </header>

                <div className="h-72 w-full">
                    <ResponsiveContainer width="100%" height="100%">
                        <LineChart
                            data={rows}
                            margin={{ top: 8, right: 12, bottom: 4, left: -12 }}
                        >
                            <CartesianGrid
                                strokeDasharray="3 3"
                                stroke="var(--color-border)"
                                vertical={false}
                            />
                            <XAxis
                                dataKey="date"
                                tickFormatter={formatDateTick}
                                tick={{ fontSize: 10, fill: 'var(--color-ink-faint)' }}
                                stroke="var(--color-border)"
                                minTickGap={24}
                            />
                            <YAxis
                                domain={['dataMin - 2', 'dataMax + 2']}
                                tickFormatter={(v: number) => v.toFixed(0)}
                                tick={{ fontSize: 10, fill: 'var(--color-ink-faint)' }}
                                stroke="var(--color-border)"
                                width={44}
                                allowDecimals={false}
                                tickCount={6}
                            />
                            <ReferenceLine
                                y={100}
                                stroke="var(--color-ink-faint)"
                                strokeDasharray="4 4"
                            />
                            <Tooltip
                                content={<IndicesTooltip />}
                                isAnimationActive={false}
                            />
                            {codes.map((code) => (
                                <Line
                                    key={code}
                                    type="monotone"
                                    dataKey={code}
                                    name={CODE_LABEL[code]}
                                    stroke={CODE_COLOR[code]}
                                    strokeWidth={2}
                                    dot={false}
                                    activeDot={{ r: 4 }}
                                    isAnimationActive={false}
                                    connectNulls
                                />
                            ))}
                        </LineChart>
                    </ResponsiveContainer>
                </div>

                {/* 범례 — 정의 순서 + 기준일 대비 ± (마지막 값 기준) */}
                <div className="flex flex-wrap gap-x-4 gap-y-1.5">
                    {codes.map((code) => {
                        const lastRow = [...rows].reverse().find((r) => r[code] != null)
                        const last = lastRow?.[code] ?? 100
                        const diff = last - 100
                        const diffCls =
                            diff > 0
                                ? 'text-success'
                                : diff < 0
                                    ? 'text-danger'
                                    : 'text-ink-faint'
                        return (
                            <div
                                key={code}
                                className="flex items-center gap-1.5 font-mono text-[11px]"
                            >
                                <span
                                    className="inline-block w-3 h-0.5 rounded"
                                    style={{ backgroundColor: CODE_COLOR[code] }}
                                    aria-hidden
                                />
                                <span className="text-ink-muted">{CODE_LABEL[code]}</span>
                                <span className={`${diffCls} tabular-nums`}>
                                    {diff > 0 ? '+' : ''}
                                    {diff.toFixed(2)}
                                </span>
                            </div>
                        )
                    })}
                </div>
            </div>
        </Card>
    )
}

/** X축 틱: "YYYY-MM-DD" → "M/D" */
function formatDateTick(value: string): string {
    const parts = value.split('-')
    if (parts.length !== 3) return value
    return `${Number(parts[1])}/${Number(parts[2])}`
}

/** 커스텀 툴팁 — 날짜 + 지수별 리베이싱 값. */
interface TooltipPayloadEntry {
    dataKey: IndexCode
    value: number
    color: string
}
function IndicesTooltip({
                            active,
                            payload,
                            label,
                        }: {
    active?: boolean
    payload?: TooltipPayloadEntry[]
    label?: string
}) {
    if (!active || !payload || payload.length === 0) return null
    return (
        <div className="rounded-md border border-border bg-panel px-2.5 py-2 shadow-sm">
            <div className="font-mono text-[10px] text-ink-faint mb-1">
                {label ? formatDateTick(label) : ''}
            </div>
            <div className="flex flex-col gap-0.5">
                {payload.map((entry) => (
                    <div
                        key={entry.dataKey}
                        className="flex items-center justify-between gap-3 font-mono text-[11px]"
                    >
                        <span className="flex items-center gap-1.5">
                            <span
                                className="inline-block w-2 h-2 rounded-full"
                                style={{ backgroundColor: entry.color }}
                                aria-hidden
                            />
                            <span className="text-ink-muted">
                                {CODE_LABEL[entry.dataKey]}
                            </span>
                        </span>
                        <span className="text-ink tabular-nums">
                            {entry.value.toFixed(2)}
                        </span>
                    </div>
                ))}
            </div>
        </div>
    )
}

function ChartSkeleton() {
    return (
        <Card padding="md">
            <div className="flex flex-col gap-3" aria-busy="true">
                <div className="h-3 w-16 bg-panel-elevated rounded animate-pulse" />
                <div className="h-72 w-full bg-panel-elevated rounded animate-pulse" />
            </div>
        </Card>
    )
}
