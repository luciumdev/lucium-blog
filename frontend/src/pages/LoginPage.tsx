import { useState, type FormEvent } from 'react'
import { useNavigate, useLocation, Link } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { authApi } from '@/features/auth/api'
import { useAuthStore } from '@/features/auth/store'
import { ApiError } from '@/shared/api/ApiError'
import { Button, Input, Card } from '@/shared/components'

interface LocationState {
    from?: { pathname: string }
}

export default function LoginPage() {
    const navigate = useNavigate()
    const location = useLocation()
    const setTokens = useAuthStore((s) => s.setTokens)

    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')

    const mutation = useMutation({
        mutationFn: authApi.login,
        onSuccess: (tokens) => {
            setTokens(tokens)
            const redirectTo =
                (location.state as LocationState)?.from?.pathname ?? '/admin'
            navigate(redirectTo, { replace: true })
        },
    })

    const submit = (e: FormEvent) => {
        e.preventDefault()
        if (!email.trim() || !password) return
        mutation.mutate({ email: email.trim(), password })
    }

    /**
     * 에러 메시지 정규화:
     *  - 401 INVALID_CREDENTIALS → "이메일 또는 비밀번호가 올바르지 않습니다"
     *  - 그 외 → 백엔드 detail 또는 generic
     */
    const errorMessage = (() => {
        const err = mutation.error
        if (!err) return null
        if (err instanceof ApiError) {
            if (err.status === 401) return '이메일 또는 비밀번호가 올바르지 않습니다.'
            return err.message
        }
        return '로그인 중 오류가 발생했습니다.'
    })()

    return (
        <div className="max-w-sm mx-auto py-8">
            <Card padding="lg">
                <header className="mb-6">
                    <p className="font-mono text-xs text-ink-faint">$ ./login.sh</p>
                    <h1 className="mt-2 font-serif text-xl font-semibold text-ink">
                        관리자 로그인
                    </h1>
                </header>

                <form onSubmit={submit} className="flex flex-col gap-4">
                    <Input
                        label="이메일"
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        autoComplete="email"
                        required
                        disabled={mutation.isPending}
                    />
                    <Input
                        label="비밀번호"
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        autoComplete="current-password"
                        required
                        disabled={mutation.isPending}
                    />

                    {errorMessage && (
                        <p className="text-sm text-danger">{errorMessage}</p>
                    )}

                    <Button
                        type="submit"
                        variant="primary"
                        isLoading={mutation.isPending}
                        disabled={!email.trim() || !password || mutation.isPending}
                    >
                        로그인
                    </Button>

                    <Link
                        to="/"
                        className="text-center text-xs text-ink-faint hover:text-ink-muted transition-colors"
                    >
                        ← 홈으로
                    </Link>
                </form>
            </Card>
        </div>
    )
}