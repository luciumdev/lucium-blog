import { Link, isRouteErrorResponse, useRouteError } from 'react-router-dom'
import { ErrorView } from '@/shared/components/ErrorView'
import { LuciumWordmark } from '@/shared/components/LuciumLogo'

/**
 * 라우트 errorElement — 렌더/loader 에서 throw 된 오류.
 * 레이아웃 자체가 깨졌을 수 있으므로 PublicLayout 없이 단독으로 그림 (로고만 상단에).
 * 백엔드 메시지·스택은 운영에서 노출하지 않음.
 */
export default function ErrorPage() {
    const error = useRouteError()
    const { code, title, detail } = toView(error)
    const debug = import.meta.env.DEV && error instanceof Error ? error.stack : undefined

    return (
        <div className="min-h-screen bg-surface">
            <header className="h-[68px] px-4 sm:px-7 flex items-center">
                <Link to="/" aria-label="lucium.dev 홈">
                    <LuciumWordmark height={40} />
                </Link>
            </header>
            <main>
                <ErrorView code={code} title={title} detail={detail} debug={debug} />
            </main>
        </div>
    )
}

function toView(error: unknown): { code: string; title: string; detail: string } {
    if (isRouteErrorResponse(error)) {
        if (error.status === 404) {
            return { code: '404', title: '페이지를 찾을 수 없습니다', detail: '주소가 바뀌었거나 삭제된 페이지일 수 있습니다.' }
        }
        if (error.status === 401 || error.status === 403) {
            return { code: String(error.status), title: '접근할 수 없는 페이지입니다', detail: '권한이 있는 계정으로 로그인해야 볼 수 있습니다.' }
        }
        return { code: String(error.status), title: '요청을 처리하지 못했습니다', detail: '잠시 후 다시 시도해 주세요.' }
    }
    return { code: 'ERROR', title: '화면을 그리는 중 문제가 생겼습니다', detail: '새로고침하거나 잠시 후 다시 시도해 주세요.' }
}
