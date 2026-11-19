import { Link, NavLink, Outlet, ScrollRestoration, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { useCallback, useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { cn } from '@/shared/lib/cn'
import { useAuthStore } from '@/features/auth/store'
import { authApi } from '@/features/auth/api'
import { useTagsQuery } from '@/features/tag/hooks/useTagsQuery'
import { LuciumWordmark } from '@/shared/components/LuciumLogo'

/**
 * 공통 레이아웃 (P · 메인 v2)
 *  - 헤더 64px: 로고(마스코트 + lucium.dev) · 검색 · GitHub · LinkedIn · RSS
 *  - 좌측 사이드 메뉴 240px 고정: 주 메뉴 → 태그 → (로그인 시) 관리 → 버전
 *  - 모바일(< md): 사이드 메뉴 숨김 → 하단 탭 바
 */

/** 외부 링크. 값이 없으면 헤더에서 숨김 */
const SOCIAL_LINKS: { label: string; href: string | null; icon: ReactNode }[] = [
    { label: 'GitHub', href: 'https://github.com/luciumdev', icon: <GitHubIcon /> },
    // TODO: LinkedIn 프로필 주소로 교체
    { label: 'LinkedIn', href: null, icon: <LinkedInIcon /> },
    // TODO: RSS 피드(/rss.xml) 백엔드 준비되면 '/rss.xml' 로 설정
    { label: 'RSS', href: null, icon: <RssIcon /> },
]

/** 빌드 시 주입 (예: CI 에서 VITE_APP_VERSION=v1.4). 없으면 표시 안 함 */
const APP_VERSION: string | undefined = import.meta.env.VITE_APP_VERSION
export default function PublicLayout() {
    const [paletteOpen, setPaletteOpen] = useState(false)
    const openPalette = useCallback(() => setPaletteOpen(true), [])
    const closePalette = useCallback(() => setPaletteOpen(false), [])

    useSearchShortcut(openPalette)

    return (
        <div className="min-h-screen bg-surface text-ink">
            <ScrollRestoration />
            <SiteHeader onSearch={openPalette} />

            <div className="flex">
                <Sidebar />
                {/* pb-24: 모바일 하단 탭 바에 가려지지 않도록 */}
                <main className="flex-1 min-w-0 px-4 sm:px-6 lg:px-8 pt-6 pb-24 md:pb-10">
                    <div className="max-w-[1200px] mx-auto">
                        <Outlet />
                    </div>
                </main>
            </div>

            <MobileTabBar />
            {paletteOpen && <SearchPalette onClose={closePalette} />}
        </div>
    )
}

/* ─────────────────────────── Header ─────────────────────────── */

function SiteHeader({ onSearch }: { onSearch: () => void }) {
    return (
        <header className="sticky top-0 z-40 h-16 bg-surface/90 backdrop-blur border-b border-border">
            <div className="h-full px-4 sm:px-6 flex items-center justify-between gap-4">
                <Link to="/" aria-label="lucium.dev 홈" className="shrink-0">
                    <LuciumWordmark height={40} />
                </Link>
                <div className="flex items-center gap-0.5">
                    <IconButton label="검색 (/)" onClick={onSearch}>
                        <SearchIcon />
                    </IconButton>
                    {SOCIAL_LINKS.filter((l) => l.href).map((l) => (
                        <a
                            key={l.label}
                            href={l.href as string}
                            target="_blank"
                            rel="noopener noreferrer"
                            aria-label={l.label}
                            className="w-11 h-11 flex items-center justify-center rounded-[10px] text-ink-muted hover:text-ink hover:bg-surface-muted transition-colors"
                        >
                            {l.icon}
                        </a>
                    ))}
                </div>
            </div>
        </header>
    )
}

function IconButton({ label, onClick, children }: { label: string; onClick: () => void; children: ReactNode }) {
    return (
        <button
            type="button"
            onClick={onClick}
            aria-label={label}
            title={label}
            className="w-11 h-11 flex items-center justify-center rounded-[10px] text-ink-muted hover:text-ink hover:bg-surface-muted transition-colors"
        >
            {children}
        </button>
    )
}

/* ─────────────────────────── Sidebar ─────────────────────────── */

/** short = 모바일 하단 탭 라벨 (좁은 폭에서 줄바꿈 방지) */
const NAV_ITEMS: { to: string; label: string; short: string; icon: ReactNode; end?: boolean }[] = [
    { to: '/', label: '홈', short: '홈', icon: <HomeIcon />, end: true },
    { to: '/posts', label: '게시글', short: '게시글', icon: <DocIcon /> },
    { to: '/market', label: '뉴스·마켓', short: '뉴스', icon: <ChartIcon /> },
    { to: '/ai-journal', label: 'AI 릴리즈 노트', short: 'AI 릴리즈', icon: <ChatIcon /> },
    { to: '/about', label: '소개', short: '소개', icon: <PersonIcon /> },
]

function Sidebar() {
    return (
        <aside
            className="
                hidden md:flex md:flex-col
                w-60 shrink-0
                sticky top-16 h-[calc(100vh-4rem)]
                px-4 pt-4 pb-5
            "
        >
            <nav aria-label="주 메뉴" className="flex flex-col gap-0.5">
                {NAV_ITEMS.map((item) => (
                    <SideNavLink key={item.to} to={item.to} label={item.label} icon={item.icon} end={item.end} />
                ))}
            </nav>

            <div className="mt-7 flex-1 min-h-0 overflow-y-auto">
                <SidebarTags />
                <SessionSection />
            </div>

            {APP_VERSION && <p className="pt-4 px-3.5 font-mono text-xs text-ink-faint">{APP_VERSION}</p>}
        </aside>
    )
}

/** 선택 표시는 배경 대신 왼쪽 세로선 — 장식 최소화 */
function SideNavLink({ to, label, icon, end }: { to: string; label: string; icon: ReactNode; end?: boolean }) {
    return (
        <NavLink
            to={to}
            end={end}
            className={({ isActive }) =>
                cn(
                    'flex items-center gap-3 h-[42px] px-3 border-l-2 text-[15px] transition-colors',
                    isActive
                        ? 'border-primary text-primary-hover font-semibold'
                        : 'border-transparent text-ink-muted font-medium hover:bg-surface-muted hover:text-ink',
                )
            }
        >
            {icon}
            {label}
        </NavLink>
    )
}

const SIDEBAR_TAG_LIMIT = 12

/** 태그 — 글 수 많은 순(백엔드 정렬) 상위 12개를 글자 링크로 */
function SidebarTags() {
    const { data, isPending, isError } = useTagsQuery()
    const { pathname } = useLocation()
    const [params] = useSearchParams()
    const activeSlug = pathname === '/posts' ? params.get('tag') : null

    if (isError) return null
    const tags = (data ?? []).filter((t) => t.postCount > 0).slice(0, SIDEBAR_TAG_LIMIT)

    return (
        <nav aria-label="태그" className="mb-6 px-3.5">
            <h2 className="mb-2.5 text-[13px] font-semibold text-ink-faint">태그</h2>

            {isPending ? (
                <div className="flex flex-wrap gap-2" aria-busy="true">
                    {Array.from({ length: 8 }).map((_, i) => (
                        <span key={i} className="h-4 w-14 bg-panel-elevated rounded animate-pulse" />
                    ))}
                </div>
            ) : tags.length === 0 ? (
                <p className="text-sm text-ink-faint">아직 태그가 없습니다.</p>
            ) : (
                <div className="flex flex-wrap gap-x-3.5 gap-y-1.5 leading-normal">
                    {tags.map((tag) => {
                        const active = activeSlug === tag.slug
                        return (
                            <Link
                                key={tag.id}
                                to={`/posts?tag=${encodeURIComponent(tag.slug)}`}
                                aria-current={active ? 'page' : undefined}
                                className={cn(
                                    'text-sm transition-colors',
                                    active ? 'text-primary-hover font-semibold' : 'text-ink hover:text-primary-hover',
                                )}
                            >
                                {tag.name}
                                <span className="ml-0.5 text-xs text-ink-faint">{tag.postCount}</span>
                            </Link>
                        )
                    })}
                </div>
            )}

            <Link to="/posts" className="inline-block mt-3 text-[13px] text-ink-faint hover:text-primary-hover transition-colors">
                태그 전체
            </Link>
        </nav>
    )
}

/** 로그인 상태일 때만 노출 — 관리자 진입 / 로그아웃 */
function SessionSection() {
    const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
    const refreshToken = useAuthStore((s) => s.refreshToken)
    const clear = useAuthStore((s) => s.clear)
    const navigate = useNavigate()

    if (!isAuthenticated) return null

    const handleLogout = async () => {
        if (refreshToken) {
            // 백엔드 토큰 무효화 — 실패해도 클라이언트 클리어는 진행
            try {
                await authApi.logout(refreshToken)
            } catch {
                /* noop */
            }
        }
        clear()
        navigate('/', { replace: true })
    }

    return (
        <div className="pt-4 border-t border-border">
            <h2 className="px-3.5 mb-1.5 text-[13px] font-semibold text-ink-faint">관리</h2>
            <Link
                to="/admin"
                className="flex items-center h-10 px-3.5 rounded-[10px] text-[15px] text-ink hover:bg-panel-elevated transition-colors"
            >
                관리자
            </Link>
            <button
                type="button"
                onClick={handleLogout}
                className="w-full flex items-center h-10 px-3.5 rounded-[10px] text-[15px] text-ink-muted hover:text-danger hover:bg-panel-elevated transition-colors"
            >
                로그아웃
            </button>
        </div>
    )
}

/* ─────────────────────────── Mobile tab bar ─────────────────────────── */

function MobileTabBar() {
    return (
        <nav
            aria-label="하단 메뉴"
            className="
                md:hidden fixed bottom-0 inset-x-0 z-40
                flex bg-panel border-t border-border
                pb-[env(safe-area-inset-bottom)]
            "
        >
            {NAV_ITEMS.map((item) => (
                <NavLink
                    key={item.to}
                    to={item.to}
                    end={item.end}
                    className={({ isActive }) =>
                        cn(
                            'flex-1 h-14 flex flex-col items-center justify-center gap-0.5 text-xs',
                            isActive ? 'text-primary font-semibold' : 'text-ink-muted',
                        )
                    }
                >
                    {item.icon}
                    {item.short}
                </NavLink>
            ))}
        </nav>
    )
}

/* ─────────────────────────── Search palette ─────────────────────────── */

/** "/" 또는 Ctrl/⌘+K로 검색창 열기. 입력 중일 때는 무시. */
function useSearchShortcut(onOpen: () => void) {
    useEffect(() => {
        const onKey = (e: KeyboardEvent) => {
            const target = e.target as HTMLElement | null
            const typing =
                target?.isContentEditable ||
                target?.tagName === 'INPUT' ||
                target?.tagName === 'TEXTAREA' ||
                target?.tagName === 'SELECT'
            const isCmdK = (e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k'
            if (isCmdK || (!typing && e.key === '/')) {
                e.preventDefault()
                onOpen()
            }
        }
        window.addEventListener('keydown', onKey)
        return () => window.removeEventListener('keydown', onKey)
    }, [onOpen])
}

function SearchPalette({ onClose }: { onClose: () => void }) {
    const navigate = useNavigate()
    const [q, setQ] = useState('')
    const tooShort = q.trim().length > 0 && q.trim().length < 2

    const submit = (e: FormEvent) => {
        e.preventDefault()
        const trimmed = q.trim()
        if (trimmed.length < 2) return
        navigate(`/search?q=${encodeURIComponent(trimmed)}`)
        onClose()
    }

    // ESC로 닫기
    useEffect(() => {
        const onKey = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onClose()
        }
        window.addEventListener('keydown', onKey)
        return () => window.removeEventListener('keydown', onKey)
    }, [onClose])

    // 뒷배경 스크롤 잠금
    useEffect(() => {
        const prev = document.body.style.overflow
        document.body.style.overflow = 'hidden'
        return () => {
            document.body.style.overflow = prev
        }
    }, [])

    return createPortal(
        <div
            className="fixed inset-0 z-[100] bg-ink/30 flex items-start justify-center pt-[15vh] px-4"
            onClick={onClose}
        >
            <form
                role="search"
                onSubmit={submit}
                onClick={(e) => e.stopPropagation()}
                className="w-full max-w-xl bg-panel rounded-2xl shadow-lg overflow-hidden"
            >
                <label htmlFor="palette-q" className="sr-only">글 검색</label>
                <div className="flex items-center gap-3 px-5 h-16 border-b border-border">
                    <SearchIcon className="w-5 h-5 text-ink-faint" />
                    <input
                        id="palette-q"
                        autoFocus
                        type="search"
                        value={q}
                        onChange={(e) => setQ(e.target.value)}
                        placeholder="스프링부트부터 트랜잭션 전파까지 검색해 보세요"
                        className="flex-1 min-w-0 bg-transparent text-[17px] text-ink placeholder:text-ink-faint focus:outline-none"
                    />
                    <kbd className="font-sans text-xs text-ink-faint border border-border-strong rounded px-1.5">esc</kbd>
                </div>
                <p className={cn('px-5 py-3 text-sm', tooShort ? 'text-danger' : 'text-ink-faint')}>
                    {tooShort ? '2자 이상 입력해 주세요.' : 'Enter로 검색 · 조사를 붙여도 찾는 형태소 검색'}
                </p>
            </form>
        </div>,
        document.body,
    )
}

/* ─────────────────────────── Icons ─────────────────────────── */

const iconProps = {
    width: 20,
    height: 20,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.8,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
    'aria-hidden': true,
}

function HomeIcon() {
    return <svg {...iconProps}><path d="M4 10.5 12 4l8 6.5V20H4z" /></svg>
}
function DocIcon() {
    return <svg {...iconProps}><path d="M6 3h9l4 4v14H6z" /><path d="M9 12h6M9 16h4" /></svg>
}
function ChartIcon() {
    return <svg {...iconProps}><path d="m4 18 5-6 4 3 7-9" /></svg>
}
function ChatIcon() {
    return <svg {...iconProps}><path d="M5 5h14v10H9l-4 4z" /></svg>
}
function PersonIcon() {
    return <svg {...iconProps}><circle cx="12" cy="8" r="3.5" /><path d="M5 20c1.3-3.5 4-5 7-5s5.7 1.5 7 5" /></svg>
}
function SearchIcon({ className }: { className?: string } = {}) {
    return (
        <svg {...iconProps} className={className}>
            <circle cx="11" cy="11" r="6.5" />
            <path d="m20 20-4-4" />
        </svg>
    )
}

function LinkedInIcon() {
    return (
        <svg {...iconProps}>
            <rect x="3" y="3" width="18" height="18" rx="3" />
            <path d="M8 10.5V17M8 7.2v.01M12 17v-3.6a2.2 2.2 0 0 1 4.4 0V17M12 10.5V17" />
        </svg>
    )
}
function RssIcon() {
    return (
        <svg {...iconProps}>
            <path d="M5 11a8 8 0 0 1 8 8M5 5a14 14 0 0 1 14 14" />
            <circle cx="6" cy="18" r="1.2" fill="currentColor" />
        </svg>
    )
}

/** GitHub 옥토캣 마크 (인라인 SVG — currentColor 상속) */
function GitHubIcon() {
    return (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path d="M12 .5C5.37.5 0 5.87 0 12.5c0 5.3 3.44 9.8 8.21 11.39.6.11.82-.26.82-.58 0-.29-.01-1.05-.02-2.06-3.34.73-4.04-1.61-4.04-1.61-.55-1.39-1.34-1.76-1.34-1.76-1.09-.75.08-.73.08-.73 1.21.09 1.84 1.24 1.84 1.24 1.07 1.84 2.81 1.31 3.5 1 .11-.78.42-1.31.76-1.61-2.67-.3-5.47-1.33-5.47-5.93 0-1.31.47-2.38 1.24-3.22-.13-.3-.54-1.52.12-3.18 0 0 1.01-.32 3.3 1.23a11.5 11.5 0 0 1 3-.4c1.02 0 2.05.14 3 .4 2.29-1.55 3.3-1.23 3.3-1.23.66 1.66.25 2.88.12 3.18.77.84 1.24 1.91 1.24 3.22 0 4.61-2.81 5.63-5.49 5.93.43.37.82 1.1.82 2.22 0 1.6-.02 2.9-.02 3.29 0 .32.22.7.83.58A12.01 12.01 0 0 0 24 12.5C24 5.87 18.63.5 12 .5z" />
        </svg>
    )
}
