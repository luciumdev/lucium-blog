import { http } from '@/shared/api/http'
import type {
    ForexResponse,
    BriefingResponse,
    NewsCategory,
    IndicesHistoryResponse,
    NewsResponse,
    CommoditiesResponse,
    CommonKeywordsResponse,
    KeywordTrendResponse,
} from './types'
import type { IndicesData } from './types'

export const marketApi = {
    forex: () => http.get<ForexResponse>('/v1/market/forex'),
    briefing: () => http.get<BriefingResponse>('/v1/market/briefing'),
    news: (category: NewsCategory) =>
        http.get<NewsResponse>('/v1/market/news', { query: { category } }),
    newsKeywords: (days = 1) =>
        http.get<CommonKeywordsResponse>('/v1/market/news/keywords', { query: { days } }),
    keywordTrend: (keyword: string, days = 5) =>
        http.get<KeywordTrendResponse>('/v1/market/news/keywords/trend', { query: { keyword, days } }),
    indices: () => http.get<IndicesData>('/v1/market/indices'),
    commodities: () => http.get<CommoditiesResponse>('/v1/market/commodities'),
    history: (days = 30) => http.get<IndicesHistoryResponse>('/v1/market/indices/history', { query: { days },
        }),
}
