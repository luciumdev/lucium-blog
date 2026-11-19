package com.ysh.infra.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchConsistencyRunner {
    private final ReindexService reindexService;

    /**
     * 부팅 시 1회 — 코드 변경/배포 직후 즉시 drift 인지.
     */
    @Bean
    public ApplicationRunner searchConsistencyOnStartup() {
        return args -> runCheck("부팅 직후");
    }

    /**
     * 매일 03:00 KST 1회 — 운영 중 drift 모니터링.
     * 새벽 한가한 시간대 + 다른 cron(06:00 forex/indices)과 분리.
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void searchConsistencyScheduled() {
        runCheck("정기 점검(03:00 KST)");
    }

    private void runCheck(String trigger) {
        try {
            ReindexService.ConsistencyReport report = reindexService.checkConsistency();
            if (report.consistent()) {
                log.info("[{}] ES 정합성 OK — db={}, es={}",
                        trigger, report.dbCount(), report.esCount());
            } else {
                log.warn("[{}] ⚠️ ES 정합성 불일치 — db={}, es={}, missing={}, stale={}. " +
                                "POST /v1/admin/search/reindex 로 백필 필요.",
                        trigger, report.dbCount(), report.esCount(),
                        report.missingInEs().size(), report.staleInEs().size());
                if (!report.missingInEs().isEmpty()) {
                    log.warn("[{}] Missing sample (앞 10개): {}",
                            trigger, report.missingInEs().stream().limit(10).toList());
                }
                if (!report.staleInEs().isEmpty()) {
                    log.warn("[{}] Stale sample (앞 10개): {}",
                            trigger, report.staleInEs().stream().limit(10).toList());
                }
            }
        } catch (Exception e) {
            log.warn("[{}] ES 정합성 체크 실패 - msg={}", trigger, e.getMessage(), e);
        }
    }
}