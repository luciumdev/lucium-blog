import { useEffect, useState } from 'react'

/**
 * 입력값 변경 후 delay ms 대기 후 값 갱신.
 * 검색 input 등 잦은 변경을 stable 값으로 변환.
 */
export function useDebouncedValue<T>(value: T, delay: number): T {
    const [debounced, setDebounced] = useState(value)

    useEffect(() => {
        const timer = setTimeout(() => setDebounced(value), delay)
        return () => clearTimeout(timer)
    }, [value, delay])

    return debounced
}