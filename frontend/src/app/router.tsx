import { createBrowserRouter } from 'react-router-dom'
import { ProtectedRoute } from '@/features/auth/ProtectedRoute'
import PublicLayout from './layouts/PublicLayout'
import AdminLayout from './layouts/AdminLayout'
import HomePage from '@/pages/HomePage'
import PostListPage from '@/pages/PostListPage'
import PostDetailPage from '@/pages/PostDetailPage'
import LoginPage from '@/pages/LoginPage'
import NotFoundPage from '@/pages/NotFoundPage'
import ErrorPage from '@/pages/ErrorPage'
import AdminDashboardPage from '@/pages/admin/AdminDashboardPage'
import PostManagePage from '@/pages/admin/PostManagePage'
import PostEditorPage from '@/pages/admin/PostEditorPage'
import SearchPage from '@/pages/SearchPage'
import MarketPage from '@/pages/MarketPage'
import AiJournalPage from '@/pages/AiJournalPage'
import AboutPage from '@/pages/AboutPage'
import StatusPage from '@/pages/StatusPage'

export const router = createBrowserRouter([
    {
        element: <PublicLayout />,
        errorElement: <ErrorPage />,
        children: [
            { path: '/', element: <HomePage /> },
            { path: '/posts', element: <PostListPage /> },
            { path: '/posts/:publicId', element: <PostDetailPage /> },
            { path: '/login', element: <LoginPage /> },
            { path: '/search', element: <SearchPage /> },
            { path: '/market', element: <MarketPage /> },
            { path: '/ai-journal', element: <AiJournalPage /> },
            { path: '/about', element: <AboutPage /> },
            { path: '/status', element: <StatusPage /> },
            // 없는 주소 — 레이아웃(헤더·사이드바) 안에서 404 화면
            { path: '*', element: <NotFoundPage /> },
        ],
    },
    {
        path: '/admin',
        element: <ProtectedRoute />,
        errorElement: <ErrorPage />,
        children: [
            {
                path: '/admin',
                element: <AdminLayout />,
                children: [
                    { index: true, element: <AdminDashboardPage /> },
                    { path: 'posts', element: <PostManagePage /> },
                    { path: 'posts/new', element: <PostEditorPage /> },
                    { path: 'posts/:publicId/edit', element: <PostEditorPage /> },
                ],
            },
        ],
    },
])
