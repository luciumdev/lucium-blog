/**
 * dackend: ApiResponse<T> 래퍼 - 응답 형식 통일함
 */
export interface ApiResponse<T> {
    success: boolean;
    data: T;
}

export interface ProblemDetail {
    type: string;
    title: string;
    status: number;
    detail: string;
    instance?: string;
    errors?:Array<{field:string, message:string}>
    [key: string]: unknown;
}

export interface Page<T> {
    content: T[];
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
    empty: boolean;
}