import { useSearchParams } from 'react-router-dom'
import { useSearchQuery } from '@/features/search/hooks/useSearchQuery'
import { ApiError } from '@/shared/api/ApiError'
import { SearchResultCard } from '@/features/search/components/SearchResultCard'

export default function SearchPage() {
    const [params] = useSearchParams()
    const keyword = params.get('q') ?? ''

    const { data, isPending, isError, error, fetchStatus } = useSearchQuery(keyword)

    // 검색어가 짧으면 isPending=true, fetchStatus='idle' (enabled=false 케이스)
    const isWaitingForKeyword = fetchStatus === 'idle' && keyword.trim().length < 2

    return (
        <section className="bg-panel rounded-2xl px-5 sm:px-7 pt-6 pb-4">
            <header>
                <h1 className="text-2xl sm:text-[26px]">
                    {keyword ? `‘${keyword}’ 검색 결과` : '검색'}
                </h1>
                <p className="mt-0.5 text-sm text-ink-faint">
                    {data ? `${data.totalElements.toLocaleString()}건 · 조사를 붙여도 찾는 형태소 검색` : ' '}
                </p>
            </header>

            <div className="mt-2">
                {isWaitingForKeyword && <p className="py-12 text-center text-[15px] text-ink-faint">2자 이상 입력해 주세요.</p>}

                {isPending && !isWaitingForKeyword && <SearchSkeleton />}

                {isError && (
                    <p className="my-6 p-4 rounded-xl bg-danger-tint text-[15px] text-danger">
                        {error instanceof ApiError ? `(${error.status}) ${error.message}` : '검색 중 오류가 발생했습니다.'}
                    </p>
                )}

                {data && data.content.length === 0 && (
                    <p className="py-12 text-center text-[15px] text-ink-faint">일치하는 글이 없습니다.</p>
                )}

                {data && data.content.length > 0 && (
                    <ul className="divide-y divide-border">
                        {data.content.map((hit) => (
                            <li key={hit.publicId}>
                                <SearchResultCard hit={hit} />
                            </li>
                        ))}
                    </ul>
                )}
            </div>
        </section>
    )
}

function SearchSkeleton() {
    return (
        <ul className="divide-y divide-border" aria-busy="true" aria-label="검색 결과 불러오는 중">
            {Array.from({ length: 5 }).map((_, i) => (
                <li key={i} className="py-4 space-y-2">
                    <div className="h-3 w-24 bg-panel-elevated rounded animate-pulse" />
                    <div className="h-5 w-2/3 bg-panel-elevated rounded animate-pulse" />
                    <div className="h-4 w-5/6 bg-panel-elevated rounded animate-pulse" />
                </li>
            ))}
        </ul>
    )
}
