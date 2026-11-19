package com.ysh.domain.admin.controller;

import com.ysh.common.response.ApiResponse;
import com.ysh.domain.admin.dto.PostStatsResponse;
import com.ysh.domain.admin.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<PostStatsResponse>> getPostStats() {
        return ResponseEntity.ok(ApiResponse.ok(adminStatsService.getPostStats()));
    }
}