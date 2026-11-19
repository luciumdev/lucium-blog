export interface LoginRequest {
    email: string,
    password: string,
}

export interface TokenResponse {
    accessToken: string,
    refreshToken: string,
    expiresIn: number,
}

export interface DecodedAccessToken {
    sub: string,
    role: string
    email: string,
    exp: number,
    iat: number,
}
