package com.ysh.domain.user.config;

import com.ysh.domain.user.entity.User;
import com.ysh.domain.user.entity.UserRole;
import com.ysh.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * prod 부팅 시 admin 계정 1회 seed.
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminSeedProperties props;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(props.email()) || !StringUtils.hasText(props.password())) {
            log.warn("Admin seed skipped — ADMIN_SEED_EMAIL/PASSWORD not configured.");
            return;
        }

        if (userRepository.existsByEmail(props.email())) {
            log.info("Admin seed skipped — account already exists.");
            return;
        }

        User admin = User.builder()
                .email(props.email())
                .password(passwordEncoder.encode(props.password()))
                .nickname(StringUtils.hasText(props.nickname()) ? props.nickname() : "admin")
                .role(UserRole.ADMIN)
                .build();
        userRepository.save(admin);
        log.info("Admin account seeded. email={}", props.email());
    }
}