import type { CollectionDay, RunStatus } from './types'

/**
 * 칸(막대)과 범례가 같은 클래스를 쓰도록 여기 한 곳에서만 정의.
 */
export const RUN_COLOR: Record<RunStatus, string> = {
    SUCCESS: 'bg-[#7C93C9]',   // 남색
    PARTIAL: 'bg-[#E9C46A]',   // 머스터드
    EMPTY: 'bg-[#E6E6E6]',     // 회색
    FAILED: 'bg-[#D98C8C]',    // 장미
}

export const RUN_LABEL: Record<RunStatus, string> = {
    SUCCESS: '성공',
    PARTIAL: '일부 실패',
    EMPTY: '데이터 없음',
    FAILED: '실패',
}

export const RUN_STATUSES: RunStatus[] = ['SUCCESS', 'PARTIAL', 'EMPTY', 'FAILED']

/** 실행 기록이 없는 칸(점선) — 상태값이 아니라 '칸이 비었음' 표시 */
export const NO_RUN_CLASS = 'border border-dashed border-border-strong'
export const NO_RUN_LABEL = '실행 없음'

/** 범례 항목 — 4개 상태 + 실행 없음. 범례는 이 배열만 그림 */
export const RUN_LEGEND: { key: RunStatus | 'NONE'; label: string; className: string }[] = [
    ...RUN_STATUSES.map((s) => ({ key: s, label: RUN_LABEL[s], className: RUN_COLOR[s] })),
    { key: 'NONE', label: NO_RUN_LABEL, className: NO_RUN_CLASS },
]

/**
 * 하루 칸의 대표 상태.
 *  - 실행 없음 → null (점선 칸)
 *  - 모두 실패 → FAILED / 실패·일부 실패가 섞이면 → PARTIAL
 *  - 성공이 있으면 → SUCCESS / 데이터 없음만 → EMPTY
 */
export function dayStatus(d: CollectionDay): RunStatus | null {
    const total = d.success + d.partial + d.empty + d.failed
    if (total === 0) return null
    if (d.failed > 0 && d.success + d.partial === 0) return 'FAILED'
    if (d.failed > 0 || d.partial > 0) return 'PARTIAL'
    if (d.success > 0) return 'SUCCESS'
    return 'EMPTY'
}
