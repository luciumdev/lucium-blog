import { Link } from 'react-router-dom'
import { format } from 'date-fns'
import type { SearchHit } from '../types'

/**
 * 검색 결과 한 줄

 * 주의: title/snippet은 백엔드 ES highlighter가 만든 HTML.
 * 본문 원문에 HTML이 섞여 있으면 그대로 렌더되므로, 백엔드에서
 * highlight encoder("html")로 원문을 이스케이프하는지 확인 필요 (아래 안내 참고).
 */
export function SearchResultCard({ hit }: { hit: SearchHit }) {
    const date = hit.publishedAt ? format(new Date(hit.publishedAt), 'yyyy.MM.dd') : null
    const meta = [hit.categoryName, date].filter(Boolean).join(' · ')

    return (
        <Link to={`/posts/${hit.publicId}`} className="group block py-4 search-hl">
            {meta && <p className="text-[13px] font-medium text-ink-faint">{meta}</p>}
            <h2
                className="mt-0.5 text-[17px] sm:text-lg font-semibold text-ink group-hover:text-primary transition-colors"
                dangerouslySetInnerHTML={{ __html: hit.title }}
            />
            {hit.snippet && (
                <p
                    className="mt-1 text-[15px] text-ink-muted line-clamp-2"
                    dangerouslySetInnerHTML={{ __html: hit.snippet }}
                />
            )}
            {hit.tags.length > 0 && (
                <p className="mt-2 flex flex-wrap gap-1.5">
                    {hit.tags.slice(0, 3).map((tag) => (
                        <span key={tag} className="px-2.5 py-0.5 rounded-full bg-surface-muted text-[13px] text-ink-muted">
                            {tag}
                        </span>
                    ))}
                </p>
            )}
        </Link>
    )
}
