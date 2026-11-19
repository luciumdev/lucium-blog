import { useEffect } from 'react'
import { useIsFetching, useQueryClient } from '@tanstack/react-query'
import { statApi } from '../api'

const STORAGE_KEY = 'lucium:visit-day'

/** 페이지 로드당 1번 — StrictMode 이중 실행·레이아웃 재마운트 방지 */
let started = false

/** KST 기준 "yyyy-MM-dd" */
function todayKst(): string {
    return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Seoul' }).format(new Date())
}

function readRecordedDay(): string | null {
    try {
        return localStorage.getItem(STORAGE_KEY)
    } catch {
        return null
    }
}

function writeRecordedDay(day: string) {
    try {
        localStorage.setItem(STORAGE_KEY, day)
    } catch {
        /* 저장소 차단 — 서버 HLL 이 중복을 걸러 주므로 무시 */
    }
}

/**
 * 방문 기록 (PublicLayout 에서 1번 호출).
 *
 * 첫 방문자는 visitor_id 쿠키가 없어 첫 화면의 병렬 요청마다 서로 다른 UUID 가 발급됨.
 * 그 묶음이 끝나기 전에 기록하면 브라우저에 남지 않는 UUID 로 집계될 수 있으므로,
 * 진행 중인 쿼리가 0이 된 뒤(= 쿠키 확정 후)에 보낸다.
 * 같은 날 재방문은 localStorage 로 요청 자체를 생략 (서버도 HLL 로 중복 제거).
 */
export function useVisitTracker() {
    const queryClient = useQueryClient()
    const fetching = useIsFetching()

    useEffect(() => {
        if (started) return
        // 렌더 시점 값(fetching)은 자식 쿼리 시작 전일 수 있어 실제 상태를 다시 확인
        if (queryClient.isFetching() > 0) return

        const today = todayKst()
        if (readRecordedDay() === today) {
            started = true
            return
        }

        started = true
        statApi
            .visit()
            .then(() => {
                writeRecordedDay(today)
                queryClient.invalidateQueries({ queryKey: ['stat', 'site'] })
            })
            .catch(() => {
                // 실패 시 다음 쿼리 상태 변화(페이지 이동 등) 때 다시 시도
                started = false
            })
    }, [fetching, queryClient])
}
