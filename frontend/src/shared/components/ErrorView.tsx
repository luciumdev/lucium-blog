import { Link } from 'react-router-dom'
import { LuciumMascot } from '@/shared/components/LuciumLogo'

/**
 * 공통 에러 화면 — 404(NotFoundPage) · 렌더 오류(ErrorPage)가 같이 씀.
 * 백엔드 JSON(ProblemDetail)을 그대로 보여주지 않고 이 화면으로 통일.
 * nginx 가 직접 내려주는 정적 페이지는 public/errors/*.html (같은 디자인).
 */
interface ErrorViewProps {
    code: string
    title: string
    detail: string
    /** 개발 환경에서만 넘김 — 스택 트레이스 */
    debug?: string
}

export function ErrorView({ code, title, detail, debug }: ErrorViewProps) {
    return (
        <div className="fx flex flex-col items-center text-center px-4 py-16 sm:py-24">
            <span className="w-24 h-24 rounded-full bg-primary-tint shadow-[inset_0_0_0_1px_#CFE5F5] flex items-center justify-center overflow-hidden">
                <LuciumMascot size={76} />
            </span>

            <p className="mt-6 font-mono text-sm font-semibold tracking-wider text-primary">{code}</p>
            <h1 className="mt-1.5 text-2xl sm:text-[28px] font-bold tracking-tight text-ink">{title}</h1>
            <p className="mt-2.5 max-w-md text-[15px] leading-relaxed text-ink-muted [text-wrap:pretty]">{detail}</p>

            <div className="mt-7 flex flex-wrap justify-center gap-2">
                <Link
                    to="/"
                    className="h-10 px-5 flex items-center rounded-full bg-primary text-sm font-medium text-white hover:bg-primary-hover transition-colors"
                >
                    홈으로
                </Link>
                <button
                    type="button"
                    onClick={() => window.history.back()}
                    className="h-10 px-5 flex items-center rounded-full bg-primary-subtle text-sm font-medium text-primary-hover hover:bg-primary-tint transition-colors"
                >
                    이전 페이지
                </button>
            </div>

            {debug && (
                <pre className="mt-8 w-full max-w-2xl overflow-auto text-left text-xs text-ink-faint bg-surface-muted p-4 rounded-[10px]">
                    {debug}
                </pre>
            )}
        </div>
    )
}
