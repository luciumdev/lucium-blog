import { forwardRef, type HTMLAttributes } from 'react'
import { cn } from '@/shared/lib/cn'

export type CardVariant = 'default' | 'interactive'
export type CardPadding = 'none' | 'sm' | 'md' | 'lg'

export interface CardProps extends HTMLAttributes<HTMLDivElement> {
    variant?: CardVariant
    padding?: CardPadding
}

/**
 * 기본 카드 컨테이너.
 *
 * variant=interactive: 호버 시 보더 강해지고 그림자 살짝.
 *   PostCard처럼 클릭 가능한 카드에 사용.
 *
 * padding=none: 안에 image-bleed 같은 자식이 있는 경우 직접 padding 제어.
 */
const paddingStyles: Record<CardPadding, string> = {
    none: '',
    sm: 'p-3',
    md: 'p-4',
    lg: 'p-6',
}
export const Card = forwardRef<HTMLDivElement, CardProps>(
    function Card(
        { variant = 'default', padding = 'md', className, children, ...rest },
        ref,
    ) {
        return (
            <div
                ref={ref}
                className={cn(
                    'bg-panel border border-border rounded-lg shadow-xs',
                    variant === 'interactive' &&
                    'transition-[border-color,box-shadow] duration-150 ' +
                    'hover:border-border-strong hover:shadow-sm',
                    paddingStyles[padding],
                    className,
                )}
                {...rest}
            >
                {children}
            </div>
        )
    },
)