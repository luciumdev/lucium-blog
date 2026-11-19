import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { aiJournalApi, type AiJournalFilter } from '../api'
import type { AiJournalDailyResponse } from '../types'

/**
 * 마지막 성공 응답을 브라우저에 보관 — 서버가 완전히 응답하지 않을 때(네트워크 장애·5xx)
 * 필터 없는 기본 조회만 저장.
 */
const STORAGE_KEY = 'lucium:ai-journal:last'

function readLastGood(): AiJournalDailyResponse | undefined {
    try {
        const raw = localStorage.getItem(STORAGE_KEY)
        return raw ? (JSON.parse(raw) as AiJournalDailyResponse) : undefined
    } catch {
        return undefined
    }
}

function saveLastGood(data: AiJournalDailyResponse) {
    // 빈 브리핑은 저장하지 않음 — 이전 성공본을 빈 값으로 덮어쓰지 않기 위함
    if (!data.briefing?.items?.length) return
    try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
    } catch {
        /* 저장 공간 부족·시크릿 모드 등은 무시 */
    }
}

/**
 * 오늘 브리핑 — 백엔드 06:00 KST 1회 수집 + 25h Redis TTL.
 *
 * 실패 시 이전 내용 유지:
 *  - 재조회 실패: React Query가 기존 data를 유지 (isError=true여도 data 존재) → 화면은 data 우선
 *  - 첫 진입부터 실패: localStorage의 마지막 성공본을 initialData로 사용
 *    (initialDataUpdatedAt=0 → 항상 stale 취급이라 마운트 시 서버 조회는 그대로 실행)
 *  - 필터 변경: keepPreviousData로 로딩 중 이전 목록 유지
 */
export function useAiJournalQuery(filter?: AiJournalFilter) {
    const isDefault = !filter?.source && !filter?.important

    return useQuery({
        queryKey: ['ai-journal', 'briefing', filter ?? {}],
        queryFn: async () => {
            const data = await aiJournalApi.briefing(filter)
            if (isDefault) saveLastGood(data)
            return data
        },
        initialData: isDefault ? readLastGood : undefined,
        initialDataUpdatedAt: 0,
        placeholderData: keepPreviousData,
        staleTime: 30 * 60_000,
        refetchInterval: 60 * 60_000,
    })
}