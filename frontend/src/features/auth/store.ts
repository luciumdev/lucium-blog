import { create } from 'zustand'
import type { DecodedAccessToken, TokenResponse } from './types'

const REFRESH_KEY = 'ysh-blog:refresh-token'

interface AuthState {
    accessToken: string | null
    refreshToken: string | null
    user: { id: string; role: string } | null
    isAuthenticated: boolean

    setTokens: (tokens: TokenResponse) => void
    clear: () => void
    hydrate: () => void
}

export const useAuthStore = create<AuthState>()((set) => ({
    accessToken: null,
    refreshToken: null,
    user: null,
    isAuthenticated: false,

    setTokens: ({ accessToken, refreshToken }) => {
        const decoded = decodeJwt(accessToken)
        try {
            localStorage.setItem(REFRESH_KEY, refreshToken)
        } catch {
            // localStorage 차단 환경 — 무시
        }
        set({
            accessToken,
            refreshToken,
            user: decoded ? { id: decoded.sub, role: decoded.role } : null,
            isAuthenticated: true,
        })
    },

    clear: () => {
        try {
            localStorage.removeItem(REFRESH_KEY)
        } catch {
            // 무시
        }
        set({
            accessToken: null,
            refreshToken: null,
            user: null,
            isAuthenticated: false,
        })
    },

    hydrate: () => {
        try {
            const refreshToken = localStorage.getItem(REFRESH_KEY)
            if (refreshToken) {
                set({ refreshToken })
            }
        } catch {
            // 무시
        }
    },
}))

/**
 * Jwt payload 디코딩
 * BASE64URL -> JSON
 * @param token
 */
function decodeJwt(token: string): DecodedAccessToken | null {
    try {
        const payload = token.split('.')[1]
        const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
        const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
        const json = atob(padded)
        return JSON.parse(json) as DecodedAccessToken
    } catch {
        return null
    }
}

/**
 * shared/api/http - setAccessTokenProvider
 */
export function getAccessToken(): string | null {
    return useAuthStore.getState().accessToken
}
