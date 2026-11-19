import { Link, NavLink, Outlet } from 'react-router-dom'
import { useAuthStore } from '@/features/auth/store'
import { authApi } from '@/features/auth/api'
import { useNavigate } from 'react-router-dom'
import { cn } from '@/shared/lib/cn'

export default function AdminLayout() {
    const user = useAuthStore((s) => s.user)
    const refreshToken = useAuthStore((s) => s.refreshToken)
    const clear = useAuthStore((s) => s.clear)
    const navigate = useNavigate()

    const handleLogout = async () => {
        if (refreshToken) {
            try {
                await authApi.logout(refreshToken)
            } catch {}
        }
        clear()
        navigate('/', { replace: true })
    }

    return (
        <div className="min-h-screen flex bg-surface text-ink">
            <aside className="w-60 shrink-0 border-r border-border bg-surface-muted flex flex-col">
                <div className="px-5 h-14 flex items-center justify-between border-b border-border">
                    <Link to="/" className="font-mono text-sm text-accent">
                        $ ./admin
                    </Link>
                </div>

                <nav className="p-3 flex flex-col gap-0.5">
                    <SideItem to="/admin" end>dashboard</SideItem>
                    <SideItem to="/admin/posts">posts</SideItem>
                    {/* 추후: comments */}
                </nav>

                <div className="mt-auto p-3 border-t border-border space-y-2">
                    {user && (
                        <p className="font-mono text-[10px] text-ink-faint">
                            user_id: {user.id} · {user.role}
                        </p>
                    )}
                    <button
                        type="button"
                        onClick={handleLogout}
                        className="
              w-full text-left px-2 py-1.5 rounded-md
              font-mono text-sm text-ink-muted
              hover:text-danger hover:bg-panel
              transition-colors
            "
                    >
                        $ logout
                    </button>
                    <Link
                        to="/"
                        className="block px-2 py-1 font-mono text-xs text-ink-faint hover:text-ink-muted transition-colors"
                    >
                        ← back to site
                    </Link>
                </div>
            </aside>

            <main className="flex-1 overflow-x-auto">
                <div className="p-8">
                    <Outlet />
                </div>
            </main>
        </div>
    )
}

function SideItem({
                      to,
                      end,
                      children,
                  }: {
    to: string
    end?: boolean
    children: React.ReactNode
}) {
    return (
        <NavLink
            to={to}
            end={end}
            className={({ isActive }) =>
                cn(
                    'px-3 py-2 rounded-md font-mono text-sm transition-colors',
                    isActive
                        ? 'bg-panel text-accent'
                        : 'text-ink-muted hover:bg-panel hover:text-ink',
                )
            }
        >
            {children}
        </NavLink>
    )
}