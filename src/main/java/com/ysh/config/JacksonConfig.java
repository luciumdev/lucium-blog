package com.ysh.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    /**
     * Jackson 2 ObjectMapper 빈 명시 등록.
     *
     * Spring Boot 4.0부터 Jackson 3(JsonMapper)가 기본이 되면서
     * Jackson 2의 ObjectMapper는 자동 빈 등록되지 않음.
     * JwtAuthenticationEntryPoint / JwtAccessDeniedHandler 등 기존 코드가
     * Jackson 2 ObjectMapper에 의존하므로 명시 등록.
     *
     * 향후 Jackson 3(JsonMapper)으로 점진 마이그레이션 예정.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

}
