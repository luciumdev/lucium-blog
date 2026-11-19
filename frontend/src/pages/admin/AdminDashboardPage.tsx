import { Card } from '@/shared/components'
import { usePostStatQuery } from '@/features/stat/hooks/usePostStatQuery.ts'
import { useSiteStatQuery } from '@/features/stat/hooks/useSiteStatQuery.ts'

export default function AdminDashboardPage() {
    const { data: postStat, isPending, isError } = usePostStatQuery()
    const site = useSiteStatQuery()

    const postValue =
        isError ? '—' : isPending ? '…' : postStat!.total.toLocaleString()

    const visitValue =
        site.isError ? '—' : site.isPending ? '…' : site.data!.todayVisitors.toLocaleString()

    return (
        <section>
            <h1 className="font-serif text-2xl font-semibold text-ink mb-6">
                Dashboard
            </h1>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                <Card>
                    <p className="text-xs uppercase tracking-wider text-ink-faint">
                        오늘 방문
                    </p>
                    <p className="mt-2 font-serif text-3xl font-semibold text-ink">
                        {visitValue}
                    </p>
                    {!site.isError && !site.isPending && site.data && (
                        <p className="mt-1 text-xs text-ink-faint">
                            누적 {site.data.totalVisitors.toLocaleString()}
                        </p>
                    )}
                </Card>
                <Card>
                    <p className="text-xs uppercase tracking-wider text-ink-faint">
                        게시글
                    </p>
                    <p className="mt-2 font-serif text-3xl font-semibold text-ink">
                        {postValue}
                    </p>
                    {!isError && !isPending && postStat && (
                        <p className="mt-1 text-xs text-ink-faint">
                            발행 {postStat.published.toLocaleString()} · 초안{' '}
                            {postStat.draft.toLocaleString()}
                        </p>
                    )}
                </Card>
                <Card>
                    <p className="text-xs uppercase tracking-wider text-ink-faint">
                        대기 댓글
                    </p>
                    <p className="mt-2 font-serif text-3xl font-semibold text-ink">—</p>
                </Card>
            </div>
        </section>
    )
}
