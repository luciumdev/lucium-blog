import type { OutputLine, RunPhase, RunResult } from './codeRunner'

/**
 * Java 실행 — CheerpJ (WebAssembly JVM, 방문자 브라우저에서 실행).
 *   1. 소스를 가상 파일시스템 /str/ 에 씀
 *   2. javac(com.sun.tools.javac.Main) 로 /files/runN/ 에 컴파일
 *   3. 컴파일된 main 클래스를 실행
 *
 * 프로필 (빌드 환경변수 VITE_JAVA_RUNNER, 기본 17)
 *  - 17: CheerpJ Java 17 런타임 + JDK 17 javac(javac17.jar) + java.base 만 담은 jlink 이미지(jdk17/)를 --system 으로 지정.
 *        record · var · 텍스트 블록 · switch 식 · sealed · instanceof 패턴 가능. Java 21 전용 문법(switch/record 패턴 등)은 불가.
 *        (CheerpJ 가 Java 21 런타임을 아직 지원하지 않음)
 *  - 8 : JavaFiddle 과 같은 검증된 조합 (Java 8 런타임 + tools.jar). 17 이 동작하지 않을 때 되돌리는 용도.
 *  자산은 Dockerfile.web 이 빌드 때 public/cheerpj/ 에 생성 (로컬은 scripts/build-java-runner-assets.sh).
 *
 * 제약
 *  - CheerpJ 는 프로그램 출력을 페이지의 #console 요소에 씀 (고정 동작) → 숨은 <pre id="console"> 하나를 두고 읽어 옴.
 *    그래서 Java 는 페이지 전체에서 동시에 하나만 실행 (runGuard 가 보장).
 *  - 메인 스레드 JVM 이라 무한 루프를 강제로 멈출 수 없음 → 제한 시간 넘으면 결과만 '시간 초과'로 돌려주고,
 *    이후 Java 실행은 새로고침 전까지 막음 (멈추지 않은 JVM 위에 또 돌리지 않도록).
 *  - Scanner(System.in) 입력은 지원하지 않음.
 *
 * 라이선스: CheerpJ Community Edition — 개인 프로젝트 무료, 런타임은 CheerpJ CDN 에서 로드해야 함(자체 호스팅 불가).
 */

const CHEERPJ_LOADER = 'https://cjrtnc.leaningtech.com/4.3/loader.js'

interface JavaProfile {
    /** cheerpjInit({ version }) */
    runtime: 8 | 17
    /** javac 가 들어 있는 jar (CheerpJ 의 /app/ = 이 사이트 루트) */
    javacClassPath: string
    /** javac 공통 옵션 */
    javacArgs: string[]
}

const PROFILES: Record<'8' | '17', JavaProfile> = {
    '17': {
        runtime: 17,
        javacClassPath: '/app/cheerpj/javac17.jar',
        // 플랫폼 클래스(java.lang 등)는 jlink 로 만든 java.base 이미지에서 읽음
        javacArgs: ['--system', '/app/cheerpj/jdk17', '-source', '17', '-target', '17', '-proc:none', '-implicit:none'],
    },
    '8': {
        runtime: 8,
        javacClassPath: '/app/cheerpj/tools.jar',
        javacArgs: ['-proc:none', '-implicit:none'],
    },
}

const PROFILE: JavaProfile = import.meta.env.VITE_JAVA_RUNNER === '8' ? PROFILES['8'] : PROFILES['17']

/** UI 표시용 — 'Java 17' 등 */
export const JAVA_RUNTIME_LABEL = `Java ${PROFILE.runtime}`
const JAVA_TIMEOUT_MS = 15_000
const LOAD_TIMEOUT_MS = 90_000

declare global {
    interface Window {
        cheerpjInit?: (options?: object) => Promise<void>
        cheerpjRunMain?: (className: string, classPath: string, ...args: string[]) => Promise<number>
        cheerpOSAddStringFile?: (path: string, data: string | Uint8Array) => void
        /** 구버전 이름 (JavaFiddle 이 사용) */
        cheerpjAddStringFile?: (path: string, data: string | Uint8Array) => void
    }
}

let ready: Promise<void> | null = null
let stuck = false
let runSeq = 0

function withTimeout<T>(p: Promise<T>, ms: number, message: string): Promise<T> {
    return new Promise<T>((resolve, reject) => {
        const t = setTimeout(() => reject(new Error(message)), ms)
        p.then(
            (v) => {
                clearTimeout(t)
                resolve(v)
            },
            (e) => {
                clearTimeout(t)
                reject(e)
            },
        )
    })
}

function loadScript(src: string): Promise<void> {
    return new Promise((resolve, reject) => {
        const s = document.createElement('script')
        s.src = src
        s.async = true
        s.onload = () => resolve()
        s.onerror = () => reject(new Error('CheerpJ 스크립트를 불러오지 못했습니다.'))
        document.head.appendChild(s)
    })
}

/** CheerpJ 가 출력하는 #console — 화면에는 숨김 */
function consoleEl(): HTMLElement {
    let el = document.getElementById('console')
    if (!el) {
        el = document.createElement('pre')
        el.id = 'console'
        el.setAttribute('aria-hidden', 'true')
        el.style.display = 'none'
        document.body.appendChild(el)
    }
    return el
}

