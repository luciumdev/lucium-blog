import { http } from '@/shared/api/http'
import type { CollectionDaily, CollectionRunPage, CollectionRunsParams, CollectionStatus } from './types.ts'

export const collectionApi = {
    /** 메인 카드 */
    status: () => http.get<CollectionStatus>('/v1/stat/collection'),

    /** 실행 로그 — 최신순, 작업·상태 필터 */
    runs: (params: CollectionRunsParams) =>
        http.get<CollectionRunPage>('/v1/stat/collection/runs', {
            query: { job: params.job, status: params.status, page: params.page ?? 0, size: params.size ?? 50 },
        }),

    /** 작업별 일간 요약 */
    daily: (days = 30) => http.get<CollectionDaily>('/v1/stat/collection/daily', { query: { days } }),
}
