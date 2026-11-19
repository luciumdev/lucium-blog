import { useEffect, useId, useMemo, useRef, useState } from 'react'
import { buildHtmlSrcDoc, HEIGHT_MESSAGE } from '@/features/post/lib/htmlSrcDoc'

const MIN_HEIGHT = 120
const MAX_HEIGHT = 40_000

/**
 * HTML 원본 글 렌더러 — 격리된 iframe.
 *
 *  - sandbox: allow-scripts (인터랙티브 노트용) + 새 탭 링크 허용.
 *    allow-same-origin 은 절대 넣지 않음 — 넣으면 글 안 스크립트가 블로그 쿠키·API 에 접근 가능해짐.
 *  - 높이: 글 안에 주입한 스크립트가 postMessage 로 보내는 값으로 맞춤.
 *    event.source 가 이 iframe 일 때만 받음 (다른 iframe·창의 메시지 무시).
 */
export function HtmlPostFrame({ html, title }: { html: string; title: string }) {
    const frameId = useId()
    const ref = useRef<HTMLIFrameElement>(null)
    const [height, setHeight] = useState(600)

    const srcDoc = useMemo(() => buildHtmlSrcDoc(html, frameId), [html, frameId])

    useEffect(() => {
        const onMessage = (e: MessageEvent) => {
            if (e.source !== ref.current?.contentWindow) return
            const data = e.data as { type?: unknown; id?: unknown; height?: unknown } | null
            if (!data || data.type !== HEIGHT_MESSAGE || data.id !== frameId) return
            if (typeof data.height !== 'number' || !Number.isFinite(data.height)) return
            setHeight(Math.min(MAX_HEIGHT, Math.max(MIN_HEIGHT, data.height)))
        }
        window.addEventListener('message', onMessage)
        return () => window.removeEventListener('message', onMessage)
    }, [frameId])

    return (
        <iframe
            ref={ref}
            title={title}
            srcDoc={srcDoc}
            sandbox="allow-scripts allow-popups allow-popups-to-escape-sandbox"
            referrerPolicy="no-referrer"
            style={{ height }}
            className="block w-full border-0 bg-white rounded-[10px]"
        />
    )
}
