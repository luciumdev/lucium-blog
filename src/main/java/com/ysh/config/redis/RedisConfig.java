package com.ysh.config.redis;

import org.springframework.context.annotation.Configuration;


/**
 * Redis 설정
 *
 * StringRedisTemplate은 spring-boot-starter-data-redis 의존성이 있으면
 * Spring Boot가 자동으로 빈 등록해줌 → 수동 선언 불필요
 *
 * @EnableCaching: @Cacheable, @CacheEvict 등 캐시 애노테이션 활성화
 */
@Configuration
public class RedisConfig {
}
