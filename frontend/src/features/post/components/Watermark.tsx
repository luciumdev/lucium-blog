/**
 * 눈에는 거의 안 보이는 워터마크 (불투명도 1.5%).
 * 읽을 때는 보이지 않고, 캡처 이미지의 대비를 크게 올리면 "lucium.dev"가 드러남 → 출처 증명용.
 * pointer-events: none → 클릭·스크롤·실습 조작에 영향 없음.
 */
const TEXT = 'lucium.dev'

const svg = encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="260" height="160">` +
        `<text x="20" y="100" transform="rotate(-24 130 80)" font-family="monospace" font-size="18" font-weight="700" fill="#808080" fill-opacity="0.015">${TEXT}</text>` +
        `</svg>`,
)

export function Watermark() {
    return (
        <div
            aria-hidden="true"
            className="pointer-events-none absolute inset-0 z-10 print:hidden"
            style={{ backgroundImage: `url("data:image/svg+xml,${svg}")`, backgroundRepeat: 'repeat' }}
        />
    )
}
