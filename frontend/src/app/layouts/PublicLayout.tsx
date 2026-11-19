import { Link, NavLink, Outlet, ScrollRestoration, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { useCallback, useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { cn } from '@/shared/lib/cn'
import { useAuthStore } from '@/features/auth/store'
import { authApi } from '@/features/auth/api'
import { useCategoriesQuery } from '@/features/category/hooks/useCategoriesQuery'
import { LuciumWordmark } from '@/shared/components/LuciumLogo'
import { getCategoryColor } from '@/shared/lib/categoryColor'
import { CategoryIcon } from '@/shared/lib/categoryIcon'
import { useVisitTracker } from '@/features/stat/hooks/useVisitTracker'
/**
 * 공통 레이아웃 (P · 메인 v2)
 *  - 헤더 56px: 로고 · GitHub · LinkedIn · RSS — 구분선 없이 페이지 배경과 같은 색
 *    검색은 메인 검색창 + 단축키(/, ⌘K) 팔레트로
 *  - 좌측 사이드 메뉴 240px 고정: 주 메뉴(게시글 아래 카테고리) → (로그인 시) 관리 → 버전
 *    사이드 메뉴 위쪽 여백(pt-3)은 본문(main pt-3)과 같게 — 첫 메뉴와 첫 카드의 윗선이 일치
 *    메뉴 색은 회색 톤: 호버 #F2F3F5 → 누르는 순간·선택 #E9EBEE (+ 진한 세로선). 파랑은 쓰지 않음
 *    하위 카테고리를 고르면 선택 표시는 카테고리로 옮기고 '게시글'은 굵은 글씨만 남김
 *  - 모바일(< md): 사이드 메뉴 숨김 → 하단 탭 바
 */

/** 외부 링크. 값이 없으면 헤더에서 숨김 */
const SOCIAL_LINKS: { label: string; href: string | null; icon: ReactNode }[] = [
    { label: 'GitHub', href: 'https://github.com/luciumdev', icon: <GitHubIcon /> },
    // TODO: LinkedIn 프로필 주소로 교체
    { label: 'LinkedIn', href: null, icon: <LinkedInIcon /> },
    // 백엔드 RssController (/v1/rss) — nginx 가 /api 를 떼고 전달
    { label: 'RSS', href: '/api/v1/rss', icon: <RssIcon /> },
]

/** 빌드 시 주입 (예: CI 에서 VITE_APP_VERSION=v1.4). 없으면 표시 안 함 */
const APP_VERSION: string | undefined = import.meta.env.VITE_APP_VERSION
export default function PublicLayout() {
    const [paletteOpen, setPaletteOpen] = useState(false)
    const openPalette = useCallback(() => setPaletteOpen(true), [])
    const closePalette = useCallback(() => setPaletteOpen(false), [])

    useSearchShortcut(openPalette)
    useVisitTracker()

    return (
        <div className="min-h-screen bg-surface text-ink">
            <ScrollRestoration />
            <SiteHeader />

            <div className="flex">
                <Sidebar />
                {/* pb-24: 모바일 하단 탭 바에 가려지지 않도록 */}
                <main className="flex-1 min-w-0 px-4 sm:px-6 lg:px-8 pt-3 pb-24 md:pb-10">
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

function SiteHeader() {
    return (
        // 본문과 같은 격자: 왼쪽 = 사이드바 폭(w-60), 오른쪽 = main 과 같은 패딩 + max-w-[1200px].
        // 그래야 아이콘 오른쪽 끝이 본문 카드 오른쪽 끝과 맞음 (전체 폭 기준이면 넓은 화면에서 카드 밖으로 나감)
        <header className="sticky top-0 z-40 h-[56px] bg-surface flex">
            <div className="shrink-0 md:w-60 pl-4 sm:pl-7 flex items-center">
                <Link to="/" aria-label="lucium.dev 홈">
                    <LuciumWordmark height={34} />
                </Link>
            </div>
            <div className="flex-1 min-w-0 px-4 sm:px-6 lg:px-8">
                {/* -mr-3: 버튼(44px) 안쪽 여백만큼 당겨서 아이콘 '그림'의 끝을 카드 끝에 맞춤 */}
                <div className="max-w-[1200px] h-full mx-auto flex items-center justify-end gap-1 [&>a:last-child]:-mr-3">
                    {SOCIAL_LINKS.filter((l) => l.href).map((l) => {
                        const external = l.href!.startsWith('http')
                        return (
                            <a
                                key={l.label}
                                href={l.href!}
                                {...(external ? { target: '_blank', rel: 'noopener noreferrer' } : {})}
                                aria-label={l.label}
                                title={l.label}
                                className="w-11 h-11 flex items-center justify-center rounded-[10px] text-ink-muted hover:text-ink hover:bg-surface-muted transition-colors"
                            >
                                {l.icon}
                            </a>
                        )
                    })}
                </div>
            </div>
        </header>
    )
}

/* ─────────────────────────── Sidebar ─────────────────────────── */

/** short = 모바일 하단 탭 라벨 (좁은 폭에서 줄바꿈 방지) */
const NAV_ITEMS: { to: string; label: string; short: string; icon: ReactNode; end?: boolean }[] = [
    { to: '/', label: '홈', short: '홈', icon: <HomeIcon />, end: true },
    { to: '/posts', label: '게시글', short: '게시글', icon: <DocIcon /> },
    { to: '/market', label: '뉴스·마켓', short: '뉴스', icon: <ChartIcon /> },
    { to: '/ai-journal', label: 'AI 릴리즈 노트', short: 'AI 릴리즈', icon: <ChatIcon /> },
    // { to: '/about', label: '소개', short: '소개', icon: <PersonIcon /> },
]

function Sidebar() {
    const { pathname } = useLocation()
    const [params] = useSearchParams()
    // /posts?category=... 이면 '게시글' 대신 하위 카테고리가 선택 표시를 가져감
    const categorySelected = pathname === '/posts' && !!params.get('category')

    return (
        <aside
            className="
                hidden md:flex md:flex-col
                w-60 shrink-0
                sticky top-[56px] h-[calc(100vh-56px)]
                px-4 pt-3 pb-5
            "
        >
            <nav aria-label="주 메뉴" className="flex-1 min-h-0 overflow-y-auto flex flex-col gap-0.5">
                {NAV_ITEMS.map((item) => (
                    <div key={item.to}>
                        <SideNavLink
                            to={item.to}
                            label={item.label}
                            icon={item.icon}
                            end={item.end}
                            childActive={item.to === '/posts' && categorySelected}
                        />
                        {item.to === '/posts' && <CategorySubNav />}
                    </div>
                ))}
                <SessionSection />
            </nav>

            {APP_VERSION && <p className="pt-4 px-3.5 font-mono text-xs text-ink-faint">{APP_VERSION}</p>}
        </aside>
    )
}

/**
 * 게시글 하위 메뉴 — 카테고리.
 * 앞 아이콘(categoryIcon)은 평소 회색, 선택된 카테고리만 카테고리 색(= 최근 글 배지 색)
 */
function CategorySubNav() {
    const { data, isPending, isError } = useCategoriesQuery()
    const { pathname } = useLocation()
    const [params] = useSearchParams()
    const activeSlug = pathname === '/posts' ? params.get('category') : null

    if (isError) return null
    if (isPending) {
        return (
            <div className="ml-[46px] my-1 flex flex-col gap-2.5 py-1" aria-busy="true">
                {Array.from({ length: 3 }).map((_, i) => (
                    <span key={i} className="h-3.5 w-20 bg-panel-elevated rounded animate-pulse" />
                ))}
            </div>
        )
    }
    if (!data || data.length === 0) return null

    return (
        <ul aria-label="카테고리" className="ml-[23px] mt-0.5 mb-1.5 border-l border-border">
            {data.map((category) => {
                const active = activeSlug === category.slug
                return (
                    <li key={category.id}>
                        <Link
                            to={`/posts?category=${encodeURIComponent(category.slug)}`}
                            aria-current={active ? 'page' : undefined}
                            className={cn(
                                'group flex items-center gap-2.5 h-9 pl-[20px] pr-2 -ml-px border-l rounded-r-lg text-sm transition-colors',
                                active
                                    ? 'border-ink bg-[#E9EBEE] text-ink font-semibold'
                                    : 'border-transparent text-ink-muted hover:bg-[#F2F3F5] hover:text-ink active:bg-[#E9EBEE]',
                            )}
                        >
                            <span
                                aria-hidden
                                className={cn('flex shrink-0 transition-colors', !active && 'text-[#9AA0A6] group-hover:text-ink-muted')}
                                style={active ? { color: getCategoryColor(category.slug).fg } : undefined}
                            >
                                <CategoryIcon slug={category.slug} className="w-3.5 h-3.5" />
                            </span>
                            <span className="truncate">{category.name}</span>
                        </Link>
                    </li>
                )
            })}
        </ul>
    )
}

/**
 * 주 메뉴 한 줄. 상태별 색 (회색 톤)
 *  - 기본: 회색 글씨 / 호버: 연회색 배경 / 누르는 순간·선택: 진한 회색 배경 + 진한 세로선
 *  - childActive: 하위 카테고리가 선택된 '게시글' → 배경 없이 굵은 글씨만
 */
function SideNavLink({ to, label, icon, end, childActive }: {
    to: string
    label: string
    icon: ReactNode
    end?: boolean
    childActive?: boolean
}) {
    return (
        <NavLink
            to={to}
            end={end}
            className={({ isActive }) =>
                cn(
                    'flex items-center gap-3 h-[42px] px-3 border-l-2 rounded-r-lg text-[15px] transition-colors',
                    isActive && !childActive
                        ? 'border-ink bg-[#E9EBEE] text-ink font-semibold'
                        : isActive
                            ? 'border-transparent text-ink font-semibold hover:bg-[#F2F3F5]'
                            : 'border-transparent text-ink-muted font-medium hover:bg-[#F2F3F5] hover:text-ink active:bg-[#E9EBEE]',
                )
            }
        >
            {icon}
            {label}
        </NavLink>
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
        <div className="mt-6 pt-4 border-t border-border">
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
// function PersonIcon() {
//     return <svg {...iconProps}><circle cx="12" cy="8" r="3.5" /><path d="M5 20c1.3-3.5 4-5 7-5s5.7 1.5 7 5" /></svg>
// }
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

/** GitHub — 시안과 같은 선형 아이콘 (다른 헤더 아이콘과 굵기 통일) */
function GitHubIcon() {
    return (
        <svg {...iconProps}>
            <path d="M9 19c-4.3 1.4-4.3-2.5-6-3m12 5v-3.5c0-1 .1-1.4-.5-2 2.8-.3 5.5-1.4 5.5-6a4.6 4.6 0 0 0-1.3-3.2 4.2 4.2 0 0 0-.1-3.2s-1.1-.3-3.5 1.3a12.3 12.3 0 0 0-6.2 0C6.5 2.8 5.4 3.1 5.4 3.1a4.2 4.2 0 0 0-.1 3.2A4.6 4.6 0 0 0 4 9.5c0 4.6 2.7 5.7 5.5 6-.6.6-.6 1.2-.5 2V21" />
        </svg>
    )
}
