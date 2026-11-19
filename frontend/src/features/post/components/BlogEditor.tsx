import {
    MDXEditor,
    headingsPlugin,
    listsPlugin,
    quotePlugin,
    thematicBreakPlugin,
    markdownShortcutPlugin,
    linkPlugin,
    linkDialogPlugin,
    imagePlugin,
    codeBlockPlugin,
    codeMirrorPlugin,
    tablePlugin,
    toolbarPlugin,
    UndoRedo,
    BoldItalicUnderlineToggles,
    BlockTypeSelect,
    CreateLink,
    InsertCodeBlock,
    InsertTable,
    ListsToggle,
    Separator,
    ButtonWithTooltip,
    type MDXEditorMethods,
} from '@mdxeditor/editor'
import '@mdxeditor/editor/style.css'
import { uploadFileToS3 } from '@/features/files/api'
import { importMarkdown, type ImportedMarkdown } from '@/features/post/lib/markdownImport'
import { useEffect, useRef, useState } from 'react'

interface BlogEditorProps {
    /** 초기 마크다운 — MDXEditor는 mount 시점에만 읽음. 이후 변경은 무시. */
    initialMarkdown: string
    /** 모든 키스트로크에 호출됨. 외부 form 상태와 동기화 용도. */
    onChange: (markdown: string) => void
    /** .md 파일을 불러올 때 파일에서 제목을 찾으면 호출 (제목 칸이 비어 있을 때 채우는 용도) */
    onImportTitle?: (title: string) => void
    placeholder?: string
}

/**
 * MDX 본문 에디터.
 *
 * 주의:
 *  - MDXEditor는 `markdown` prop을 mount 시점에만 읽음. 비동기 로드된 내용을
 *    반영하려면 호출 측에서 데이터 준비 후 mount하거나 key로 강제 remount 필요.
 *    PostEditorPage는 form ready 게이트로 처리.
 *  - 툴바(plugins)는 mount 때 한 번만 읽힘 → 툴바 버튼 핸들러는 ref 만 참조해야 최신 값이 반영됨.
 *
 * 이미지 넣기 (3가지 모두 S3 업로드 후 커서/놓은 위치에 삽입)
 *  1. 툴바 [이미지] 버튼 — 커서를 둔 자리에 바로 들어감.
 *     기본 InsertImage 는 URL 입력 다이얼로그를 거치는데, 다이얼로그 입력칸으로 포커스가 옮겨가면
 *     에디터 커서 위치를 잃고 본문 맨 앞/끝에 들어가는 문제가 있어 파일 선택으로 직접 대체.
 *  2. 이미지 파일을 원하는 위치로 드래그 & 드롭
 *  3. 클립보드 이미지 붙여넣기 (Ctrl+V)
 *
 * .md 불러오기 — 툴바 [MD] 버튼. 본문이 있으면 '교체' 또는 '커서 위치에 끼워 넣기' 선택.
 */
const CODE_LANGUAGES = {
    '': 'Plain text',
    js: 'JavaScript',
    ts: 'TypeScript',
    jsx: 'JSX',
    tsx: 'TSX',
    html: 'HTML',
    css: 'CSS',
    java: 'Java',
    c: 'C',
    python: 'Python',
    sql: 'SQL',
    bash: 'Bash',
    json: 'JSON',
    yaml: 'YAML',
}
/** Lexical 업데이트가 커밋된 뒤 읽기 위한 대기 */
const nextTick = () => new Promise<void>((resolve) => setTimeout(resolve, 0))

/** 파일명 → 대체 텍스트 (확장자 제거, 대괄호는 마크다운 문법과 충돌하므로 제거) */
function altFromFileName(name: string): string {
    return name.replace(/\.[^.]+$/, '').replace(/[[\]]/g, '').trim() || 'image'
}

