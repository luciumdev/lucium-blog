package com.ysh.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import io.jsonwebtoken.security.SignatureException;

@Slf4j
@Component
public class JwtProvider {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration}")
    private long accessExpiration;      // 초 단위

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;     // 초 단위

    private SecretKey key;

    @PostConstruct
    public void init() {
        // 최소 32자(256bit) 검증 — 짧은 시크릿은 서버 기동 시 즉시 실패
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret must be at least 256 bits (32 bytes).");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String userId, String role) {
        return buildToken(userId, role, accessExpiration * 1000L);
    }

    public String generateRefreshToken(String userId) {
        return buildToken(userId, null, refreshExpiration * 1000L);
    }

    private String buildToken(String userId, String role, long expirationMs) {
        Date now = new Date();
        JwtBuilder builder = Jwts.builder()
                .subject(userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .id(UUID.randomUUID().toString())   // jti: Replay Attack 방지용
                .signWith(key, Jwts.SIG.HS256);

        if (role != null) {
            builder.claim("role", role);
        }
        return builder.compact();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("JWT signature invalid: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT malformed: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claim empty: {}", e.getMessage());
        }
        return false;
    }

    public String getUserId(String token) {
        return parseToken(token).getPayload().getSubject();
    }

    public String getRole(String token) {
        return parseToken(token).getPayload().get("role", String.class);
    }

    public long getRefreshExpiration() {
        return refreshExpiration;
    }

    private Jws<Claims> parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }


}
