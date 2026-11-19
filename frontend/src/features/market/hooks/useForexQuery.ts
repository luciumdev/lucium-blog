import { useQuery } from '@tanstack/react-query'
import { marketApi } from '../api'

export function useForexQuery() {
    return useQuery({
        queryKey: ['market', 'forex'],
        queryFn: () => marketApi.forex(),
        staleTime: 55_000,         // 1분 polling 직전까지 fresh // 조심
        refetchInterval: 60_000,   // 1분마다 백엔드 캐시 갱신 캐치
    })
}