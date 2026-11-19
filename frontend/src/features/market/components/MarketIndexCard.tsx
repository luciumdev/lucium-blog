import type {IndexSeries} from "@/features/market/types.ts";
import { Card } from '@/shared/components'
import { Sparkline } from './Sparkline'

const SPARK_DEPTH = 5

const CODE_LABEL: Record<string, string> = {
    KOSPI: 'KOSPI',
    KOSDAQ: 'KOSDAQ',
    NASDAQ_PROXY: 'NASDAQ',
    SP500_PROXY: 'S&P 500'
}

export function MarketIndexCard({ series }: { series: IndexSeries }) {
    // 시리즈 자체가 비어있을 때 (KIS 호출 실패 또는 부분 실패)
    if (!series.latest) {
        return (
            <Card padding="md">
                <div className="flex flex-col gap-1">
                    <span className="font-mono text-xs text-ink-muted uppercase tracking-[0.15em] font-semibold">
                        {CODE_LABEL[series.code] ?? series.code}
                    </span>
                    <span className="font-mono text-sm text-ink-faint">— 데이터 없음</span>
                </div>
            </Card>
        )
    }

    const { close, change, changeRate } = series.latest
    const isUp = change > 0
    const isDown = change < 0
    const colorCls = isUp ? 'text-success' : isDown ? 'text-danger' : 'text-ink-faint'
    const sign = isUp ? '+' : ''   // 음수는 자체 부호 가져옴

    // bars는 최신순 → 시각적으로 left=과거 되도록 reverse
    const sparkCloses = [...series.bars]
        .slice(0, SPARK_DEPTH)
        .reverse()
        .map((b) => b.close)

    return (
        <Card padding="md">
            <div className="flex flex-col gap-1.5">
                <header className="flex items-center justify-between gap-2">
                    <span className="font-mono text-xs text-ink-muted uppercase tracking-[0.15em] font-semibold">
                        {CODE_LABEL[series.code] ?? series.code}
                    </span>
                    <span className="font-mono text-[10px] text-ink-faint truncate">
                        {series.displayName}
                    </span>
                </header>

                <div className="font-mono text-2xl text-ink tabular-nums font-semibold">
                    {close.toLocaleString('ko-KR', {
                        minimumFractionDigits: 2,
                        maximumFractionDigits: 2,
                    })}
                </div>

                <div className={`font-mono text-xs ${colorCls} tabular-nums`}>
                    {sign}
                    {change.toLocaleString('ko-KR', {
                        minimumFractionDigits: 2,
                        maximumFractionDigits: 2,
                    })}
                    {' · '}
                    {sign}
                    {changeRate.toFixed(2)}%
                </div>

                <Sparkline closes={sparkCloses} />
            </div>
        </Card>
    )
}