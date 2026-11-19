package com.ysh.domain.collection.service;

import com.ysh.domain.collection.dto.CollectionDailyResponse;
import com.ysh.domain.collection.dto.CollectionDailyResponse.Day;
import com.ysh.domain.collection.dto.CollectionDailyResponse.JobDaily;
import com.ysh.domain.collection.dto.CollectionRunPageResponse;
import com.ysh.domain.collection.entity.CollectionJob;
import com.ysh.domain.collection.entity.CollectionRun;
import com.ysh.domain.collection.entity.RunStatus;
import com.ysh.domain.collection.repository.CollectionRunRepository;
import com.ysh.domain.collection.repository.CollectionRunRepository.DailyRow;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * /status 실행 이력 조회 (read model).
 *
 * <p>날짜 기준은 서버 기본 시간대 — 기록(CollectionRunRecorder)도 LocalDateTime.now() 로 남기므로
 * 같은 기준으로 묶어야 칸이 하루씩 밀리지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionHistoryService {

    /** collection_runs 보관 기간(CollectionRunRetention)과 같게 */
    private static final int MAX_DAYS = 90;
    private static final int MAX_PAGE_SIZE = 100;

    /** 화면에 보여줄 작업만 — 원자재는 수집·기록은 계속되지만 화면에서 제외 (CollectionJob.displayed) */
    private static final List<CollectionJob> VISIBLE_JOBS = CollectionJob.displayedJobs();

    private final CollectionRunRepository runRepository;

    /** 실행 로그 — 최신순. job·status 는 null 이면 전체 */
    public CollectionRunPageResponse runs(CollectionJob job, RunStatus status, int page, int size, boolean includeRaw) {
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.clamp(size, 1, MAX_PAGE_SIZE),
                Sort.by(Sort.Order.desc("startedAt"), Sort.Order.desc("id")));

        Page<CollectionRun> result = runRepository.findAll(filter(job, status), pageable);

        List<CollectionRunPageResponse.Item> items = result.getContent().stream()
                .map(run -> toItem(run, includeRaw))
                .toList();
        return new CollectionRunPageResponse(
                items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    /** 작업별 최근 N일 — 하루 단위 상태별 건수 + 성공률 + 평균 소요 */
    public CollectionDailyResponse daily(int days) {
        int d = Math.clamp(days, 1, MAX_DAYS);
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(d - 1L);

        List<DailyRow> rows = runRepository.findDailyRowsSince(from.atStartOfDay());

        Map<CollectionJob, Acc> byJob = new EnumMap<>(CollectionJob.class);
        for (DailyRow row : rows) {
            if (!VISIBLE_JOBS.contains(row.getJob())) continue;
            byJob.computeIfAbsent(row.getJob(), k -> new Acc()).add(row);
        }

        List<JobDaily> jobs = new ArrayList<>();
        for (CollectionJob job : VISIBLE_JOBS) {
            Acc acc = byJob.getOrDefault(job, new Acc());
            List<Day> daily = new ArrayList<>(d);
            for (LocalDate date = from; !date.isAfter(today); date = date.plusDays(1)) {
                int[] c = acc.perDay.getOrDefault(date, new int[4]);
                daily.add(new Day(date.toString(),
                        c[RunStatus.SUCCESS.ordinal()], c[RunStatus.PARTIAL.ordinal()],
                        c[RunStatus.EMPTY.ordinal()], c[RunStatus.FAILED.ordinal()]));
            }
            jobs.add(new JobDaily(
                    job.name(), job.label(), job.cycle(),
                    acc.total, acc.successRate(), acc.avgDurationMs(), daily));
        }
        return new CollectionDailyResponse(d, jobs);
    }

    /* ─────────────── helpers ─────────────── */

    private static Specification<CollectionRun> filter(CollectionJob job, RunStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>(3);
            predicates.add(root.get("job").in(VISIBLE_JOBS));   // 숨긴 작업은 job 파라미터로 요청해도 빈 결과
            if (job != null) predicates.add(cb.equal(root.get("job"), job));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static CollectionRunPageResponse.Item toItem(CollectionRun run, boolean includeRaw) {
        return new CollectionRunPageResponse.Item(
                run.getId(),
                run.getJob().name(),
                run.getJob().label(),
                run.getTrigger(),
                run.getStatus(),
                run.getStartedAt(),
                durationMs(run.getStartedAt(), run.getFinishedAt()),
                run.getItemCount(),
                RunReasonClassifier.toPublic(run.getStatus(), run.getMessage()),
                includeRaw ? run.getMessage() : null);
    }

    private static long durationMs(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0L;
        return Math.max(0L, Duration.between(start, end).toMillis());
    }

    /** 작업 1개의 기간 누적값. 상태 배열 인덱스 = RunStatus.ordinal() */
    private static final class Acc {
        private final Map<LocalDate, int[]> perDay = new HashMap<>();
        private int total;
        private int success;
        private int empty;
        private long durationSum;

        void add(DailyRow row) {
            RunStatus status = row.getStatus();
            perDay.computeIfAbsent(row.getStartedAt().toLocalDate(), k -> new int[4])[status.ordinal()]++;
            total++;
            if (status == RunStatus.SUCCESS) success++;
            if (status == RunStatus.EMPTY) empty++;
            durationSum += durationMs(row.getStartedAt(), row.getFinishedAt());
        }

        /** SUCCESS ÷ (전체 − EMPTY) × 100, 소수 1자리 — 메인 카드(CollectionStatusService)와 같은 식 */
        Double successRate() {
            int denominator = total - empty;
            if (denominator == 0) return null;
            return Math.round(success * 1000.0 / denominator) / 10.0;
        }

        Long avgDurationMs() {
            return total == 0 ? null : durationSum / total;
        }
    }
}
