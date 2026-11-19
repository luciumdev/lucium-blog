package com.ysh.config.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * MVC 설정
 *
 * CORS 주의:
 *   Spring Security 환경에서는 SecurityConfig의 CorsConfigurationSource가 우선 적용됨.
 *   addCorsMappings()는 Security 필터 이후에 동작하므로 여기서 CORS 설정 금지.
 *   CORS는 SecurityConfig.corsConfigurationSource() 에서 단일 관리.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final VisitorIdArgumentResolver visitorIdArgumentResolver;
    /**
     * 페이지네이션 기본값
     * - 기본 페이지 크기: 100
     * - 파라미터명: page, size, sort (Spring 기본값 유지)
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        PageableHandlerMethodArgumentResolver pageable = new PageableHandlerMethodArgumentResolver();
        pageable.setMaxPageSize(100);
        resolvers.add(pageable);
        resolvers.add(visitorIdArgumentResolver);
    }
}
