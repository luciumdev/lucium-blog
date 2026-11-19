import { forwardRef, type ButtonHTMLAttributes } from 'react'
import { cn } from '@/shared/lib/cn'

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger'
export type ButtonSize = 'sm' | 'md' | 'lg'

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
    variant?: ButtonVariant
    size?: ButtonSize
    isLoading?: boolean
}

/**
 * 디자인 토큰 기반 버튼.
 *
 * variant 결정 가이드:
 *  - primary: 페이지의 주요 CTA 1개 (저장, 발행, 로그인)
 *  - secondary: 보조 액션 (취소 옆에 같이 있는 미리보기 등)
 *  - ghost: 인라인 액션 (테이블 행의 편집/삭제)
 *  - danger: 파괴적 액션 (삭제 확인)
 *
 * isLoading은 disabled를 동시에 활성화 — 버튼 누른 직후 중복 클릭 차단.
 */
const variantStyles: Record<ButtonVariant, string> = {
    primary:
        'bg-primary text-white hover:bg-primary-hover focus-visible:ring-primary',
    secondary:
        'bg-primary-tint text-primary hover:bg-primary-mid focus-visible:ring-primary',
    ghost:
        'bg-transparent text-ink hover:bg-surface-muted focus-visible:ring-border-strong',
    danger:
        'bg-danger text-white hover:bg-red-700 focus-visible:ring-danger',
}

const sizeStyles: Record<ButtonSize, string> = {
    sm: 'h-8 px-3 text-sm',
    md: 'h-10 px-4 text-sm',
    lg: 'h-12 px-6 text-base',
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
    function Button(
        { variant = 'primary', size = 'md', isLoading, disabled, className, children, ...rest },
        ref,
    ) {
        return (
            <button
                ref={ref}
                disabled={disabled || isLoading}
                className={cn(
                    'inline-flex items-center justify-center gap-2 rounded-md font-medium',
                    'transition-colors duration-150',
                    'focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-2',
                    'disabled:opacity-50 disabled:cursor-not-allowed',
                    variantStyles[variant],
                    sizeStyles[size],
                    className,
                )}
                {...rest}
            >
                {isLoading ? <Spinner /> : children}
            </button>
        )
    },
)

function Spinner() {
    return (
        <svg
            className="animate-spin h-4 w-4"
            viewBox="0 0 24 24"
            fill="none"
            aria-hidden="true"
        >
            <circle
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                strokeOpacity="0.25"
                strokeWidth="4"
            />
            <path
                d="M4 12a8 8 0 0 1 8-8"
                stroke="currentColor"
                strokeWidth="4"
                strokeLinecap="round"
            />
        </svg>
    )
}