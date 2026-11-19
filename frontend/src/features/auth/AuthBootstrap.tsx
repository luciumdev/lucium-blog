import {useEffect} from "react";
import {getAccessToken, useAuthStore} from "@/features/auth/store.ts";
import {setAccessTokenProvider, setRefreshHandler} from '@/shared/api/http'
import {authApi} from "@/features/auth/api.ts";

/**
 * 웹 시작 시 한번 실행함
 * 1. http래퍼에 토큰 공급자
 * 2. http 래퍼에 refresh 핸들러 등록 (401 자동 refresh 흐름)
 * 3. 로컬스토리지의 refresttoken복원함
 *
 */
export function AuthBootstrap() {
    useEffect(() => {
        setAccessTokenProvider(getAccessToken)

        setRefreshHandler(async () => {
            const { refreshToken, setTokens, clear } = useAuthStore.getState()
            if (!refreshToken) return null

            try {
                const tokens = await authApi.refresh(refreshToken)
                setTokens(tokens)
                return tokens.accessToken
            } catch {
                // refreshToken도 만료/무효 → 클리어. redirect는 ProtectedRoute가 처리
                clear()
                return null
            }
        })
        useAuthStore.getState().hydrate()
        // dev 디버깅용 — prod 빌드 시 트리쉐이킹으로 제거됨
        if (import.meta.env.DEV) {
            ;(window as unknown as { __auth: typeof useAuthStore }).__auth = useAuthStore
        }
    }, [])

    return null
}

