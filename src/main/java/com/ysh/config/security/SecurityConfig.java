package com.ysh.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter      jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint  jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler       jwtAccessDeniedHandler;

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String[] allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(ct -> {})
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                // [수정] permissionsPolicy() deprecated (Spring Security 7) → addHeaderWriter로 직접 추가
                .addHeaderWriter((request, response) ->
                    response.setHeader("Permissions-Policy",
                        "camera=(), microphone=(), geolocation=()"))
            )

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler))

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET,
                    "/v1/posts",
                    "/v1/posts/{publicId}",
                    "/v1/posts/{publicId}/comments",
                    "/v1/categories",
                    "/v1/tags",
                    "/v1/search",
                    "/v1/stat/posts/popular",
                    "/v1/stat/site",
                    "/v1/stat/collection",
                    "/v1/stat/collection/**",
                    "/v1/rss",
                    "/v1/stat/tags",
                    "/v1/market/**"
                ).permitAll()
                .requestMatchers(HttpMethod.POST,
                    "/v1/auth/login",
                    "/v1/auth/refresh",
                    "/v1/posts/{publicId}/comments",
                    "/v1/stat/view/{publicId}",
                    "/v1/stat/visit",
                    "/v1/market/admin/news/collect"
                ).permitAll()
                .requestMatchers(HttpMethod.DELETE,
                    "/v1/comments/{publicId}"
                ).permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/v1/market/indices",
                    "/v1/market/forex",
                    "/v1/market/commodities",
                    "/v1/market/news"
                ).permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/v1/ai-journal/briefing",
                    "/v1/ai-journal/dates"
                ).permitAll()
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // 없는 경로(404)·서버 오류(500)가 /error 로 포워드될 때 401 로 덮이지 않게
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated()
            )

            .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins));
        config.setAllowedMethods(
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(
            List.of("Authorization", "Content-Type", "X-Requested-With", "X-Refresh-Token"));
        config.setExposedHeaders(List.of("Authorization", "X-Cache"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
