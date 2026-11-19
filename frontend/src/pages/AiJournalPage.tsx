import { Card } from '@/shared/components'
import { AiDailyBriefing } from '@/features/ai-journal/components/AiDailyBriefing'
import { AiNewsFeed } from '@/features/ai-journal/components/AiNewsFeed'
import { useAiJournalQuery } from '@/features/ai-journal/hooks/useAiJournalQuery'
import { staleLabel } from '@/features/ai-journal/lib/journalStatus'

/**
 * 표시 우선순위: 데이터가 있으면 항상 데이터를 보여주고, 오류·이전 수집본 여부는 안내 배너로만 알림.
 */
export default function AiJournalPage() {
    const { data, isPending, isError, refetch, isFetching } = useAiJournalQuery()

    const briefing = data?.briefing
    const items = briefing?.items ?? []
    const failedSources = data?.failedSources ?? []
    const hasData = items.length > 0

    // 1. 첫 로딩
    if (isPending && !hasData) {
        return (
            <div className="space-y-6">
                <PageHeader />
                <BriefingSkeleton />
                <FeedSkeleton />
            </div>
        )
    }

    // 2. 데이터 없음 — 오류인지 단순히 비었는지 구분
    if (!hasData) {
        return (
            <div className="space-y-6">
                <PageHeader />
                <Card padding="lg">
                    {isError ? (
                        <div className="flex flex-wrap items-center justify-between gap-3">
                            <p className="text-[15px] text-danger">AI 릴리즈 노트를 불러오지 못했습니다.</p>
                            <RetryButton onClick={() => refetch()} loading={isFetching} />
                        </div>
                    ) : (
                        <p className="text-[15px] text-ink-faint">아직 수집된 AI 뉴스가 없습니다.</p>
                    )}
                    {failedSources.length > 0 && <FailedSources sources={failedSources.map((s) => s.source)} />}
                </Card>
            </div>
        )
    }

    // 3. 데이터 있음 — 오류/이전 수집본이면 배너만 추가
    const stale = staleLabel(data?.journalDate)

    return (
        <div className="space-y-6">
            <PageHeader />

            {isError && (
                <Notice tone="danger">
                    <span>최신 정보를 불러오지 못해 마지막으로 받은 내용을 표시하고 있습니다.</span>
                    <RetryButton onClick={() => refetch()} loading={isFetching} />
                </Notice>
            )}
            {!isError && stale && (
                <Notice tone="info">
                    <span>오늘 수집이 아직 완료되지 않아 {stale}.</span>
                </Notice>
            )}
            {failedSources.length > 0 && <FailedSources sources={failedSources.map((s) => s.source)} />}

            <AiDailyBriefing
                summary={briefing?.daily_summary ?? ''}
                count={briefing?.highlight_count ?? { total: items.length, high: 0, medium: 0, low: 0 }}
                keywords={briefing?.top_keywords ?? []}
            />

            <section>
                <h2 className="text-[17px] font-bold mb-3">{stale ? '수집된 뉴스' : '오늘의 뉴스'}</h2>
                <AiNewsFeed items={items} />
            </section>
        </div>
    )
}

function PageHeader() {
    return (
        <header>
            <h1 className="text-2xl sm:text-[26px]">AI 릴리즈 노트</h1>
            <p className="mt-0.5 text-sm text-ink-faint">AI/ML 기술 동향 — 매일 06:00 KST 자동 수집 · LLM 요약</p>
        </header>
    )
}

function Notice({ tone, children }: { tone: 'danger' | 'info'; children: React.ReactNode }) {
    const cls = tone === 'danger' ? 'bg-danger-tint text-danger' : 'bg-primary-tint text-primary-hover'
    return (
        <div role="status" className={`flex flex-wrap items-center justify-between gap-3 px-4 py-3 rounded-xl text-[15px] ${cls}`}>
            {children}
        </div>
    )
}

function RetryButton({ onClick, loading }: { onClick: () => void; loading: boolean }) {
    return (
        <button
            type="button"
            onClick={onClick}
            disabled={loading}
            className="h-9 px-3.5 rounded-lg bg-panel border border-border-strong text-sm font-medium text-ink hover:bg-panel-elevated disabled:opacity-60 transition-colors"
        >
            {loading ? '다시 불러오는 중…' : '다시 시도'}
        </button>
    )
}

function FailedSources({ sources }: { sources: string[] }) {
    return <p className="mt-2 text-sm text-warning">일부 소스 수집 실패: {sources.join(', ')}</p>
}

function BriefingSkeleton() {
    return (
        <Card padding="lg">
            <div className="space-y-2" aria-busy="true">
                <div className="h-4 w-1/3 bg-panel-elevated rounded animate-pulse" />
                <div className="h-5 w-full bg-panel-elevated rounded animate-pulse" />
                <div className="h-5 w-5/6 bg-panel-elevated rounded animate-pulse" />
            </div>
        </Card>
    )
}

function FeedSkeleton() {
    return (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
            {Array.from({ length: 6 }).map((_, i) => (
                <Card key={i} padding="md">
                    <div className="space-y-2" aria-busy="true">
                        <div className="h-3 w-1/3 bg-panel-elevated rounded animate-pulse" />
                        <div className="h-4 w-full bg-panel-elevated rounded animate-pulse" />
                        <div className="h-4 w-5/6 bg-panel-elevated rounded animate-pulse" />
                    </div>
                </Card>
            ))}
        </div>
    )
}
