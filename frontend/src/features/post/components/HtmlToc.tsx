import { useEffect, useState, type RefObject } from 'react'

/** HTML 원본 글(iframe)이 postMessage로 보내는 목차 항목. level 1 = 대주제, 2 = 소주제 / text에 번호 포함 / top = iframe 문서 기준 y좌표 */
export type HtmlTocItem = { id: string; level?: number; text: string; top: number }

type WrapRef = RefObject<HTMLElement | null>

const SCROLL_OFFSET = 32 // 마크다운 heading의 scroll-mt-8과 동일
const ACTIVE_LINE = 140 // 화면 위에서 이 높이를 지난 섹션을 현재 섹션으로 간주

function findFrame(wrapRef: WrapRef) {
    return wrapRef.current?.querySelector('iframe') ?? null
}

/** iframe 문서 기준 y좌표로 부모 창을 스크롤 (iframe은 높이가 늘어나 있어 자체 스크롤이 없음) */
function scrollToFrameY(frame: HTMLIFrameElement, y: number) {
    const top = frame.getBoundingClientRect().top + window.scrollY + y - SCROLL_OFFSET
    window.scrollTo({ top, behavior: 'smooth' })
}

/** iframe 메시지 수신: note-toc(목차 목록), note-scroll-to(노트 내부 앵커 클릭) */
export function useHtmlToc(wrapRef: WrapRef) {
    const [items, setItems] = useState<HtmlTocItem[]>([])

    useEffect(() => {
        const onMessage = (e: MessageEvent) => {
            const frame = findFrame(wrapRef)
            if (!frame || e.source !== frame.contentWindow) return
            const d = e.data
            if (d?.type === 'note-toc' && Array.isArray(d.items)) {
                setItems(d.items as HtmlTocItem[])
            } else if (d?.type === 'note-scroll-to' && typeof d.top === 'number') {
                scrollToFrameY(frame, d.top)
            }
        }
        window.addEventListener('message', onMessage)
        return () => window.removeEventListener('message', onMessage)
    }, [wrapRef])

    return items
}

function useActiveIndex(items: HtmlTocItem[], wrapRef: WrapRef) {
    const [active, setActive] = useState(0)

    useEffect(() => {
        if (items.length === 0) return
        let raf = 0
        const update = () => {
            raf = 0
            const frame = findFrame(wrapRef)
            if (!frame) return
            const base = frame.getBoundingClientRect().top
            let k = 0
            items.forEach((it, i) => {
                if (base + it.top < ACTIVE_LINE) k = i
            })
            setActive(k)
        }
        const onScroll = () => {
            if (!raf) raf = requestAnimationFrame(update)
        }
        update()
        window.addEventListener('scroll', onScroll, { passive: true })
        window.addEventListener('resize', onScroll)
        return () => {
            window.removeEventListener('scroll', onScroll)
            window.removeEventListener('resize', onScroll)
            if (raf) cancelAnimationFrame(raf)
        }
    }, [items, wrapRef])

    return active
}

/** xl 이상 — 오른쪽 sticky CONTENTS (마크다운 글의 TableOfContents와 같은 위치) */
export function HtmlToc({ items, wrapRef }: { items: HtmlTocItem[]; wrapRef: WrapRef }) {
    const active = useActiveIndex(items, wrapRef)
    if (items.length < 2) return null

    return (
        <nav className="sticky top-8" aria-label="목차">
            <p className="mb-3 font-mono text-xs tracking-[0.15em] text-ink-faint">CONTENTS</p>
            <ul className="border-l border-border">
                {items.map((it, i) => (
                    <li key={it.id}>
                        <button
                            type="button"
                            onClick={() => {
                                const frame = findFrame(wrapRef)
                                if (frame) scrollToFrameY(frame, it.top)
                            }}
                            className={`-ml-px block w-full border-l-2 py-1.5 text-left font-mono text-xs transition-colors ${
                                it.level === 2 ? 'pl-6' : 'pl-3'
                            } ${
                                i === active
                                    ? 'border-primary text-primary'
                                    : 'border-transparent text-ink-faint hover:text-primary'
                            }`}
                        >
                            {it.text}
                        </button>
                    </li>
                ))}
            </ul>
        </nav>
    )
}

/** xl 미만 — 본문 상단 접이식 목차 (마크다운 글의 MobileToc와 같은 모양) */
export function HtmlMobileToc({ items, wrapRef }: { items: HtmlTocItem[]; wrapRef: WrapRef }) {
    if (items.length < 2) return null

    return (
        <details className="xl:hidden mb-6 rounded-md border border-border bg-panel">
            <summary className="cursor-pointer select-none px-4 py-2.5 font-mono text-xs text-ink-muted">
                contents ({items.length})
            </summary>
            <ul className="border-t border-border px-4 py-3">
                {items.map((it) => (
                    <li key={it.id}>
                        <button
                            type="button"
                            onClick={() => {
                                const frame = findFrame(wrapRef)
                                if (frame) scrollToFrameY(frame, it.top)
                            }}
                            className={`block w-full py-1 text-left font-mono text-xs text-ink-faint hover:text-primary transition-colors ${
                                it.level === 2 ? 'pl-3' : ''
                            }`}
                        >
                            {it.text}
                        </button>
                    </li>
                ))}
            </ul>
        </details>
    )
}
