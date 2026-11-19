/**
 * 코드 실행 남용 방지 (탭 단위).
 *
 * 실행 자체는 방문자 브라우저에서 돌아서 서버 부하는 없지만,
 *  - 연타하면 방문자 PC 의 CPU 를 계속 점유하고
 *  - Java 는 첫 실행 때 서버에서 javac·플랫폼 이미지(약 16MB)를 받으므로
 * 아래 세 가지로 막는다.
 *  1. 동시에 하나만 — 다른 코드 블록이 실행 중이면 거절
 *  2. 쿨다운 — 직전 실행이 끝나고 1.5초 안에는 거절
 *  3. 분당 횟수 제한 — 최근 60초 동안 20회 초과 시 거절 (sessionStorage 로 새로고침해도 유지)
 *
 * 서버 쪽(/cheerpj/ 다운로드)은 nginx limit_conn + 장기 캐시로 따로 막음.
 */

const MAX_RUNS_PER_WINDOW = 20
const WINDOW_MS = 60_000
const COOLDOWN_MS = 1_500
const STORAGE_KEY = 'lucium:code-runs'

let running = false
let lastEndedAt = 0

function loadStamps(): number[] {
    try {
        const raw = sessionStorage.getItem(STORAGE_KEY)
        const arr: unknown = raw ? JSON.parse(raw) : []
        return Array.isArray(arr) ? arr.filter((v): v is number => typeof v === 'number') : []
    } catch {
        return []   // 사생활 보호 모드 등 storage 사용 불가 → 메모리만 사용
    }
}

let stamps = loadStamps()

function saveStamps() {
    try {
        sessionStorage.setItem(STORAGE_KEY, JSON.stringify(stamps))
    } catch {
        /* storage 불가 — 무시 */
    }
}

export type RunTicket = { ok: true; release: () => void } | { ok: false; reason: string }

export function acquireRun(now = Date.now()): RunTicket {
    if (running) {
        return { ok: false, reason: '다른 코드가 실행 중입니다. 끝난 뒤 다시 눌러 주세요.' }
    }
    if (now - lastEndedAt < COOLDOWN_MS) {
        return { ok: false, reason: '잠시 후 다시 실행해 주세요.' }
    }

    stamps = stamps.filter((t) => now - t < WINDOW_MS)
    if (stamps.length >= MAX_RUNS_PER_WINDOW) {
        const waitSec = Math.ceil((WINDOW_MS - (now - stamps[0])) / 1000)
        return { ok: false, reason: `실행 횟수가 너무 많습니다. ${waitSec}초 뒤 다시 시도해 주세요.` }
    }

    stamps.push(now)
    saveStamps()
    running = true

    let released = false
    return {
        ok: true,
        release: () => {
            if (released) return
            released = true
            running = false
            lastEndedAt = Date.now()
        },
    }
}