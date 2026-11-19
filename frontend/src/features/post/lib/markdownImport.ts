/**
 * .md 파일 → 에디터 본문.
 *  - 앞머리(front matter: --- title: ... ---)는 본문에서 빼고 title 만 꺼냄
 *  - 앞머리가 없고 첫 줄이 "# 제목" 이면 그걸 제목으로 쓰고 본문에서 뺌 (게시글 제목과 중복 방지)
 *  - 로컬 경로 이미지(./img.png 등)는 블로그에서 열 수 없으므로 개수를 세서 알려줌
 */

export interface ImportedMarkdown {
    body: string
    title: string | null
    /** http(s)·data: 가 아닌 이미지 경로 — 다시 업로드가 필요한 것 */
    localImages: string[]
}

const FRONT_MATTER = /^---\r?\n([\s\S]*?)\r?\n---[ \t]*(?:\r?\n|$)/
const IMAGE = /!\[[^\]]*\]\(\s*<?([^)\s>]+)>?(?:\s+"[^"]*")?\s*\)/g

function unquote(v: string): string {
    const t = v.trim()
    if ((t.startsWith('"') && t.endsWith('"')) || (t.startsWith("'") && t.endsWith("'"))) {
        return t.slice(1, -1)
    }
    return t
}

export function importMarkdown(raw: string): ImportedMarkdown {
    let text = raw.replace(/^﻿/, '').replace(/\r\n/g, '\n')
    let title: string | null = null

    const fm = FRONT_MATTER.exec(text)
    if (fm) {
        const line = fm[1].split('\n').find((l) => /^title\s*:/i.test(l))
        if (line) title = unquote(line.replace(/^title\s*:/i, '')) || null
        text = text.slice(fm[0].length)
    }

    if (!title) {
        const h1 = /^\s*#\s+(.+?)\s*#*\s*(?:\n|$)/.exec(text)
        if (h1 && h1.index === 0) {
            title = h1[1]
            text = text.slice(h1[0].length)
        }
    }

    const localImages: string[] = []
    for (const m of text.matchAll(IMAGE)) {
        const src = m[1]
        if (!/^(https?:)?\/\//i.test(src) && !/^data:/i.test(src)) localImages.push(src)
    }

    return { body: text.replace(/^\n+/, ''), title, localImages }
}
