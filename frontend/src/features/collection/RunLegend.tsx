import { cn } from '@/shared/lib/cn'
import { RUN_LEGEND } from './runStatus.ts'

/**
 * 실행 상태 범례 — 메인 카드·/status 페이지 공통.
 * 색 네모는 막대와 같은 클래스(RUN_COLOR · NO_RUN_CLASS)
 */
export function RunLegend({ className }: { className?: string }) {
    return (
        <p className={cn('flex flex-wrap gap-x-2.5 gap-y-0.5 text-[11px] text-ink-faint', className)}>
            {RUN_LEGEND.map((item) => (
                <span key={item.key} className="inline-flex items-center gap-1 whitespace-nowrap">
                    <span aria-hidden className={cn('w-2 h-2 rounded-[1px]', item.className)} />
                    {item.label}
                </span>
            ))}
        </p>
    )
}
