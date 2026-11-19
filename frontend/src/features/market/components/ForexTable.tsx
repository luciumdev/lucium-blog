import { Card } from '@/shared/components'
import { useForexQuery } from '../hooks/useForexQuery'
import type { ForexRate } from '../types'

export function ForexTable() {
    const { data, isPending, isError } = useForexQuery()

    return (
        <Card padding="none" className="h-full flex flex-col">
            <header className="px-4 py-3 border-b border-border flex items-center justify-between">
                <h2 className="font-mono text-xs uppercase tracking-[0.2em] text-ink-muted font-semibold">
                    forex
                </h2>
                {data && (
                    <span className="font-mono text-[10px] text-ink-faint">
                        {formatTime(data.snapshotAt)}
                    </span>
                )}
            </header>

            {isPending && <ForexTableSkeleton />}

            {isError && (
                <p className="px-4 py-6 text-sm text-danger font-mono">
                    환율을 불러오지 못했습니다.
                </p>
            )}

            {data && data.rates.length === 0 && (
                <p className="px-4 py-6 text-sm text-ink-faint font-mono">
                    — 데이터 없음
                </p>
            )}

            {data && data.rates.length > 0 && (
                <ul className="flex-1 flex flex-col divide-y divide-border">
                    {data.rates.map((rate) => (
                        <ForexRow key={rate.currencyCode} rate={rate} />
                    ))}
                </ul>
            )}
        </Card>
    )
}

function ForexRow({ rate }: { rate: ForexRate }) {
    const hasChange = rate.change != null && rate.changeRate != null
    const isUp = hasChange && rate.change! > 0
    const isDown = hasChange && rate.change! < 0
    const colorCls = !hasChange
        ? 'text-ink-faint'
        : isUp
            ? 'text-success'
            : isDown
                ? 'text-danger'
                : 'text-ink-faint'
    const sign = isUp ? '+' : ''   // 음수는 자체 부호 가져옴

    return (
        <li className="flex-1 px-4 py-3 flex items-center justify-between hover:bg-panel-elevated transition-colors">
            <div className="flex flex-col">
                <span className="font-mono text-sm text-ink font-semibold">
                    {rate.currencyCode.replace(/[()]/g, '')}
                </span>
                <span className="text-[11px] text-ink-faint">{rate.currencyName}</span>
            </div>
            <div className="flex flex-col items-end gap-0.5">
                <span className="font-mono text-base text-ink-muted tabular-nums">
                    {rate.rate.toLocaleString('ko-KR', {
                        minimumFractionDigits: 2,
                        maximumFractionDigits: 2,
                    })}
                </span>
                {hasChange && (
                    <span className={`font-mono text-[11px] ${colorCls} tabular-nums`}>
                        {sign}
                        {rate.change!.toFixed(2)}
                        {' · '}
                        {sign}
                        {rate.changeRate!.toFixed(2)}%
                    </span>
                )}
            </div>
        </li>
    )
}

function formatTime(iso: string): string {
    try {
        return new Date(iso).toLocaleTimeString('ko-KR', {
            hour: '2-digit',
            minute: '2-digit',
        })
    } catch {
        return ''
    }
}

function ForexTableSkeleton() {
    return (
        <ul className="divide-y divide-border" aria-busy="true">
            {Array.from({ length: 4 }).map((_, i) => (
                <li key={i} className="px-4 py-3 flex items-center justify-between">
                    <div className="flex flex-col gap-1">
                        <div className="h-4 w-12 bg-surface rounded animate-pulse" />
                        <div className="h-3 w-20 bg-surface rounded animate-pulse" />
                    </div>
                    <div className="h-5 w-24 bg-surface rounded animate-pulse" />
                </li>
            ))}
        </ul>
    )
}