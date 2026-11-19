import { useEffect, useState } from 'react'
import { cn } from '@/shared/lib/cn'
import type { TocItem } from '../lib/toc'

interface Props {
    items: TocItem[]
}

function TocLink({ item, active }: { item: TocItem; active: boolean }) {
    const cls = cn(
        '-ml-px block border-l-2 py-1 pr-2 font-mono text-xs leading-snug transition-colors',
        active
            ? 'border-primary text-primary'
            : 'border-transparent text-ink-faint hover:text-ink-muted',
    )
    const style = { paddingLeft: (item.level - 2) * 12 + 12 }

    return <a href={`#${item.id}`} style={style} className={cls}>{item.text}</a>
}

export function TableOfContents({ items }: Props) {
    const [activeId, setActiveId] = useState('')

    useEffect(() => {
        if (items.length === 0) return

        // IntersectionObserver는 교차 '변화' 시에만 발화한다.
        // 긴 섹션에서 관찰 밴드에 heading이 없으면 콜백이 오지 않아 하이라이트가 멈춘다.
        // → 스크롤마다 "뷰포트 상단을 지나친 마지막 heading"을 직접 계산한다.
        const OFFSET = 100 // 상단 기준선

        const compute = () => {
            const nodes = items
                .map((i) => ({ id: i.id, el: document.getElementById(i.id) }))
                .filter((n): n is { id: string; el: HTMLElement } => n.el !== null)

            if (nodes.length === 0) return

            // 문서 하단 도달 시 마지막 항목
            if (window.innerHeight + window.scrollY >= document.body.scrollHeight - 2) {
                setActiveId(nodes[nodes.length - 1].id)
                return
            }

            let current = nodes[0].id
            for (const n of nodes) {
                if (n.el.getBoundingClientRect().top <= OFFSET) current = n.id
                else break
            }
            setActiveId(current)
        }

        let ticking = false
        const onScroll = () => {
            if (ticking) return
            ticking = true
            requestAnimationFrame(() => {
                compute()
                ticking = false
            })
        }

        compute() // 초기 1회
        window.addEventListener('scroll', onScroll, { passive: true })
        window.addEventListener('resize', onScroll)
        return () => {
            window.removeEventListener('scroll', onScroll)
            window.removeEventListener('resize', onScroll)
        }
    }, [items])

    if (items.length < 2) return null

    return (
        <nav aria-label="목차" className="sticky top-8 max-h-[calc(100vh-6rem)] overflow-y-auto">
            <p className="mb-3 px-3 font-mono text-[10px] font-semibold uppercase tracking-[0.2em] text-ink-muted">
                contents
            </p>

            <ul className="border-l border-border">
                {items.map((item) => (
                    <li key={item.id}>
                        <TocLink item={item} active={activeId === item.id} />
                    </li>
                ))}
            </ul>
        </nav>
    )
}