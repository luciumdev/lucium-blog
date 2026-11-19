export interface SearchHit {
    publicId: string
    title: string       // <em>...</em> 하이라이팅 포함될 수 있음
    snippet: string     // content 하이라이팅 (없으면 summary)
    categoryName: string | null
    tags: string[]
    publishedAt: string | null
    score: number
}