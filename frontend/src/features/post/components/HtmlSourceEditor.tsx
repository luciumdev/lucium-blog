import { useRef, useState } from 'react'
import { cn } from '@/shared/lib/cn'
import { HtmlPostFrame } from '@/features/post/components/HtmlPostFrame'

interface HtmlSourceEditorProps {
    value: string
    onChange: (html: string) => void
    /** 파일의 <title> 을 찾으면 호출 (제목 칸이 비어 있을 때 채우는 용도) */
    onImportTitle?: (title: string) => void
}

/** src/href 가 상대 경로인 리소스 — 블로그에 올리면 깨지므로 알려줌 */
function findLocalRefs(html: string): string[] {
    const refs: string[] = []
    const re = /\b(?:src|href)\s*=\s*["']([^"'#]+)["']/gi
    for (const m of html.matchAll(re)) {
        const v = m[1].trim()
        if (!/^(?:[a-z][a-z0-9+.-]*:|\/\/)/i.test(v)) refs.push(v)
    }
    return [...new Set(refs)]
}

function titleOf(html: string): string | null {
    const m = /<title[^>]*>([\s\S]*?)<\/title>/i.exec(html)
    const t = m?.[1].replace(/\s+/g, ' ').trim()
    return t ? t : null
}

/**
 * HTML 원본 글 편집 — 파일 불러오기 + 원본 소스 + 미리보기.
 * 미리보기는 실제 게시글과 같은 격리 iframe(HtmlPostFrame) 이라 스크립트도 그대로 동작함.
 */
export function HtmlSourceEditor({ value, onChange, onImportTitle }: HtmlSourceEditorProps) {
    const inputRef = useRef<HTMLInputElement>(null)
    const [tab, setTab] = useState<'source' | 'preview'>(value.trim() ? 'preview' : 'source')
    const [notice, setNotice] = useState<string | null>(null)

    const onFile = async (file: File) => {
        const html = (await file.text()).replace(/^﻿/, '')
        if (value.trim() && !window.confirm('지금 HTML 을 불러온 파일로 바꿀까요?')) return

        onChange(html)
        const title = titleOf(html)
        if (title) onImportTitle?.(title)

        const local = findLocalRefs(html)
        setNotice(
            local.length > 0
                ? `상대 경로 파일 ${local.length}개(${local.slice(0, 3).join(', ')}${local.length > 3 ? ' …' : ''})는 블로그에서 열리지 않습니다. CSS·JS 는 파일 안에 넣고, 이미지는 업로드한 주소(https://…)로 바꿔 주세요.`
                : `${file.name} 을(를) 불러왔습니다.`,
        )
        setTab('preview')
    }

    const tabCls = (active: boolean) =>
        cn(
            'h-9 px-3.5 rounded-md text-sm transition-colors',
            active ? 'bg-panel text-ink font-medium shadow-xs' : 'text-ink-muted hover:text-ink',
        )

    return (
        <div className="border border-border rounded-lg overflow-hidden bg-surface">
            <input
                ref={inputRef}
                type="file"
                accept=".html,.htm,text/html"
                hidden
                onChange={(e) => {
                    const f = e.target.files?.[0]
                    e.target.value = ''
                    if (f) void onFile(f)
                }}
            />

            <div className="flex flex-wrap items-center justify-between gap-2 px-2.5 py-2 border-b border-border bg-surface-muted">
                <div role="tablist" className="flex gap-1">
                    <button type="button" role="tab" aria-selected={tab === 'source'} className={tabCls(tab === 'source')} onClick={() => setTab('source')}>
                        소스
                    </button>
                    <button type="button" role="tab" aria-selected={tab === 'preview'} className={tabCls(tab === 'preview')} onClick={() => setTab('preview')}>
                        미리보기
                    </button>
                </div>
                <button
                    type="button"
                    onClick={() => inputRef.current?.click()}
                    className="h-9 px-3.5 rounded-md bg-primary-tint text-sm font-medium text-primary-hover hover:bg-primary-subtle transition-colors"
                >
                    HTML 파일 불러오기
                </button>
            </div>

            {notice && (
                <div role="status" className="flex items-start justify-between gap-3 px-4 py-2.5 text-sm border-b border-border bg-primary-subtle text-primary-hover">
                    <span className="min-w-0 break-words">{notice}</span>
                    <button type="button" onClick={() => setNotice(null)} className="shrink-0 text-xs underline underline-offset-2">
                        닫기
                    </button>
                </div>
            )}

            {tab === 'source' ? (
                <textarea
                    value={value}
                    onChange={(e) => onChange(e.target.value)}
                    spellCheck={false}
                    placeholder="<!doctype html> … 파일을 불러오거나 직접 붙여 넣으세요"
                    className="block w-full min-h-[500px] p-4 bg-panel font-mono text-[13px] leading-relaxed text-ink resize-y outline-none [caret-color:auto]"
                />
            ) : value.trim() ? (
                <div className="p-3 bg-surface-muted">
                    <HtmlPostFrame html={value} title="HTML 미리보기" />
                </div>
            ) : (
                <p className="px-4 py-16 text-center text-sm text-ink-faint">불러온 HTML 이 없습니다.</p>
            )}
        </div>
    )
}
