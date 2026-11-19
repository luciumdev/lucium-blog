/**
 * 게시글 코드 블록 브라우저 실행기 — 실행은 방문자 브라우저에서, 서버 연산 0.
 *
 *  - Python: Pyodide(CPython → WebAssembly). 첫 실행 때 런타임(약 10MB)을 CDN 에서 받고, 이후엔 재사용.
 *  - JavaScript: 실행마다 새 Web Worker.
 *    Python · JS 는 Web Worker 에서 돌아서 무한 루프가 나도 화면이 멈추지 않음 → 제한 시간 넘으면 worker 종료.
 *  - Java: CheerpJ(브라우저 JVM) — javaRunner.ts 참고. Java 17 문법까지 (21 전용 문법 불가).
 *  - C 는 지원하지 않음.
 *  - 연타·남용 방지는 runGuard.ts (동시 1개, 쿨다운, 분당 횟수 제한).
 *
 * 주의: worker 는 블로그와 같은 origin 이라 fetch 가 가능함.
 *       코드는 관리자만 쓰는 글 본문이라 허용하지만, 방문자 입력 코드를 돌리는 용도로 쓰면 안 됨.
 */

import { acquireRun } from './runguard.ts'
import { runJava, JAVA_RUNTIME_LABEL } from './javaRunner'

export type RunLang = 'python' | 'javascript' | 'java'

export interface OutputLine {
    stream: 'out' | 'err'
    text: string
}

export interface RunResult {
    lines: OutputLine[]
    ok: boolean
    timedOut: boolean
    /** 실제 코드 실행 시간 (런타임 다운로드 제외) */
    ms: number
    /** 남용 방지로 실행하지 않음 (runGuard) */
    rejected?: boolean
}

export type RunPhase = 'loading' | 'running'

const PYODIDE_VERSION = '0.26.4'
const PYODIDE_INDEX_URL = `https://cdn.jsdelivr.net/pyodide/v${PYODIDE_VERSION}/full/`

export const RUN_TIMEOUT_MS = 10_000
const PY_LOAD_TIMEOUT_MS = 60_000

const LANG_ALIASES: Record<string, RunLang> = {
    python: 'python',
    py: 'python',
    python3: 'python',
    javascript: 'javascript',
    js: 'javascript',
    java: 'java',
}

/** 코드 블록 언어명 → 실행 가능 언어 (실행 불가면 null) */
export function toRunLang(lang: string | undefined): RunLang | null {
    if (!lang) return null
    return LANG_ALIASES[lang.toLowerCase()] ?? null
}

export const RUN_LANG_LABEL: Record<RunLang, string> = {
    python: 'Python',
    javascript: 'JavaScript',
    java: JAVA_RUNTIME_LABEL,   // 'Java 17'
}

export async function runCode(lang: RunLang, code: string, onPhase?: (p: RunPhase) => void): Promise<RunResult> {
    const ticket = acquireRun()
    if (!ticket.ok) {
        return { lines: [{ stream: 'err', text: ticket.reason }], ok: false, timedOut: false, ms: 0, rejected: true }
    }
    try {
        if (lang === 'python') return await runPython(code, onPhase)
        if (lang === 'java') return await runJava(code, onPhase)
        return await runJavaScript(code, onPhase)
    } finally {
        ticket.release()
    }
}

/* ───────────────────────── 공통 ───────────────────────── */

function workerFromSource(source: string): Worker {
    const url = URL.createObjectURL(new Blob([source], { type: 'text/javascript' }))
    try {
        return new Worker(url)
    } finally {
        URL.revokeObjectURL(url)
    }
}

export function timeoutResult(lines: OutputLine[], ms: number): RunResult {
    return {
        lines: [...lines, { stream: 'err', text: `시간 초과 — ${RUN_TIMEOUT_MS / 1000}초 안에 끝나지 않아 중단했습니다.` }],
        ok: false,
        timedOut: true,
        ms,
    }
}

