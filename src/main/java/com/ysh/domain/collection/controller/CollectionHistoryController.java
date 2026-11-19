package com.ysh.domain.collection.controller;

import com.ysh.common.response.ApiResponse;
import com.ysh.domain.collection.dto.CollectionDailyResponse;
import com.ysh.domain.collection.dto.CollectionRunPageResponse;
import com.ysh.domain.collection.entity.CollectionJob;
import com.ysh.domain.collection.entity.RunStatus;
import com.ysh.domain.collection.service.CollectionHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * /status 실행 이력 — 공개 (SecurityConfig permitAll: /v1/stat/collection/**).
 * 관리자 토큰이 있으면 실행 로그에 원문(rawMessage)을 함께 내려준다.
 */
@RestController
@RequestMapping("/v1/stat/collection")
@RequiredArgsConstructor
public class CollectionHistoryController {

    private final CollectionHistoryService historyService;

    /** 실행 로그 — 관리자 여부에 따라 응답이 달라지므로 공용 캐시 금지 */
    @GetMapping("/runs")
    public ResponseEntity<ApiResponse<CollectionRunPageResponse>> runs(
            @RequestParam(required = false) CollectionJob job,
            @RequestParam(required = false) RunStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(historyService.runs(job, status, page, size, isAdmin())));
    }

    /** 작업별 일간 요약 — 인증과 무관, 1분 캐시 */
    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<CollectionDailyResponse>> daily(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(1)).cachePublic())
                .body(ApiResponse.ok(historyService.daily(days)));
    }

    private static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
                && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
