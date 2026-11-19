import { Card } from '@/shared/components'
import { useIndicesQuery } from '../hooks/useIndicesQuery'
import { MarketIndexCard } from './MarketIndexCard'

interface IndicesGridProps {
    /** true면 2x2 (좁은 컬럼용), false(기본)면 4x1 풀폭 */
    compact?: boolean
}

export function IndicesGrid({ compact = false }: IndicesGridProps) {
    const { data, isPending, isError } = useIndicesQuery()

    const gridCls = compact
        ? 'grid grid-cols-2 gap-3'
        : 'grid grid-cols-2 lg:grid-cols-4 gap-3'

    if (isError) return null

    if (isPending) {
        return (
            <div className={gridCls}>
                {Array.from({ length: 4 }).map((_, i) => (
                    <IndexCardSkeleton key={i} />
                ))}
            </div>
        )
    }

    if (!data || data.series.length === 0) return null

    return (
        <div className={gridCls}>
            {data.series.map((s) => (
                <MarketIndexCard key={s.code} series={s} />
            ))}
        </div>
    )
}

function IndexCardSkeleton() {
    return (
        <Card padding="md">
            <div className="flex flex-col gap-1.5" aria-busy="true">
                <div className="h-3 w-12 bg-panel-elevated rounded animate-pulse" />
                <div className="h-7 w-24 bg-panel-elevated rounded animate-pulse" />
                <div className="h-3 w-20 bg-panel-elevated rounded animate-pulse" />
                <div className="h-7 w-full bg-panel-elevated rounded animate-pulse" />
            </div>
        </Card>
    )
}