import type { CSSProperties, ReactNode } from 'react'
import { LuciumMascot } from '@/shared/components/LuciumLogo'

/**
 * 소개 (/about)
 */

const LINKS: { label: string; href: string | null }[] = [
    { label: 'GitHub', href: 'https://github.com/luciumdev' },
    // TODO: LinkedIn 프로필 주소
    { label: 'LinkedIn', href: null },
]

const INTRO = [
    'FULL STACK DEVELOPER',
]

interface Career {
    company: string
    role: string
    period: string
    points: string[]
}

const CAREERS: Career[] = [
    {
        company: '에듀테크',
        role: '개발자',
        period: '2024.12 – 2025.12',
        points: [
            '영어 학습·창업 교육 플랫폼 개발 및 배포',
            '교육청 다국어 학습 플랫폼 담당',
        ],
    },
    {
        company: '에듀테크',
        role: '개발자 인턴',
        period: '2024.07 – 2024.11',
        points: [
            '교육청 다국어 학습 웹 서비스 영어 채팅 기능 구현',
        ],
    },
]

const HOW_IT_WORKS: { title: string; body: string }[] = [
    {
        title: '검색',
        body: 'Elasticsearch + nori 형태소 분석. "트랜잭션이"처럼 조사를 붙여도 찾고, 검색어는 결과에 하이라이트됩니다.',
    },
    {
        title: '데이터 수집',
        body: '뉴스는 30분, 환율은 1시간, 지수는 하루 2번 스케줄러가 수집합니다. 매 실행은 성공·실패와 건수를 기록해 메인의 "데이터 수집" 카드에 그대로 보입니다.',
    },
    {
        title: '뉴스 보강',
        body: '수집 직후 LLM 배치로 HackerNews 제목을 번역하고 키워드를 정규화합니다. 테크·경제 공통 키워드는 Elasticsearch 집계로 계산합니다.',
    },
    {
        title: '조회수 · 캐시',
        body: '조회수는 Redis 에 모았다가 주기적으로 DB 에 반영하고, 자주 읽는 데이터는 Redis 캐시 → DB 순으로 읽습니다.',
    },
    {
        title: '배포',
        body: 'main 에 push 하면 GitHub Actions 가 테스트 → Docker 이미지 빌드 → EC2(Docker Compose) 배포까지 진행합니다.',
    },
]

const STACK: { group: string; items: string[] }[] = [
    { group: '백엔드', items: ['Java 21', 'Spring Boot 4', 'Spring Data JPA', 'Flyway'] },
    { group: '데이터', items: ['PostgreSQL', 'Redis', 'Elasticsearch 9 (nori)'] },
    { group: '프론트엔드', items: ['React', 'TypeScript', 'Vite', 'Tailwind CSS', 'TanStack Query'] },
    { group: '인프라', items: ['Docker Compose', 'AWS EC2 · S3 · CloudFront', 'Nginx', 'GitHub Actions'] },
]

export default function AboutPage() {
    return (
        <div className="max-w-[860px] flex flex-col gap-4">
            <Section delay={0.05} className="p-6 sm:p-7">
                <div className="flex flex-wrap items-center gap-4">
                    <span className="w-[72px] h-[72px] shrink-0 rounded-full bg-primary-tint shadow-[inset_0_0_0_1px_#CFE5F5] flex items-center justify-center overflow-hidden">
                        <LuciumMascot size={58} />
                    </span>
                    <div className="min-w-0 flex-1">
                        <h1 className="text-2xl font-bold tracking-tight">lucium.dev</h1>
                        <p className="mt-0.5 text-[15px] text-ink-faint">풀스택에서 MLOps로</p>
                    </div>
                    <div className="flex gap-2">
                        {LINKS.filter((l) => l.href).map((l) => (
                            <a
                                key={l.label}
                                href={l.href as string}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="h-9 px-3.5 flex items-center rounded-full bg-primary-subtle text-sm text-primary-hover hover:bg-primary-tint transition-colors"
                            >
                                {l.label}
                            </a>
                        ))}
                    </div>
                </div>
                <div className="mt-5 pt-4 border-t border-border space-y-2 text-[15px] leading-[1.8] text-ink [text-wrap:pretty]">
                    {INTRO.map((p) => (
                        <p key={p}>{p}</p>
                    ))}
                </div>
            </Section>

            <Section delay={0.12} className="px-6 sm:px-7 pt-5 pb-3">
                <Title>경력</Title>
                <ol>
                    {CAREERS.map((c) => (
                        <li key={c.role + c.period} className="py-4 border-t border-border first:border-t-0">
                            <div className="flex flex-wrap items-baseline justify-between gap-x-3">
                                <p className="text-base font-semibold">
                                    {c.company}
                                    <span className="ml-2 text-sm font-normal text-ink-muted">{c.role}</span>
                                </p>
                                <p className="text-[13px] text-ink-faint tabular-nums">{c.period}</p>
                            </div>
                            <ul className="mt-2 space-y-1 text-[15px] text-ink-muted">
                                {c.points.map((pt) => (
                                    <li key={pt} className="flex gap-2">
                                        <span aria-hidden className="mt-[0.7em] w-1 h-1 shrink-0 rounded-full bg-primary" />
                                        <span>{pt}</span>
                                    </li>
                                ))}
                            </ul>
                        </li>
                    ))}
                </ol>
            </Section>

            <Section delay={0.19} className="px-6 sm:px-7 pt-5 pb-3">
                <Title>이 사이트는 이렇게 만들었습니다</Title>
                <dl>
                    {HOW_IT_WORKS.map((h) => (
                        <div key={h.title} className="grid sm:grid-cols-[120px_1fr] gap-x-4 gap-y-1 py-3.5 border-t border-border first:border-t-0">
                            <dt className="text-[15px] font-semibold">{h.title}</dt>
                            <dd className="text-[15px] leading-relaxed text-ink-muted [text-wrap:pretty]">{h.body}</dd>
                        </div>
                    ))}
                </dl>
            </Section>

            <Section delay={0.26} className="px-6 sm:px-7 pt-5 pb-5">
                <Title>기술 스택</Title>
                <dl className="space-y-3">
                    {STACK.map((s) => (
                        <div key={s.group} className="grid sm:grid-cols-[120px_1fr] gap-x-4 gap-y-1.5">
                            <dt className="text-sm text-ink-faint pt-0.5">{s.group}</dt>
                            <dd className="flex flex-wrap gap-1.5">
                                {s.items.map((it) => (
                                    <span key={it} className="px-2.5 py-0.5 rounded-md bg-surface-muted text-sm text-ink">
                                        {it}
                                    </span>
                                ))}
                            </dd>
                        </div>
                    ))}
                </dl>
            </Section>
        </div>
    )
}

function Section({ delay, className, children }: { delay: number; className?: string; children: ReactNode }) {
    const style: CSSProperties = { animationDelay: `${delay}s` }
    return (
        <section style={style} className={`fx bg-panel border border-border rounded-[10px] ${className ?? ''}`}>
            {children}
        </section>
    )
}

function Title({ children }: { children: ReactNode }) {
    return (
        <div className="sec-h pb-2.5 mb-1 border-b border-border">
            <h2 className="text-[17px] font-bold">{children}</h2>
        </div>
    )
}
