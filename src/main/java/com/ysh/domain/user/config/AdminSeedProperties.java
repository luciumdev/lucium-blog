package com.ysh.domain.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * admin seed 설정. prod에서 환경변수로 주입.
 * ADMIN_SEED_EMAIL / ADMIN_SEED_PASSWORD / ADMIN_SEED_NICKNAME
 */
@ConfigurationProperties(prefix = "admin.seed")
public record AdminSeedProperties(
        String email,
        String password,
        String nickname
) {}