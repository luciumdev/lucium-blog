import { ErrorView } from '@/shared/components/ErrorView'

/** path='*' — PublicLayout 안에서 렌더되므로 헤더·사이드바는 그대로 보임 */
export default function NotFoundPage() {
    return (
        <ErrorView
            code="404"
            title="페이지를 찾을 수 없습니다"
            detail="주소가 바뀌었거나 삭제된 페이지일 수 있습니다."
        />
    )
}
