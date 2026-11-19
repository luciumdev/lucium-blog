package com.ysh.domain.collection.service;

import com.ysh.domain.collection.repository.CollectionRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 수집 기록 보관 기간 관리 — 90일 지난 행 삭제 (매일 03:10 KST) */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectionRunRetention {

    static final int RETENTION_DAYS = 90;

    private final CollectionRunRepository repository;

    @Scheduled(cron = "0 10 3 * * *", zone = "Asia/Seoul")
    public void purge() {
        try {
            int deleted = repository.deleteStartedBefore(LocalDateTime.now().minusDays(RETENTION_DAYS));
            if (deleted > 0) log.info("수집 기록 정리. deleted={}", deleted);
        } catch (Exception e) {
            log.warn("수집 기록 정리 실패: {}", e.getMessage());
        }
    }
}
