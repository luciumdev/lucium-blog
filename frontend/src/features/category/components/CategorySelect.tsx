import {
    Listbox,
    ListboxButton,
    ListboxOption,
    ListboxOptions,
} from '@headlessui/react'
import { CategoryIcon } from '@/shared/lib/categoryIcon'
import type { Category } from '@/features/category/types'

interface CategorySelectProps {
    value: number | null
    onChange: (id: number | null) => void
    categories: Category[] | undefined
    error?: string
    placeholder?: string
}

/**
 * 카테고리 드롭다운 — Headless UI Listbox 기반.
 *
 * 책임 분리:
 *  - 접근성 / 키보드 nav / focus trap — Headless UI
 *  - 시각적 톤 / 디자인 토큰 — Tailwind utility
 *  - 데이터 모델 — number | null (백엔드 categoryId 매핑)
 */
export function CategorySelect({
                                   value,
                                   onChange,
                                   categories,
                                   error,
                                   placeholder = '선택 안 함',
                               }: CategorySelectProps) {
    const selected = categories?.find((c) => c.id === value) ?? null

    return (
        <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-ink">카테고리</label>

            <Listbox value={value} onChange={onChange}>
                <div className="relative">
                    <ListboxButton
                        className={`
                            relative w-full h-10 pl-3 pr-9 text-left text-sm
                            rounded-md border bg-surface text-ink cursor-pointer
                            transition-colors
                            focus:outline-none
                            focus-visible:ring-2 focus-visible:ring-primary focus-visible:border-primary
                            data-[open]:ring-2 data-[open]:ring-primary data-[open]:border-primary
                            ${
                            error
                                ? 'border-danger'
                                : 'border-border hover:border-border-strong'
                        }
                        `}
                    >
                        {selected ? (
                            <span className="flex items-center gap-2">
                                <CategoryIcon
                                    slug={selected.slug}
                                    className="w-4 h-4 text-ink-muted"
                                />
                                <span>{selected.name}</span>
                            </span>
                        ) : (
                            <span className="text-ink-faint">{placeholder}</span>
                        )}
                        <span className="absolute inset-y-0 right-0 flex items-center pr-2.5 text-ink-faint">
                            <ChevronIcon />
                        </span>
                    </ListboxButton>

                    <ListboxOptions
                        className="
                            absolute z-50 left-0 right-0 mt-1
                            max-h-60 overflow-auto
                            rounded-md border border-border bg-panel
                            shadow-md focus:outline-none
                            py-1
                            origin-top
                            transition duration-100 ease-out
                            data-[closed]:scale-95 data-[closed]:opacity-0
                        "
                        transition
                    >
                        <ListboxOption
                            value={null}
                            className="
                                group flex items-center gap-2 px-3 py-2 text-sm cursor-pointer
                                text-ink-muted
                                data-[focus]:bg-surface-muted
                            "
                        >
                            <span className="w-4 h-4" aria-hidden />
                            <span>선택 안 함</span>
                        </ListboxOption>

                        {categories?.map((category) => (
                            <ListboxOption
                                key={category.id}
                                value={category.id}
                                className="
                                    group flex items-center gap-2 px-3 py-2 text-sm cursor-pointer
                                    text-ink
                                    data-[focus]:bg-surface-muted
                                    data-[selected]:bg-primary-tint data-[selected]:text-primary
                                "
                            >
                                <CategoryIcon
                                    slug={category.slug}
                                    className="w-4 h-4 text-ink-muted group-data-[selected]:text-primary"
                                />
                                <span>{category.name}</span>
                                <CheckIcon className="ml-auto w-3.5 h-3.5 opacity-0 group-data-[selected]:opacity-100" />
                            </ListboxOption>
                        ))}
                    </ListboxOptions>
                </div>
            </Listbox>

            {error && <p className="text-xs text-danger">{error}</p>}
        </div>
    )
}

function ChevronIcon() {
    return (
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
            <polyline points="6 9 12 15 18 9" />
        </svg>
    )
}

function CheckIcon({ className = '' }: { className?: string }) {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" className={className} aria-hidden="true">
            <polyline points="20 6 9 17 4 12" />
        </svg>
    )
}