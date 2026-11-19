type Props = {
    closes: number[]
}

export function Sparkline({ closes }: Props) {
    if (closes.length < 2) return null

    const W = 120
    const H = 28
    const min = Math.min(...closes)
    const max = Math.max(...closes)
    const range = max - min || 1

    const points = closes
        .map((c, i) => {
            const x = (i / (closes.length - 1)) * W
            const y = H - ((c - min) / range) * H
            return `${x.toFixed(1)},${y.toFixed(1)}`
        })
        .join(' ')

    return (
        <svg
            viewBox={`0 0 ${W} ${H}`}
            preserveAspectRatio="none"
            className="w-full h-7 opacity-70"
            aria-hidden
        >
            <polyline
                points={points}
                fill="none"
                stroke="var(--color-ink-muted)"
                strokeWidth={2.5}   //줄굵기
                strokeLinecap="round"
                strokeLinejoin="round"
                vectorEffect="non-scaling-stroke"
            />
        </svg>
    )
}