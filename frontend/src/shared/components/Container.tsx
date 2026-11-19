import { type HTMLAttributes } from 'react'
import { cn } from '@/shared/lib/cn'

/**
 * 페이지 최대 폭 + 가운데 정렬.
 *
 * max-w-6xl (1152px) 결정:
 *  - 블로그 본문(주력 콘텐츠)은 700~800px이 가독성 최적
 *  - 본문 + 220px 사이드바 + gap = 1100px 근처
 *  - 5xl(1024)이면 사이드바 고려 시 본문이 좁고
 *  - 7xl(1280)은 마켓 대시보드 외엔 빈 공간이 너무 많음
 */
export function Container({
                              className,
                              ...rest
                          }: HTMLAttributes<HTMLDivElement>) {
    return (
        <div
            className={cn('mx-auto w-full max-w-6xl px-4 sm:px-6', className)}
            {...rest}
        />
    )
}