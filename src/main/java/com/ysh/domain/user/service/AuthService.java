package com.ysh.domain.user.service;

import com.ysh.common.exception.BusinessException;
import com.ysh.common.exception.ErrorCode;
import com.ysh.config.security.JwtProvider;
import com.ysh.domain.user.dto.LoginRequest;
import com.ysh.domain.user.dto.TokenResponse;
import com.ysh.domain.user.entity.User;
import com.ysh.domain.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private String dummyBcryptHash;
    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";
    private static final String BLACKLIST_PREFIX     = "auth:blacklist:";

    private final UserRepository      userRepository;
    private final PasswordEncoder     passwordEncoder;
    private final JwtProvider         jwtProvider;
    private final StringRedisTemplate redisTemplate;

    @PostConstruct
    void init() {
        this.dummyBcryptHash = passwordEncoder.encode("__never_match__");
        log.debug("dummy BCrypt hash initialized for timing attack defense");
    }
    // ─────────────────────────────────────────────────────────────
    // 로그인
    // ─────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.getPassword(), dummyBcryptHash);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        // 타이밍 어택 방지
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        String userId       = user.getId().toString();
        String accessToken  = jwtProvider.generateAccessToken(userId, user.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(userId);
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + userId,
                refreshToken,
                Duration.ofSeconds(jwtProvider.getRefreshExpiration())
        );
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600L)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // 토큰 재발급 (Refresh Token Rotation)
    // ─────────────────────────────────────────────────────────────
    public TokenResponse refresh(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        if (isBlacklisted(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        String userId = jwtProvider.getUserId(refreshToken);

        // Redis 저장 토큰과 일치 확인
        String storedToken = redisTemplate.opsForValue()
                .get(REFRESH_TOKEN_PREFIX + userId);

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // UUID 변환은 반드시 findUserByIdString() 통해서 — 직접 호출 금지
        User user = findUserByIdString(userId);

        String newAccessToken  = jwtProvider.generateAccessToken(userId, user.getRole().name());
        String newRefreshToken = jwtProvider.generateRefreshToken(userId);

        // 기존 토큰 블랙리스트 등록 후 새 토큰 저장 (Rotation)
        blacklist(refreshToken);
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + userId,
                newRefreshToken,
                Duration.ofSeconds(jwtProvider.getRefreshExpiration())
        );

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(3600L)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // 로그아웃
    // ─────────────────────────────────────────────────────────────
    public void logout(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            // 만료된 토큰으로 로그아웃 시도도 정상 처리
            return;
        }

        String userId = jwtProvider.getUserId(refreshToken);
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
        blacklist(refreshToken);
    }

    // ─────────────────────────────────────────────────────────────
    // private 유틸
    // ─────────────────────────────────────────────────────────────

    /**
     * JWT subject(userId 문자열) → UUID 변환 후 User 조회
     *
     * UUID.fromString()은 형식이 맞지 않으면 IllegalArgumentException 던짐.
     * 직접 호출하면 GlobalExceptionHandler에서 500으로 처리됨.
     * 반드시 이 메서드를 통해서 조회해야 INVALID_TOKEN(401)으로 처리됨.
     */
    private User findUserByIdString(String userId) {
        try {
            return userRepository.findById(UUID.fromString(userId))
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        } catch (IllegalArgumentException e) {
            log.warn("JWT subject가 UUID 형식이 아닙니다: {}", userId);
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    private void blacklist(String token) {
        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + token,
                "1",
                Duration.ofSeconds(3600)
        );
    }

    private boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}