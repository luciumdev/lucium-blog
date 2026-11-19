import { forwardRef, useId, type InputHTMLAttributes } from 'react'
import { cn } from '@/shared/lib/cn'

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
    label?: string
    error?: string
    helperText?: string
}

/**
 * label / helperText / error 일체형 input.
 *
 * useId() — React 18에서 SSR-safe 한 unique id. 직접 Math.random 쓰면
 *           hydration mismatch 위험 있음.
 *
 * aria-invalid + aria-describedby 자동 연결 — 스크린리더에서 에러 함께 읽음.
 */
export const Input = forwardRef<HTMLInputElement, InputProps>(
    function Input({ label, error, helperText, id, className, ...rest }, ref) {
        const reactId = useId()
        const inputId = id ?? reactId
        const helperId = `${inputId}-helper`

        return (
            <div className="flex flex-col gap-1.5">
                {label && (
                    <label
                        htmlFor={inputId}
                        className="text-sm font-medium text-ink"
                    >
                        {label}
                    </label>
                )}
                <input
                    ref={ref}
                    id={inputId}
                    aria-invalid={!!error || undefined}
                    aria-describedby={error || helperText ? helperId : undefined}
                    className={cn(
                        'h-10 px-3 rounded-md border text-sm bg-surface text-ink',
                        'placeholder:text-ink-faint',
                        'focus:outline-none focus:ring-2 focus:ring-offset-1',
                        'disabled:bg-surface-muted disabled:opacity-60 disabled:cursor-not-allowed',
                        error
                            ? 'border-danger focus:border-danger focus:ring-danger'
                            : 'border-border focus:border-primary focus:ring-primary',
                        className,
                    )}
                    {...rest}
                />
                {(error || helperText) && (
                    <p
                        id={helperId}
                        className={cn(
                            'text-xs',
                            error ? 'text-danger' : 'text-ink-muted',
                        )}
                    >
                        {error ?? helperText}
                    </p>
                )}
            </div>
        )
    },
)