import { useEffect } from 'react'

/** 입력창·편집 영역은 막지 않음 (댓글, 검색 등) */
function isEditable(target: EventTarget | null) {
    if (!(target instanceof HTMLElement)) return false
    return !!target.closest('input, textarea, select, [contenteditable="true"]')
}

const BLOCKED_KEYS = new Set(['c', 'x', 'a', 's', 'p', 'u'])

/**
 * 게시글 복사·우클릭·드래그·인쇄 단축키 차단.
 * 개발자도구·소스 보기·스크린샷까지 막을 수는 없음 → 일반 사용자의 손쉬운 복사를 줄이는 용도.
 */
export function useCopyGuard(enabled = true) {
    useEffect(() => {
        if (!enabled) return

        const block = (e: Event) => {
            if (isEditable(e.target)) return
            e.preventDefault()
        }

        const onKeyDown = (e: KeyboardEvent) => {
            if (isEditable(e.target)) return
            const key = e.key.toLowerCase()
            const mod = e.ctrlKey || e.metaKey
            if ((mod && BLOCKED_KEYS.has(key)) || key === 'printscreen') {
                e.preventDefault()
                // PrintScreen은 막을 수 없고, 클립보드를 비우는 정도만 가능 (브라우저 권한에 따라 실패할 수 있음)
                if (key === 'printscreen') navigator.clipboard?.writeText('').catch(() => {})
            }
        }

        const events = ['copy', 'cut', 'contextmenu', 'dragstart', 'selectstart'] as const
        events.forEach((t) => document.addEventListener(t, block))
        document.addEventListener('keydown', onKeyDown)
        document.documentElement.classList.add('copy-guard')

        return () => {
            events.forEach((t) => document.removeEventListener(t, block))
            document.removeEventListener('keydown', onKeyDown)
            document.documentElement.classList.remove('copy-guard')
        }
    }, [enabled])
}
