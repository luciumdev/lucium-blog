import { http } from '@/shared/api/http'
import type { LoginRequest, TokenResponse } from './types'

export const authApi = {
    login: (body: LoginRequest) =>
        http.post<TokenResponse>('/v1/auth/login', body),

    refresh: (refreshToken: string) =>
        http.rawPost<TokenResponse>('/v1/auth/refresh', undefined, {
            headers: { 'X-Refresh-Token': refreshToken },
        }),
    logout: (refreshToken: string) =>
        http.post<void>('/v1/auth/logout', undefined, {
            headers: { 'X-Refresh-Token': refreshToken},
        }),
}