export function BlogEditor({ initialMarkdown, onChange, onImportTitle, placeholder }: BlogEditorProps) {
    const editorRef = useRef<MDXEditorMethods>(null)
    const imageInputRef = useRef<HTMLInputElement>(null)
    const mdInputRef = useRef<HTMLInputElement>(null)

    // 툴바 클로저가 최신 콜백을 보도록 ref 로 보관
    const onChangeRef = useRef(onChange)
    const parseErrorRef = useRef<string | null>(null)

    const onImportTitleRef = useRef(onImportTitle)
    useEffect(() => {
        onChangeRef.current = onChange
        onImportTitleRef.current = onImportTitle
    }, [onChange, onImportTitle])

    const [imageProgress, setImageProgress] = useState<number | null>(null)
    const [notice, setNotice] = useState<{ tone: 'info' | 'error'; text: string } | null>(null)
    const imageUploading = imageProgress !== null

    const uploadImage = async (file: File): Promise<string> => {
        setImageProgress(0)
        try {
            return await uploadFileToS3(file, { onProgress: setImageProgress })
        } finally {
            setImageProgress(null)
        }
    }

    /** 툴바 [이미지] — 여러 장 선택 시 고른 순서대로 커서 위치에 연달아 삽입 */
    const onImagesPicked = async (files: File[]) => {
        setNotice(null)
        for (const file of files) {
            if (!file.type.startsWith('image/')) {
                setNotice({ tone: 'error', text: `이미지 파일이 아닙니다: ${file.name}` })
                continue
            }
            try {
                const url = await uploadImage(file)
                editorRef.current?.insertMarkdown(`![${altFromFileName(file.name)}](${url})`)
            } catch (e) {
                setNotice({ tone: 'error', text: `업로드 실패 (${file.name}): ${e instanceof Error ? e.message : String(e)}` })
                return
            }
        }
    }

    /** 툴바 [MD] — .md 파일 불러오기 */
    const onMarkdownPicked = async (file: File) => {
        setNotice(null)
        const editor = editorRef.current
        if (!editor) return

        let imported: ImportedMarkdown
        try {
            imported = importMarkdown(await file.text())
        } catch {
            setNotice({ tone: 'error', text: '파일을 읽지 못했습니다.' })
            return
        }

        if (imported.title) onImportTitleRef.current?.(imported.title)

        if (!imported.body.trim()) {
            setNotice({ tone: 'error', text: '불러온 파일에 제목 외 본문이 없습니다.' })
            return
        }

        const before = editor.getMarkdown()
        const hasContent = before.trim().length > 0
        const replace = !hasContent || window.confirm(
            '지금 본문을 불러온 파일 내용으로 바꿀까요?\n\n[확인] 본문 교체\n[취소] 커서 위치에 끼워 넣기',
        )

        parseErrorRef.current = null
        try {
            if (replace) editor.setMarkdown(imported.body)
            else editor.insertMarkdown(imported.body)
        } catch (e) {
            parseErrorRef.current = e instanceof Error ? e.message : String(e)
        }

        await nextTick() // 커밋 후에 읽어야 반영된 값이 나옴
        const after = editor.getMarkdown()

        const failed =
            parseErrorRef.current !== null ||
            after.trim() === '' ||
            (!replace && after === before)

        if (failed) {
            // 교체 모드에서 파싱 실패 시 본문이 비워지므로 원래 내용 복구
            editor.setMarkdown(before)
            onChangeRef.current(before)
            setNotice({
                tone: 'error',
                text: `불러오지 못했습니다: ${parseErrorRef.current ?? '에디터에 반영되지 않았습니다.'} (HTML 태그나 { } 문법이 있는지 확인해 주세요)`,
            })
            return
        }

        onChangeRef.current(after)

        if (imported.localImages.length > 0) {
            setNotice({
                tone: 'info',
                text: `내 컴퓨터 경로 이미지 ${imported.localImages.length}개는 블로그에서 보이지 않습니다. 해당 위치에 커서를 두고 [이미지]로 다시 넣어 주세요. (${imported.localImages.slice(0, 3).join(', ')}${imported.localImages.length > 3 ? ' …' : ''})`,
            })
        } else {
            setNotice({ tone: 'info', text: `${file.name} 을(를) 불러왔습니다.` })
        }
    }

    return (
        <div className="relative border border-border rounded-lg overflow-hidden bg-surface">
            {/* 툴바 버튼이 여는 숨은 파일 입력 */}
            <input
                ref={imageInputRef}
                type="file"
                accept="image/*"
                multiple
                hidden
                onChange={(e) => {
                    const files = Array.from(e.target.files ?? [])
                    e.target.value = ''
                    if (files.length) void onImagesPicked(files)
                }}
            />
            <input
                ref={mdInputRef}
                type="file"
                accept=".md,.markdown,text/markdown,text/plain"
                hidden
                onChange={(e) => {
                    const file = e.target.files?.[0]
                    e.target.value = ''
                    if (file) void onMarkdownPicked(file)
                }}
            />

            {imageUploading && (
                <div
                    role="progressbar"
                    aria-valuenow={imageProgress ?? 0}
                    aria-valuemin={0}
                    aria-valuemax={100}
                    aria-label="이미지 업로드 진행률"
                    className="absolute top-2 right-2 z-20 flex items-center gap-2 px-3 py-1.5 rounded-md bg-panel border border-border shadow-md font-mono text-xs text-ink">
                    <span>이미지 업로드</span>
                    <div className="w-20 h-1 rounded-full bg-surface-muted overflow-hidden">
                        <div
                            className="h-full bg-primary transition-all duration-150"
                            style={{ width: `${imageProgress}%` }}
                        />
                    </div>
                    <span className="tabular-nums w-8 text-right">{imageProgress}%</span>
                </div>
            )}

            {notice && (
                <div
                    role={notice.tone === 'error' ? 'alert' : 'status'}
                    className={`flex items-start justify-between gap-3 px-4 py-2.5 text-sm border-b border-border ${
                        notice.tone === 'error' ? 'bg-danger-tint text-danger' : 'bg-primary-subtle text-primary-hover'
                    }`}
                >
                    <span className="min-w-0 break-words">{notice.text}</span>
                    <button type="button" onClick={() => setNotice(null)} className="shrink-0 text-xs underline underline-offset-2">
                        닫기
                    </button>
                </div>
            )}

            <MDXEditor
                ref={editorRef}
                markdown={initialMarkdown}
                onChange={onChange}
                onError={({ error }) => {
                    parseErrorRef.current = error
                    setNotice({ tone: 'error', text: `마크다운 해석 오류: ${error}` })
                }}
                placeholder={placeholder ?? '본문을 입력하세요...'}
                contentEditableClassName="min-h-[500px] outline-none px-4 py-3 text-ink"
                plugins={[
                    headingsPlugin(),
                    listsPlugin(),
                    quotePlugin(),
                    thematicBreakPlugin(),
                    markdownShortcutPlugin(),
                    linkPlugin(),
                    linkDialogPlugin(),
                    // 드래그 & 드롭 · 붙여넣기 이미지도 이 핸들러로 S3 업로드됨
                    imagePlugin({ imageUploadHandler: uploadImage }),
                    codeBlockPlugin({ defaultCodeBlockLanguage: 'java' }),
                    codeMirrorPlugin({ codeBlockLanguages: CODE_LANGUAGES }),
                    tablePlugin(),
                    toolbarPlugin({
                        toolbarContents: () => (
                            <>
                                <UndoRedo />
                                <Separator />
                                <BoldItalicUnderlineToggles />
                                <Separator />
                                <BlockTypeSelect />
                                <ListsToggle />
                                <Separator />
                                <CreateLink />
                                <ButtonWithTooltip
                                    title="이미지 (커서 위치에 삽입 · 드래그/붙여넣기도 가능)"
                                    onClick={() => imageInputRef.current?.click()}
                                >
                                    <ImageIcon />
                                </ButtonWithTooltip>
                                <Separator />
                                <InsertCodeBlock />
                                <InsertTable />
                                <Separator />
                                <ButtonWithTooltip
                                    title=".md 파일 불러오기"
                                    onClick={() => mdInputRef.current?.click()}
                                >
                                    <span className="px-1 text-xs font-semibold">MD</span>
                                </ButtonWithTooltip>
                            </>
                        ),
                    }),
                ]}
            />
        </div>
    )
}

function ImageIcon() {
    return (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <rect x="3" y="4" width="18" height="16" rx="2" />
            <circle cx="8.5" cy="9.5" r="1.5" />
            <path d="m21 16-5-5-9 9" />
        </svg>
    )
}
