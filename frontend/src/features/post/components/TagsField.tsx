import { useState, type KeyboardEvent, type ClipboardEvent } from 'react'
import { getTagColor } from '@/shared/lib/tagColor'

interface TagsFieldProps {
    value: string[]
    onChange: (tags: string[]) => void
    error?: string
    placeholder?: string
    maxTags?: number
    maxLengthPerTag?: number
}

/**
 * 태그 chip 입력 컨트롤.
 *
 * 동작:
 *  - Enter / 콤마(,) — 입력 확정 + chip 추가
 *  - Backspace (input 비어있을 때) — 마지막 chip 제거
 *  - Paste (콤마/줄바꿈 포함) — 일괄 split 추가
 *  - 빈 입력으로 blur — 자동 확정 (입력 도중 다른 영역 클릭 시 미저장 방지)
 *
 * 한글 IME trap:
 *  - 조합 중인 Enter(확정)는 add 트리거에서 제외 — e.nativeEvent.isComposing 검사
 */
export function TagsField({
                              value,
                              onChange,
                              error,
                              placeholder,
                              maxTags = 20,
                              maxLengthPerTag = 50,
                          }: TagsFieldProps) {
    const [input, setInput] = useState('')

    const addTag = (raw: string) => {
        const trimmed = raw.trim()
        if (!trimmed) return
        if (trimmed.length > maxLengthPerTag) return
        if (value.includes(trimmed)) return
        if (value.length >= maxTags) return
        onChange([...value, trimmed])
    }

    const removeTag = (tag: string) => {
        onChange(value.filter((t) => t !== tag))
    }

    const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
        // IME 조합 중 Enter는 변환 확정용. add 트리거에서 제외.
        if (e.nativeEvent.isComposing) return

        if (e.key === 'Enter' || e.key === ',') {
            e.preventDefault()
            addTag(input)
            setInput('')
        } else if (e.key === 'Backspace' && input === '' && value.length > 0) {
            e.preventDefault()
            removeTag(value[value.length - 1])
        }
    }

    const handleBlur = () => {
        if (input.trim()) {
            addTag(input)
            setInput('')
        }
    }

    const handlePaste = (e: ClipboardEvent<HTMLInputElement>) => {
        const text = e.clipboardData.getData('text')
        if (!text.includes(',') && !text.includes('\n')) return
        e.preventDefault()
        text.split(/[,\n]/).forEach((part) => addTag(part))
        setInput('')
    }

    const reachedLimit = value.length >= maxTags

    return (
        <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-ink">태그</label>

            <div
                className={`
                    flex flex-wrap items-center gap-1.5 px-2 py-1.5 min-h-10
                    rounded-md border bg-surface
                    transition-colors
                    ${
                    error
                        ? 'border-danger focus-within:border-danger focus-within:ring-2 focus-within:ring-danger'
                        : 'border-border focus-within:border-primary focus-within:ring-2 focus-within:ring-primary'
                }
                `}
            >
                {value.map((tag) => (
                    <Chip key={tag} tag={tag} onRemove={() => removeTag(tag)} />
                ))}

                <input
                    type="text"
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    onKeyDown={handleKeyDown}
                    onBlur={handleBlur}
                    onPaste={handlePaste}
                    disabled={reachedLimit}
                    placeholder={
                        value.length === 0
                            ? placeholder ?? 'Enter 또는 콤마로 추가'
                            : ''
                    }
                    aria-label="태그 입력"
                    className="
                        flex-1 min-w-[120px] bg-transparent text-ink text-sm
                        placeholder:text-ink-faint outline-none
                        disabled:cursor-not-allowed
                    "
                />
            </div>

            <div className="flex items-center justify-between text-xs">
                <span className={error ? 'text-danger' : 'text-ink-faint'}>
                    {error ?? 'Enter · 콤마로 추가 / Backspace로 마지막 삭제'}
                </span>
                <span className={reachedLimit ? 'text-danger' : 'text-ink-faint'}>
                    {value.length}/{maxTags}
                </span>
            </div>
        </div>
    )
}

function Chip({ tag, onRemove }: { tag: string; onRemove: () => void }) {
    const color = getTagColor(tag)
    return (
        <span
            style={{
                color: color.text,
                borderColor: color.border,
                backgroundColor: color.hoverBg,
            }}
            className="inline-flex items-center gap-1 pl-2 pr-1 py-0.5 rounded font-mono text-xs border"
        >
            <span>#{tag}</span>
            <button
                type="button"
                onClick={onRemove}
                onMouseDown={(e) => e.preventDefault()}  // input blur 방지
                aria-label={`태그 ${tag} 제거`}
                className="
                    inline-flex items-center justify-center
                    w-4 h-4 rounded-sm
                    opacity-60 hover:opacity-100 hover:bg-black/10
                    transition-opacity
                "
            >
                <CloseIcon />
            </button>
        </span>
    )
}

function CloseIcon() {
    return (
        <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" aria-hidden="true">
            <line x1="6" y1="6" x2="18" y2="18" />
            <line x1="6" y1="18" x2="18" y2="6" />
        </svg>
    )
}