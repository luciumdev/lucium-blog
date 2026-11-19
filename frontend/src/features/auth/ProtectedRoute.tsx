import { useAuthStore } from './store'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
/**
 * 인증 가드.
 *
 * 동작:
 *  - isAuthenticated=true → children 렌더
 *  - false → /login으로 리다이렉트, state.from에 원래 경로 보존
 *
 * 한 가지 트레이드오프:
 *  새로고침 직후엔 isAuthenticated=false (accessToken 메모리 휘발)인데,
 *  refreshToken으로 자동 복구하기 전에 이 가드가 먼저 실행되면 잘못 리다이렉트.
 *
 *  해결: refreshToken은 있는데 isAuthenticated=false인 경우 = "복구 진행 중"으로 보고
 *        잠깐 로딩 표시. 첫 API 호출이 401 → refresh → store update → 재렌더링되면
 *        isAuthenticated=true가 되어 정상 진입.
 *
 *  단, refreshToken 없으면 진짜 미인증 → 즉시 /login.
 *
 * 추후 role 기반 가드(예: admin only) 필요하면 prop으로 받게 확장.
 */
export function ProtectedRoute() {
    const location = useLocation()
    const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
    const refreshToken = useAuthStore((s) => s.refreshToken);

    if (!isAuthenticated && !refreshToken) {
        return <Navigate to="/login" state={{from: location}} replace />
    }

    if (!isAuthenticated && refreshToken) {
        return (
            <div className="font-mono text-sm text-ink-faint p-8">
                $ verifying session...
            </div>
        )
    }
    return <Outlet />
}