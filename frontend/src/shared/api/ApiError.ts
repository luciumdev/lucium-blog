import type { ProblemDetail } from "@/shared/api/types";

export class ApiError extends Error {
    readonly status: number;
    readonly problem?: ProblemDetail;   // 네트워크 에러엔 서버 problem 없음 → 옵셔널

    constructor(message: string, status: number, problem?: ProblemDetail) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.problem = problem;
    }

    static network(cause: unknown): ApiError {
        return new ApiError(
            cause instanceof Error ? cause.message : "Network Error",
            0,
        );
    }
}