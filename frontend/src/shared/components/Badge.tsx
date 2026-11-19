import { type HTMLAttributes } from 'react'
import { cn } from '@/shared/lib/cn'

export type BadgeVariant =
    | 'default'
    | 'primary'
    | 'accent'
    | 'success'
    | 'warning'
    | 'danger'

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
    variant?: BadgeVariant
}

/**
 * 카테고리/태그/상태 표시용 작은 라벨.
 *
 * 컬러는 의미 매핑 — 사람이 색만 보고 의미 추정 가능해야 함:
 *  - primary: 카테고리 (주요 분류)
 *  - accent: 태그 (보조 분류, 강조)
 *  - success/warning/danger: 상태 (PUBLISHED, DRAFT, 삭제됨 등)
 */
const variantStyles: Record<BadgeVariant, string> = {
    default: 'bg-surface-muted text-ink-muted border-border',
    primary: 'bg-primary-tint text-primary-hover border-primary-mid',
    accent: 'bg-accent-tint text-accent-hover border-accent',
    success: 'bg-green-50 text-green-700 border-green-200',
    warning: 'bg-yellow-50 text-yellow-700 border-yellow-200',
    danger: 'bg-danger-tint text-danger border-red-200',
}

export function Badge({
                          variant = 'default',
                          className,
                          ...rest
                      }: BadgeProps) {
    return (
        <span
            className={cn(
                'inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-xs font-medium border',
                variantStyles[variant],
                className,
            )}
            {...rest}
        />
    )
}