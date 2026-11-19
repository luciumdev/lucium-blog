export interface TagColor {
    text: string
    border: string
    hoverBg: string
}

const PALETTE: TagColor[] = [
    { text: '#0369A1', border: 'rgba(14, 165, 233, 0.4)',  hoverBg: 'rgba(186, 230, 253, 0.6)' }, // sky
    { text: '#B45309', border: 'rgba(245, 158, 11, 0.4)',  hoverBg: 'rgba(254, 243, 199, 0.6)' }, // amber
    { text: '#BE123C', border: 'rgba(244, 63, 94, 0.4)',   hoverBg: 'rgba(254, 226, 226, 0.6)' }, // rose
    { text: '#6D28D9', border: 'rgba(139, 92, 246, 0.4)',  hoverBg: 'rgba(237, 233, 254, 0.6)' }, // violet
    { text: '#0E7490', border: 'rgba(6, 182, 212, 0.4)',   hoverBg: 'rgba(207, 250, 254, 0.6)' }, // cyan
    { text: '#BE185D', border: 'rgba(236, 72, 153, 0.4)',  hoverBg: 'rgba(252, 231, 243, 0.6)' }, // pink
    { text: '#C2410C', border: 'rgba(249, 115, 22, 0.4)',  hoverBg: 'rgba(255, 237, 213, 0.6)' }, // orange
    { text: '#4D7C0F', border: 'rgba(132, 204, 22, 0.4)',  hoverBg: 'rgba(236, 252, 203, 0.6)' }, // lime
    { text: '#4338CA', border: 'rgba(99, 102, 241, 0.4)',  hoverBg: 'rgba(224, 231, 255, 0.6)' }, // indigo
    { text: '#0F766E', border: 'rgba(20, 184, 166, 0.4)',  hoverBg: 'rgba(204, 251, 241, 0.6)' }, // teal
    { text: '#A21CAF', border: 'rgba(217, 70, 239, 0.4)',  hoverBg: 'rgba(250, 232, 255, 0.6)' }, // fuchsia
    { text: '#475569', border: 'rgba(100, 116, 139, 0.4)', hoverBg: 'rgba(241, 245, 249, 0.6)' }, // slate
]

function hash(tag: string): number {
    let h = 0
    for (let i = 0; i < tag.length; i++) {
        h = (h * 31 + tag.charCodeAt(i)) | 0
    }
    return Math.abs(h)
}

export function getTagColor(tag: string): TagColor {
    return PALETTE[hash(tag) % PALETTE.length]
}
/** 그라디언트 등에서 인접 색을 뽑기 위해 노출. */
export const PALETTE_SIZE = PALETTE.length

/** 같은 해시 함수로 인덱스만 반환. 팔레트 순환에 사용. */
export function paletteIndex(tag: string): number {
    return hash(tag) % PALETTE.length
}

/** 인덱스로 직접 색 조회. */
export function getPaletteAt(i: number): TagColor {
    return PALETTE[((i % PALETTE.length) + PALETTE.length) % PALETTE.length]
}