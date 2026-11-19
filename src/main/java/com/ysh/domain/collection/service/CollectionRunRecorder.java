package com.ysh.domain.collection.service;

import com.ysh.domain.collection.entity.*;
import com.ysh.domain.collection.repository.CollectionRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * 수집 작업 실행 + 결과 기록.
 *
 * <ul>
 *   <li>작업 예외는 여기서 잡아 FAILED 로 기록 — 스케줄러 스레드로 예외가 새지 않는다.</li>
 *   <li>기록 저장 실패는 로그만 남긴다 — 기록 때문에 수집이 실패로 바뀌면 안 됨.</li>
 *   <li>작업은 트랜잭션 밖에서 실행된다. 저장은 리포지토리 기본 트랜잭션으로 짧게.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectionRunRecorder {

    private final CollectionRunRepository repository;

    public RunOutcome run(CollectionJob job, RunTrigger trigger, Supplier<RunOutcome> task) {
        LocalDateTime startedAt = LocalDateTime.now();
        RunOutcome outcome;
        try {
            outcome = task.get();
            if (outcome == null) outcome = RunOutcome.failed("결과 없음");
        } catch (Exception e) {
            log.error("수집 작업 실패. job={}, trigger={}", job, trigger, e);
            outcome = RunOutcome.failed(e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        try {
            repository.save(CollectionRun.of(job, trigger, outcome, startedAt, LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("수집 기록 저장 실패. job={}, status={}, cause={}", job, outcome.status(), e.getMessage());
        }

        log.info("수집 작업 종료. job={}, trigger={}, status={}, items={}{}",
                job, trigger, outcome.status(), outcome.itemCount(),
                outcome.message() == null ? "" : ", message=" + outcome.message());
        return outcome;
    }
}
