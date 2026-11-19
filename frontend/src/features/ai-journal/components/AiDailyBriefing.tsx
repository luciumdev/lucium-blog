import type { AiHighlightCount } from '../types'
import { splitSentences } from '../lib/splitSentences'

interface Props {
    summary: string
    count: AiHighlightCount
    keywords: string[]
}

/**
 * 오늘의 요약 (P 시안).
 * 요약을 주제(문장)별로 한 줄씩 나눠 목록으로 표시
 * 테두리(border-border)로 페이지 배경과 구분 — 메인 카드와 같은 방식
 */
export function AiDailyBriefing({ summary, count, keywords }: Props) {
    const sentences = splitSentences(summary)

    return (
        <section className="bg-panel border border-border rounded-2xl p-6 sm:p-7">
            <header className="flex flex-wrap items-center justify-between gap-3">
                <h2 className="text-[17px] font-bold">오늘의 요약</h2>
                <dl className="flex items-center gap-1.5 text-[13px]">
                    <CountChip label="전체" value={count.total} className="bg-surface-muted text-ink-muted" />
                    <CountChip label="중요" value={count.high} className="bg-danger-tint text-danger" />
                    <CountChip label="보통" value={count.medium} className="bg-primary-tint text-primary-hover" />
                    <CountChip label="참고" value={count.low} className="bg-surface-muted text-ink-faint" />
                </dl>
            </header>

            {sentences.length > 0 && (
                <ul className="mt-4 space-y-2.5">
                    {sentences.map((sentence, i) => (
                        <li key={i} className="flex gap-3 text-[16px] leading-[1.7] text-ink">
                            <span aria-hidden className="mt-[0.7em] w-1.5 h-1.5 shrink-0 rounded-full bg-primary" />
                            <span className="[text-wrap:pretty]">{sentence}</span>
                        </li>
                    ))}
                </ul>
            )}

            {keywords.length > 0 && (
                <div className="mt-5 pt-4 border-t border-border flex flex-wrap items-center gap-1.5">
                    <span className="mr-1 text-[13px] font-medium text-ink-faint">키워드</span>
                    {keywords.map((kw) => (
                        <span key={kw} className="px-2.5 py-0.5 rounded-full bg-surface-muted text-[13px] text-ink-muted">
                            {kw}
                        </span>
                    ))}
                </div>
            )}
        </section>
    )
}

function CountChip({ label, value, className }: { label: string; value: number; className: string }) {
    return (
        <div className={`flex items-center gap-1 px-2.5 py-1 rounded-full ${className}`}>
            <dt>{label}</dt>
            <dd className="font-semibold tabular-nums">{value}</dd>
        </div>
    )
}
