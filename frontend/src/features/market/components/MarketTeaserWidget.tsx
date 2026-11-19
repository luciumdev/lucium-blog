import { Link } from 'react-router-dom'
import { useForexQuery } from '../hooks/useForexQuery'
import type { ForexRate } from '../types'

/** 사이드바에 좁아서 4개만 노출. 순서 고정. */
const VISIBLE_CURRENCIES = ['USD', 'EUR', 'JPY(100)', 'CNH']

export function MarketTeaserWidget() {
    const { data, isPending, isError } = useForexQuery()

    // 위젯 실패해도 사이드바는 살아있어야 함
    if (isError) return null

    return (
        <div className="mb-5">
            <p className="px-2 mb-1.5 text-[10px] font-mono font-semibold uppercase tracking-[0.2em] text-ink-muted">
                market
            </p>
            <div className="flex flex-col gap-0.5">
                {isPending && <ForexSkeleton />}
                {data && data.rates.length === 0 && (
                    <p className="px-2 py-1 text-xs text-ink-faint font-mono">
                        — 데이터 없음
                    </p>
                )}
                {data && data.rates.length > 0 && <ForexLines rates={data.rates} />}

                <Link
                    to="/market"
                    className="
                        mt-1 flex items-center gap-2 px-2 py-1.5 rounded-md
                        font-mono text-xs text-ink-muted
                        hover:bg-panel/50 hover:text-primary
                        transition-colors group
                    "
                >
                    <span className="text-ink-faint group-hover:text-primary">→</span>
                    <span>view all</span>
                </Link>
            </div>
        </div>
    )
}

function ForexLines({ rates }: { rates: ForexRate[] }) {
    const visible = VISIBLE_CURRENCIES.map((code) =>
        rates.find((r) => r.currencyCode === code),
    ).filter((r): r is ForexRate => r !== undefined)

    if (visible.length === 0) {
        return (
            <p className="px-2 py-1 text-xs text-ink-faint font-mono">
                — 통화 정보 없음
            </p>
        )
    }

    return (
        <ul
            className="
                flex flex-col
                rounded-md border border-border
                divide-y divide-border/60
                overflow-hidden
                bg-surface/40
            "
        >
            {visible.map((rate) => (
                <li
                    key={rate.currencyCode}
                    className="
                        flex items-center justify-between
                        px-2.5 py-1.5
                        hover:bg-panel/50 transition-colors
                    "
                >
                    <span className="font-mono text-[11px] text-ink-muted">
                        {formatLabel(rate.currencyCode)}
                    </span>
                    <span className="font-mono text-xs text-ink tabular-nums">
                        {rate.rate.toLocaleString('ko-KR', {
                            minimumFractionDigits: 2,
                            maximumFractionDigits: 2,
                        })}
                    </span>
                </li>
            ))}
        </ul>
    )
}

/** "JPY(100)" → "JPY100" — 좁은 사이드바에 맞게 괄호 제거. */
function formatLabel(code: string): string {
    return code.replace(/[()]/g, '')
}

function ForexSkeleton() {
    return (
        <ul className="flex flex-col gap-0.5" aria-busy="true">
            {Array.from({ length: 4 }).map((_, i) => (
                <li key={i}>
                    <div className="flex items-center justify-between px-2 py-1">
                        <div className="h-3 w-10 bg-panel rounded animate-pulse" />
                        <div className="h-3 w-16 bg-panel rounded animate-pulse" />
                    </div>
                </li>
            ))}
        </ul>
    )
}