/* ───────────────────────── JavaScript ───────────────────────── */

const JS_WORKER = `
const lines = [];
const fmt = (v) => {
  if (typeof v === 'string') return v;
  if (v instanceof Error) return v.name + ': ' + v.message;
  if (v === undefined) return 'undefined';
  if (typeof v === 'function') return String(v);
  try { const s = JSON.stringify(v); return s === undefined ? String(v) : s; } catch (_) { return String(v); }
};
const sink = (stream) => (...args) => {
  lines.push({ stream, text: args.map(fmt).join(' ') });
  self.postMessage({ type: 'line', line: lines[lines.length - 1] });
};
console.log = console.info = console.debug = console.table = sink('out');
console.warn = console.error = sink('err');
self.onmessage = async (e) => {
  const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor;
  let ok = true;
  try {
    await new AsyncFunction(e.data.code)();
    // setTimeout 등 비동기 출력이 조금 늦게 찍히는 경우를 위해 한 틱 대기
    await new Promise((r) => setTimeout(r, 50));
  } catch (err) {
    ok = false;
    lines.push({ stream: 'err', text: (err && err.name ? err.name + ': ' + err.message : String(err)) });
  }
  self.postMessage({ type: 'done', ok, lines });
};
`

function runJavaScript(code: string, onPhase?: (p: RunPhase) => void): Promise<RunResult> {
    return new Promise((resolve) => {
        const worker = workerFromSource(JS_WORKER)
        const started = performance.now()
        const streamed: OutputLine[] = []
        onPhase?.('running')

        const timer = setTimeout(() => {
            worker.terminate()
            resolve(timeoutResult(streamed, performance.now() - started))
        }, RUN_TIMEOUT_MS)

        worker.onmessage = (e: MessageEvent) => {
            const msg = e.data as { type: 'line'; line: OutputLine } | { type: 'done'; ok: boolean; lines: OutputLine[] }
            if (msg.type === 'line') {
                streamed.push(msg.line)
                return
            }
            clearTimeout(timer)
            worker.terminate()
            resolve({ lines: msg.lines, ok: msg.ok, timedOut: false, ms: performance.now() - started })
        }
        worker.onerror = (e) => {
            clearTimeout(timer)
            worker.terminate()
            resolve({
                lines: [...streamed, { stream: 'err', text: e.message || '실행 중 오류가 발생했습니다.' }],
                ok: false,
                timedOut: false,
                ms: performance.now() - started,
            })
        }
        worker.postMessage({ code })
    })
}

/* ───────────────────────── Python (Pyodide) ───────────────────────── */

const PY_WORKER = `
let py = null;
let loading = null;

function cleanTraceback(msg) {
  // Pyodide 내부 프레임(/lib/python3xx.zip/...) 제거 → 사용자 코드(<exec>) 부분만 남김
  const lines = String(msg).split('\\n');
  const first = lines.findIndex((l) => l.includes('File "<exec>"'));
  if (first < 0) return String(msg).trim();
  return ['Traceback (most recent call last):', ...lines.slice(first)].join('\\n').trim();
}

self.onmessage = async (e) => {
  const { type, code, indexURL } = e.data;
  if (type === 'load') {
    try {
      if (!loading) {
        loading = (async () => {
          importScripts(indexURL + 'pyodide.js');
          py = await loadPyodide({ indexURL });
        })();
      }
      await loading;
      self.postMessage({ type: 'loaded' });
    } catch (err) {
      self.postMessage({ type: 'load-failed', message: String(err && err.message ? err.message : err) });
    }
    return;
  }

  const lines = [];
  py.setStdout({ batched: (s) => lines.push({ stream: 'out', text: s }) });
  py.setStderr({ batched: (s) => lines.push({ stream: 'err', text: s }) });
  // input() 은 지원하지 않음 → EOF 로 처리 (EOFError)
  py.setStdin({ stdin: () => null });

  let ok = true;
  const ns = py.globals.get('dict')();
  try {
    await py.loadPackagesFromImports(code);   // import numpy 등 → 필요한 패키지 자동 다운로드
    self.postMessage({ type: 'started' });
    await py.runPythonAsync(code, { globals: ns });
  } catch (err) {
    ok = false;
    lines.push({ stream: 'err', text: cleanTraceback(err && err.message ? err.message : err) });
  } finally {
    ns.destroy();
  }
  self.postMessage({ type: 'done', ok, lines });
};
`

