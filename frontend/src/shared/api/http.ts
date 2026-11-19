import { ApiError } from '@/shared/api/ApiError'
import type { ApiResponse, ProblemDetail } from '@/shared/api/types'

const BASE_URL = '/api'

/* === 토큰 공급자 === */
let getAccessToken: () => string | null = () => null
export function setAccessTokenProvider(fn: () => string | null) {
    getAccessToken = fn
}

/* === Refresh 콜백 — 외부에서 주입 === */

/**
 * 401 받았을 때 호출되는 refresh 함수.
 * AuthBootstrap에서 setRefreshHandler로 등록.
 *
 * 반환:
 *  - 성공: 새 accessToken
 *  - 실패: null (호출자는 store clear + redirect 처리)
 *
 * 등록 안 되어있으면 항상 null — 401은 그대로 throw됨.
 */
let refreshHandler: () => Promise<string | null> = async () => null
export function setRefreshHandler(fn: () => Promise<string | null>) {
    refreshHandler = fn
}

/**
 * 동시 요청 N개가 모두 401 받았을 때 refresh를 N번 호출하는 거 방지.
 * 첫 401이 refresh 시작하면 그 Promise를 모듈 변수에 캐싱,
 * 후속 401들은 같은 Promise를 await.
 */
let refreshInFlight: Promise<string | null> | null = null

function getOrStartRefresh(): Promise<string | null> {
    if (!refreshInFlight) {
        refreshInFlight = refreshHandler().finally(() => {
            refreshInFlight = null
        })
    }
    return refreshInFlight
}

/* === 요청 옵션 === */
interface RequestOptions extends Omit<RequestInit, 'body'> {
    body?: unknown
    query?: Record<string, string | number | boolean | undefined | null>
}

/**
 * 메인 request 함수.
 *
 * @param skipAuthRetry refresh 후 재시도 시 true로 설정해 무한 루프 방지.
 *                       refresh 자체 호출(`POST /v1/auth/refresh`)도 이걸로 단락.
 */
async function request<T>(
    path: string,
    options: RequestOptions = {},
    skipAuthRetry = false,
): Promise<T> {
    const { body, query, headers, ...rest } = options

    const url = BASE_URL + path + buildQueryString(query)

    const finalHeaders = new Headers(headers)
    finalHeaders.set('Accept', 'application/json')
    if (body !== undefined) {
        finalHeaders.set('Content-Type', 'application/json')
    }
    const token = getAccessToken()
    if (token) {
        finalHeaders.set('Authorization', `Bearer ${token}`)
    }

    let response: Response
    try {
        response = await fetch(url, {
            ...rest,
            headers: finalHeaders,
            body: body !== undefined ? JSON.stringify(body) : undefined,
            credentials: 'include',
        })
    } catch (e) {
        throw ApiError.network(e)
    }

    /* === 401 인터셉터 === */
    if (response.status === 401 && !skipAuthRetry) {
        const newToken = await getOrStartRefresh()
        if (newToken) {
            // 새 토큰으로 원 요청 1회 재시도. skipAuthRetry로 무한 루프 차단.
            return request<T>(path, options, true)
        }
        // refresh 실패 — 401 그대로 throw, 호출부(또는 zustand)가 redirect 처리
    }

    // 204 No Content
    if (response.status === 204) {
        return undefined as T
    }

    const contentType = response.headers.get('Content-Type') ?? ''
    const isJson = contentType.includes('application/json')
    const payload = isJson ? await response.json().catch(() => null) : null

    if (!response.ok) {
        const problem = (payload as ProblemDetail | null) ?? undefined
        const message = problem?.detail ?? `HTTP ${response.status}`
        throw new ApiError(message, response.status, problem)
    }

    // 백엔드 ApiResponse<T> 래핑 자동 unwrap
    if (payload && typeof payload === 'object' && 'success' in payload) {
        return (payload as ApiResponse<T>).data
    }
    return payload as T
}

function buildQueryString(query?: RequestOptions['query']): string {
    if (!query) return ''
    const params = new URLSearchParams()
    for (const [k, v] of Object.entries(query)) {
        if (v !== undefined && v !== null && v !== '') {
            params.set(k, String(v))
        }
    }
    const qs = params.toString()
    return qs ? `?${qs}` : ''
}

/* === Public API === */
export const http = {
    get: <T>(path: string, options?: Omit<RequestOptions, 'body'>) =>
        request<T>(path, { ...options, method: 'GET' }),

    post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
        request<T>(path, { ...options, method: 'POST', body }),

    put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
        request<T>(path, { ...options, method: 'PUT', body }),

    patch: <T>(path: string, body?: unknown, options?: RequestOptions) =>
        request<T>(path, { ...options, method: 'PATCH', body }),

    del: <T>(path: string, options?: RequestOptions) =>
        request<T>(path, { ...options, method: 'DELETE' }),

    /**
     * Refresh 호출 전용 — 401 받아도 인터셉터 안 타게 skipAuthRetry=true.
     * authApi.refresh가 직접 fetch 안 쓰고 이걸 통해 호출하면 일관성 OK.
     * 지금은 authApi.refresh가 http.post 쓰는데, 그게 401 받아도 무한루프 안 빠지게 안전판.
     */
    rawPost: <T>(path: string, body?: unknown, options?: RequestOptions) =>
        request<T>(path, { ...options, method: 'POST', body }, true),
}