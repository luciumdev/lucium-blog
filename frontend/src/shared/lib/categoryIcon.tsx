import type { SVGProps } from 'react'

const baseProps: SVGProps<SVGSVGElement> = {
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 2,
    strokeLinecap: 'round',
    strokeLinejoin: 'round',
    'aria-hidden': true,
}

/**
 * 카테고리 slug → SVG 아이콘 매핑.
 *
 * 디자인 결정:
 *  - 24x24 viewBox + stroke=2 + currentColor (Lucide 톤)
 *  - 외부 라이브러리 안 씀 (의존성 최소 + 콘솔 톤 통일)
 *  - currentColor라 부모의 text-* 컬러 자동 상속
 *  - 분기 키는 slug(영문, 불변) — 카테고리명이 바뀌어도 아이콘 유지
 *  - 모르는 slug는 hash 아이콘으로 fallback
 *
 * 대분류 4종: dev / infra / project / log
 * 추가 카테고리는 case 한 줄만 더하면 됨.
 */
export function CategoryIcon({
                                 slug,
                                 className = 'w-3 h-3',
                             }: {
    slug: string
    className?: string
}) {
    const props = { ...baseProps, className }

    switch (slug.trim().toLowerCase()) {
        case 'dev':
            // 코드 괄호 </>
            return (
                <svg {...props}>
                    <polyline points="16 18 22 12 16 6" />
                    <polyline points="8 6 2 12 8 18" />
                </svg>
            )
        case 'infra':
            // 서버 스택 (랙)
            return (
                <svg {...props}>
                    <rect x="2" y="2" width="20" height="8" rx="2" />
                    <rect x="2" y="14" width="20" height="8" rx="2" />
                    <line x1="6" y1="6" x2="6.01" y2="6" />
                    <line x1="6" y1="18" x2="6.01" y2="18" />
                </svg>
            )
        case 'project':
            // 폴더
            return (
                <svg {...props}>
                    <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
                </svg>
            )
        case 'log':
            // 로그 라인 (리스트)
            return (
                <svg {...props}>
                    <line x1="8" y1="6" x2="21" y2="6" />
                    <line x1="8" y1="12" x2="21" y2="12" />
                    <line x1="8" y1="18" x2="21" y2="18" />
                    <line x1="3" y1="6" x2="3.01" y2="6" />
                    <line x1="3" y1="12" x2="3.01" y2="12" />
                    <line x1="3" y1="18" x2="3.01" y2="18" />
                </svg>
            )
        default:
            // hash 아이콘 — 모르는 slug fallback
            return (
                <svg {...props}>
                    <line x1="4" y1="9" x2="20" y2="9" />
                    <line x1="4" y1="15" x2="20" y2="15" />
                    <line x1="10" y1="3" x2="8" y2="21" />
                    <line x1="16" y1="3" x2="14" y2="21" />
                </svg>
            )
    }
}
