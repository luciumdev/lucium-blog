package com.ysh.domain.stat.controller;

import com.ysh.common.annotation.VisitorId;
import com.ysh.common.response.ApiResponse;
import com.ysh.domain.post.dto.PostSummaryResponse;
import com.ysh.domain.stat.dto.PostStatResponse;
import com.ysh.domain.stat.dto.SiteStatResponse;
import com.ysh.domain.stat.service.StatService;
import com.ysh.domain.stat.service.VisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/stat")
@RequiredArgsConstructor
public class StatController {

    private final StatService statService;
    private final VisitService visitService;

    // 인증 필요 — SecurityConfig permitAll 목록에 없으므로 authenticated() 적용
    @GetMapping("/posts/summary")
    public ResponseEntity<ApiResponse<PostStatResponse>> getPostSummary() {
        return ResponseEntity.ok(ApiResponse.ok(statService.getPostSummary()));
    }

    // 공개 — SecurityConfig permitAll: /v1/stat/site
    @GetMapping("/site")
    public ResponseEntity<ApiResponse<SiteStatResponse>> getSiteStat() {
        return ResponseEntity.ok(ApiResponse.ok(statService.getSiteStat()));
    }

    // 공개 — SecurityConfig permitAll(POST): /v1/stat/visit. visitor_id 쿠키 기준 순 방문 기록
    @PostMapping("/visit")
    public ResponseEntity<Void> recordVisit(@VisitorId UUID visitorId) {
        visitService.record(visitorId);
        return ResponseEntity.noContent().build();
    }

    // 공개 — SecurityConfig permitAll: /v1/stat/posts/popular (기존 등록됨)
    @GetMapping("/posts/popular")
    public ResponseEntity<ApiResponse<List<PostSummaryResponse>>> getPopularPosts(
            @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(ApiResponse.ok(statService.getPopularPosts(size)));
    }
}