let pyWorker: Worker | null = null
let pyReady: Promise<Worker> | null = null
/** 한 worker 를 공유하므로 실행은 한 번에 하나씩 */
let pyQueue: Promise<unknown> = Promise.resolve()

function resetPython() {
    pyWorker?.terminate()
    pyWorker = null
    pyReady = null
}

function loadPython(): Promise<Worker> {
    if (pyReady) return pyReady
    const worker = workerFromSource(PY_WORKER)
    pyWorker = worker

    pyReady = new Promise<Worker>((resolve, reject) => {
        const timer = setTimeout(() => {
            resetPython()
            reject(new Error('Python 런타임을 불러오는 데 너무 오래 걸립니다. 네트워크를 확인해 주세요.'))
        }, PY_LOAD_TIMEOUT_MS)

        worker.onmessage = (e: MessageEvent) => {
            const msg = e.data as { type: string; message?: string }
            if (msg.type === 'loaded') {
                clearTimeout(timer)
                resolve(worker)
            } else if (msg.type === 'load-failed') {
                clearTimeout(timer)
                resetPython()
                reject(new Error(`Python 런타임을 불러오지 못했습니다. (${msg.message ?? '알 수 없는 오류'})`))
            }
        }
        worker.postMessage({ type: 'load', indexURL: PYODIDE_INDEX_URL })
    })
    return pyReady
}

function runPython(code: string, onPhase?: (p: RunPhase) => void): Promise<RunResult> {
    const job = pyQueue.then(() => runPythonNow(code, onPhase))
    pyQueue = job.catch(() => undefined)
    return job
}

async function runPythonNow(code: string, onPhase?: (p: RunPhase) => void): Promise<RunResult> {
    if (!pyReady) onPhase?.('loading')

    let worker: Worker
    try {
        worker = await loadPython()
    } catch (e) {
        return {
            lines: [{ stream: 'err', text: e instanceof Error ? e.message : String(e) }],
            ok: false,
            timedOut: false,
            ms: 0,
        }
    }

    return new Promise<RunResult>((resolve) => {
        let started = performance.now()
        let timer: ReturnType<typeof setTimeout> | null = null

        // 패키지 다운로드(loadPackagesFromImports)는 시간 제한에서 제외 — 'started' 부터 계산
        const armTimeout = () => {
            started = performance.now()
            onPhase?.('running')
            timer = setTimeout(() => {
                resetPython()   // 무한 루프는 worker 를 죽이는 것 말고 멈출 방법이 없음 → 다음 실행 때 런타임 재로딩
                resolve(timeoutResult([], performance.now() - started))
            }, RUN_TIMEOUT_MS)
        }

        worker.onmessage = (e: MessageEvent) => {
            const msg = e.data as { type: 'started' } | { type: 'done'; ok: boolean; lines: OutputLine[] }
            if (msg.type === 'started') {
                armTimeout()
                return
            }
            if (timer) clearTimeout(timer)
            resolve({ lines: msg.lines, ok: msg.ok, timedOut: false, ms: performance.now() - started })
        }
        worker.onerror = (e) => {
            if (timer) clearTimeout(timer)
            resetPython()
            resolve({
                lines: [{ stream: 'err', text: e.message || 'Python 실행 중 오류가 발생했습니다.' }],
                ok: false,
                timedOut: false,
                ms: performance.now() - started,
            })
        }
        worker.postMessage({ type: 'run', code })
    })
}