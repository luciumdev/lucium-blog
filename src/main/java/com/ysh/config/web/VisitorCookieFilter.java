package com.ysh.config.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * 방문자 UUID 쿠키 발급
 */
@Slf4j
@RequiredArgsConstructor
public class VisitorCookieFilter extends OncePerRequestFilter {

    private static final String VISITOR_COOKIE_NAME = "visitor_id";
    static final String REQUEST_ATTR_NAME = "visitorId";

    private static final Duration COOKIE_MAX_AGE = Duration.ofDays(365);

    /**
     * prod 전용
     * local/dev에서는 false
     */
    private final boolean isSecureCookie;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        UUID visitorId = readExistingCookie(request)
                .orElseGet(()-> (UUID) issueNewCookie(response));
        request.setAttribute(REQUEST_ATTR_NAME, visitorId);
        filterChain.doFilter(request, response);
    }

    /**
     * 쿠키 확인
     * @param request
     * @return
     */
    private Optional<UUID> readExistingCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (!VISITOR_COOKIE_NAME.equals(cookie.getName())) {
                continue;
            }
            try {
                return Optional.of(UUID.fromString(cookie.getValue()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID in 쿠키값, reissuing: {}", cookie.getValue());
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * 쿠키 생성
     * @param response
     * @return
     */
    private Object issueNewCookie(HttpServletResponse response) {
        UUID newId = UUID.randomUUID();
        ResponseCookie cookie  = ResponseCookie.from(VISITOR_COOKIE_NAME, newId.toString())
                .httpOnly(true)
                .secure(isSecureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(COOKIE_MAX_AGE)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return newId;
    }
}
