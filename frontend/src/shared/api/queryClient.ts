import { QueryClient } from '@tanstack/react-query'
import { ApiError } from './ApiError'

export const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 60 * 1000,
            gcTime: 5 * 60 * 1000,
            retry: (failureCount, error) => {
                if (error instanceof ApiError && error.status >= 400 && error.status < 500) {
                    return false
                }
                return failureCount < 1
            },
            refetchOnWindowFocus: false,    // 과도한 refresh 방지
        },
        mutations: {
            retry: false,                   // 변경 작업은 절대 자동 재시도 금지
        },
    },
})