function loadCheerpj(): Promise<void> {
    if (ready) return ready
    ready = withTimeout(
        (async () => {
            consoleEl()
            if (!window.cheerpjInit) await loadScript(CHEERPJ_LOADER)
            if (!window.cheerpjInit) throw new Error('CheerpJ 초기화 함수를 찾지 못했습니다.')
            await window.cheerpjInit({ status: 'none', version: PROFILE.runtime })
        })(),
        LOAD_TIMEOUT_MS,
        'Java 런타임을 불러오는 데 너무 오래 걸립니다. 네트워크를 확인해 주세요.',
    ).catch((e) => {
        ready = null   // 실패하면 다음 클릭 때 다시 시도
        throw e
    })
    return ready
}

/** public class 이름 (파일명이 이것과 같아야 javac 가 통과) + package → 실행할 FQCN */
function findMainClass(code: string): { fileName: string; fqcn: string } | null {
    const noComments = code.replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/.*$/gm, '')
    const publicCls = /\bpublic\s+(?:final\s+|abstract\s+)*class\s+([A-Za-z_$][\w$]*)/.exec(noComments)
    const anyCls = /\bclass\s+([A-Za-z_$][\w$]*)/.exec(noComments)
    const name = publicCls?.[1] ?? anyCls?.[1]
    if (!name) return null
    const pkg = /^\s*package\s+([\w.]+)\s*;/m.exec(noComments)?.[1]
    return { fileName: `${name}.java`, fqcn: pkg ? `${pkg}.${name}` : name }
}

function toLines(text: string, stream: OutputLine['stream']): OutputLine[] {
    const t = text.replace(/\r\n/g, '\n').replace(/\n+$/, '')
    return t ? t.split('\n').map((line) => ({ stream, text: line })) : []
}

export async function runJava(code: string, onPhase?: (p: RunPhase) => void): Promise<RunResult> {
    const fail = (text: string): RunResult => ({ lines: [{ stream: 'err', text }], ok: false, timedOut: false, ms: 0 })

    if (stuck) {
        return fail('이전 Java 코드가 아직 끝나지 않았습니다. 페이지를 새로고침한 뒤 다시 실행해 주세요.')
    }
    const main = findMainClass(code)
    if (!main) return fail('class 선언을 찾지 못했습니다. main 메서드가 있는 클래스가 필요합니다.')

    if (!ready) onPhase?.('loading')
    try {
        await loadCheerpj()
    } catch (e) {
        return fail(e instanceof Error ? e.message : String(e))
    }

    const addFile = window.cheerpOSAddStringFile ?? window.cheerpjAddStringFile
    const runMain = window.cheerpjRunMain
    if (!addFile || !runMain) return fail('CheerpJ API 를 찾지 못했습니다.')

    const out = consoleEl()
    const n = ++runSeq
    const srcPath = `/str/${main.fileName}`   // 같은 이름이면 덮어씀 (cheerpOSAddStringFile 동작)
    const outDir = `/files/run${n}/`   // 실행마다 새 폴더 → 이전 실행의 .class 가 섞이지 않음

    onPhase?.('running')
    const started = performance.now()

    const job = (async (): Promise<RunResult> => {
        addFile(srcPath, new TextEncoder().encode(code))

        // 1) 컴파일 (첫 실행은 tools.jar 다운로드 포함)
        out.textContent = ''
        const javacExit = await runMain(
            'com.sun.tools.javac.Main',
            PROFILE.javacClassPath,
            ...PROFILE.javacArgs,
            '-encoding', 'UTF-8',
            '-d', outDir,
            srcPath,
        )
        const compileLog = out.textContent ?? ''
        if (javacExit !== 0) {
            return {
                lines: [{ stream: 'err', text: '컴파일 오류' }, ...toLines(compileLog, 'err')],
                ok: false,
                timedOut: false,
                ms: performance.now() - started,
            }
        }

        // 2) 실행
        out.textContent = ''
        const runStarted = performance.now()
        const exit = await runMain(main.fqcn, outDir)
        const lines = toLines(out.textContent ?? '', 'out')
        if (exit !== 0) lines.push({ stream: 'err', text: `종료 코드 ${exit}` })
        return { lines, ok: exit === 0, timedOut: false, ms: performance.now() - runStarted }
    })()

    try {
        return await withTimeout(job, JAVA_TIMEOUT_MS + (n === 1 ? LOAD_TIMEOUT_MS : 0), 'timeout')
    } catch (e) {
        if (e instanceof Error && e.message === 'timeout') {
            stuck = true
            return {
                lines: [
                    ...toLines(out.textContent ?? '', 'out'),
                    { stream: 'err', text: `시간 초과 — ${JAVA_TIMEOUT_MS / 1000}초 안에 끝나지 않았습니다. 다시 실행하려면 새로고침해 주세요.` },
                ],
                ok: false,
                timedOut: true,
                ms: performance.now() - started,
            }
        }
        return fail(e instanceof Error ? `${e.name}: ${e.message}` : String(e))
    }
}
