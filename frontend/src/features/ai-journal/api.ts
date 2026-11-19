import { http } from '@/shared/api/http'
import type { AiJournalDailyResponse } from './types'
export interface AiJournalFilter {
    source?: string
    important?: 'HIGH' | 'MEDIUM' | 'LOW'
}

export const aiJournalApi = {
    briefing: (filter?: AiJournalFilter) => {
        const params = new URLSearchParams()
        if (filter?.source) params.set('source', filter.source)
        if (filter?.important) params.set('important', filter.important)
        const query = params.toString() ? `?${params.toString()}` : ''
        return http.get<AiJournalDailyResponse>(`/v1/ai-journal/briefing${query}`)
    },
}