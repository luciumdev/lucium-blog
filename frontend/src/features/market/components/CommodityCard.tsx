import type { CommodityPrice } from '@/features/market/types.ts'
import { Card } from '@/shared/components'

const CURRENCY_SYMBOL: Record<string, string> = {
    USD: '$',
    KRW: '₩',
    EUR: '€',
    JPY: '¥',
}

export function CommodityCard({ price }: { price: CommodityPrice }) {
    const isUp = price.changeValue > 0
    const isDown = price.changeValue < 0
    const colorCls = isUp ? 'text-success' : isDown ? 'text-danger' : 'text-ink-faint'
    const sign = isUp ? '+' : ''  // 음수는 자체 부호
    const arrow = isUp ? '↑' : isDown ? '↓' : '→'
    const symbol = CURRENCY_SYMBOL[price.currency] ?? ''

    return (
        <Card padding="md">
        <div className="flex flex-col gap-1.5">
        <header className="flex items-center justify-between gap-2">
        <span className="font-mono text-xs text-ink-muted uppercase tracking-[0.15em] font-semibold">
            {price.code}
            </span>
            <span className="font-mono text-[10px] text-ink-faint truncate">
        {price.name} · /{price.unit}
    </span>
    </header>

    <div className="font-mono text-2xl text-ink tabular-nums font-semibold">
        {symbol}
    {price.price.toLocaleString('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    })}
    </div>

    <div className={`font-mono text-xs ${colorCls} tabular-nums flex items-center gap-1`}>
    <span aria-hidden="true">{arrow}</span>
        <span>
        {sign}
    {price.changeValue.toLocaleString('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    })}
    {' · '}
    {sign}
    {price.changePercent.toFixed(2)}%
    </span>
    </div>
    </div>
    </Card>
)
}
