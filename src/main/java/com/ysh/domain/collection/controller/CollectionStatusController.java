package com.ysh.domain.collection.controller;

import com.ysh.domain.collection.dto.CollectionStatusResponse;
import com.ysh.domain.collection.service.CollectionStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class CollectionStatusController {

    private final CollectionStatusService statusService;

    /** 메인 "데이터 수집" 카드. 브라우저 1분 캐시 (가장 짧은 수집 주기가 30분이라 충분) */
    @GetMapping("/v1/stat/collection")
    public ResponseEntity<CollectionStatusResponse> status() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(1)).cachePublic())
                .body(statusService.status());
    }
}
