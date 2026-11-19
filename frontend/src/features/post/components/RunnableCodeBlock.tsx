import { useRef, useState, type ComponentProps } from 'react'
import { cn } from '@/shared/lib/cn'
import {
    runCode,
    toRunLang,
    RUN_LANG_LABEL,
    type RunLang,
    type RunPhase,
    type RunResult,
} from '@/features/post/lib/codeRunner'

/** react-markdown 이 넘겨주는 hast 노드 — 필요한 필드만 최소 정의 (hast 타입 의존 없이) */
interface HastNode {
    type: string
    tagName?: string
    value?: string
    properties?: { className?: unknown }
    children?: HastNode[]
}

function hastText(node: HastNode | undefined): string {
    if (!node) return ''
    if (node.type === 'text') return node.value ?? ''
    return (node.children ?? []).map(hastText).join('')
}

/** <pre><code class="language-xxx"> 에서 언어명과 원본 코드 추출 (rehype-highlight 가 span 으로 쪼갠 뒤에도 텍스트는 그대로) */
function readCode(pre: HastNode | undefined): { lang: string | undefined; code: string } {
    const codeEl = pre?.children?.find((c) => c.type === 'element' && c.tagName === 'code')
    const cls = codeEl?.properties?.className
    const classes = Array.isArray(cls) ? cls.map(String) : typeof cls === 'string' ? cls.split(' ') : []
    const lang = classes.find((c) => c.startsWith('language-'))?.slice('language-'.length)
    return { lang, code: hastText(codeEl).replace(/\n$/, '') }
}

type State =
    | { kind: 'idle' }
    | { kind: 'busy'; phase: RunPhase }
    | { kind: 'done'; result: RunResult }

/**
 * 마크다운 코드 블록 — Python · JavaScript 는 [실행] 버튼과 출력 패널이 붙음.
 * 그 외 언어는 기존 <pre> 그대로.
 */
export function RunnableCodeBlock({
    node,
    className,
    children,
    ...rest
}: ComponentProps<'pre'> & { node?: unknown }) {
    const { lang, code } = readCode(node as HastNode | undefined)
    const runLang = toRunLang(lang)
    const [state, setState] = useState<State>({ kind: 'idle' })
    const runId = useRef(0)

    const preClass = cn('rounded-lg overflow-x-auto', className)

    if (!runLang) {
        return (
            <pre className={cn('my-5', preClass)} {...rest}>
                {children}
            </pre>
        )
    }

    const busy = state.kind === 'busy'

    const onRun = async () => {
        const id = ++runId.current
        setState({ kind: 'busy', phase: 'running' })
        const result = await runCode(runLang, code, (phase) => {
            if (runId.current === id) setState({ kind: 'busy', phase })
        })
        if (runId.current === id) setState({ kind: 'done', result })
    }

    return (
        <div className="my-5">
            <div className="relative">
                <pre className={cn(preClass, state.kind !== 'idle' && 'rounded-b-none')} {...rest}>
                    {children}
                </pre>
                <button
                    type="button"
                    onClick={onRun}
                    disabled={busy}
                    aria-label={`${RUN_LANG_LABEL[runLang]} 코드 실행`}
                    className="absolute top-2 right-2 h-8 pl-2.5 pr-3 flex items-center gap-1.5 rounded-md bg-white/10 text-[13px] font-medium text-white/90 hover:bg-white/20 disabled:opacity-60 disabled:cursor-wait transition-colors"
                >
                    <PlayIcon />
                    {busy ? '실행 중' : state.kind === 'done' ? '다시 실행' : '실행'}
                </button>
            </div>

            {state.kind !== 'idle' && (
                <div className="rounded-b-lg border border-t-0 border-border bg-panel">
                    <div className="h-9 px-3.5 flex items-center justify-between border-b border-border text-xs text-ink-faint">
                        <span>
                            출력 · {RUN_LANG_LABEL[runLang]}
                            {state.kind === 'done' && !state.result.timedOut && !state.result.rejected && ` · ${formatMs(state.result.ms)}`}
                        </span>
                        {!busy && (
                            <button
                                type="button"
                                onClick={() => {
                                    runId.current++
                                    setState({ kind: 'idle' })
                                }}
                                className="h-7 px-2 -mr-2 rounded text-ink-faint hover:text-ink hover:bg-surface-muted"
                            >
                                닫기
                            </button>
                        )}
                    </div>
                    <Output state={state} lang={runLang} />
                </div>
            )}
        </div>
    )
}

const LOADING_TEXT: Record<RunLang, string> = {
    python: 'Python 런타임을 불러오는 중… (처음 한 번만 몇 초 걸립니다)',
    java: 'Java 런타임과 컴파일러(약 16MB)를 불러오는 중… (처음 한 번만 수십 초 걸릴 수 있습니다)',
    javascript: '실행 중…',
}

function Output({ state, lang }: { state: Exclude<State, { kind: 'idle' }>; lang: RunLang }) {
    const base = 'px-3.5 py-3 font-mono text-[13px] leading-relaxed whitespace-pre-wrap break-words'

    if (state.kind === 'busy') {
        return (
            <p className={cn(base, 'text-ink-faint')} aria-live="polite">
                {state.phase === 'loading' ? LOADING_TEXT[lang] : '실행 중…'}
            </p>
        )
    }

    const { lines } = state.result
    if (lines.length === 0) {
        return <p className={cn(base, 'text-ink-faint')}>(출력 없음)</p>
    }
    return (
        <div className={cn(base, 'max-h-80 overflow-y-auto')} aria-live="polite">
            {lines.map((l, i) => (
                <div key={i} className={l.stream === 'err' ? 'text-danger' : 'text-ink'}>
                    {l.text || ' ' /* 빈 줄도 높이 유지 */}
                </div>
            ))}
        </div>
    )
}

function formatMs(ms: number) {
    return ms < 1000 ? `${Math.max(1, Math.round(ms))}ms` : `${(ms / 1000).toFixed(2)}s`
}

function PlayIcon() {
    return (
        <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path d="M7 4.5v15a1 1 0 0 0 1.53.85l12-7.5a1 1 0 0 0 0-1.7l-12-7.5A1 1 0 0 0 7 4.5Z" />
        </svg>
    )
}
