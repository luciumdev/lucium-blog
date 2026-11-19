const KEY = 'ysh-blog:read'

interface ReadMap {
    [publicId: string]: number  // 읽은 시각, ms timestamp
}

/**
 * localStorage에 읽음 기록 관리.
 *
 * 디자인 결정:
 *  - timestamp 단일 키 (Date.now()) — 시간 표시에 활용
 *  - 한 키에 JSON 객체로 모음 — 마이그레이션 시 한 번에 옮길 수 있음
 *  - try-catch로 차단된 환경(Safari Private 등) 안전
 *
 * 추후 백엔드 API로 마이그레이션 시:
 *  - GET /v1/visitors/me/reads?postIds=... → Set<publicId>
 *  - 이 함수의 시그니처는 그대로, 내부 구현만 fetch로 교체
 */
function load(): ReadMap {
    try {
        const raw = localStorage.getItem(KEY)
        return raw ? (JSON.parse(raw) as ReadMap) : {}
    } catch {
        return {}
    }
}

export function markAsRead(publicId: string): void {
    try {
        const map = load()
        map[publicId] = Date.now()
        localStorage.setItem(KEY, JSON.stringify(map))
    } catch {
        // localStorage 차단 환경 — 무시 (기능 degrade)
    }
}

export function getReadAt(publicId: string): number | null {
    return load()[publicId] ?? null
}