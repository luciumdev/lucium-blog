package com.ysh.domain.collection.repository;

import com.ysh.domain.collection.entity.CollectionJob;
import com.ysh.domain.collection.entity.CollectionRun;
import com.ysh.domain.collection.entity.RunStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 실행 로그 필터(작업·상태)는 JpaSpecificationExecutor 로 조립.
 * (JPQL nullable 파라미터 패턴은 PostgreSQL 타입 추론 오류 이력이 있어 사용하지 않음)
 */
public interface CollectionRunRepository extends JpaRepository<CollectionRun, Long>,
        JpaSpecificationExecutor<CollectionRun> {

    /** 작업별 최근 실행 — 최신순 (idx_collection_runs_job_started) */
    List<CollectionRun> findByJobOrderByStartedAtDesc(CollectionJob job, Pageable pageable);

    /** 기간 내 상태별 건수 — 성공률 계산용. jobs 는 비어 있으면 안 됨 (IN () 오류) */
    @Query("""
        SELECT r.status AS status, COUNT(r) AS cnt
          FROM CollectionRun r
         WHERE r.startedAt >= :since
           AND r.job IN :jobs
         GROUP BY r.status""")
    List<StatusCount> countByStatusSince(@Param("since") LocalDateTime since,
                                         @Param("jobs") Collection<CollectionJob> jobs);

    /** 일간 요약용 — 메시지 컬럼은 빼고 필요한 4개만 (idx_collection_runs_started) */
    @Query("""
        SELECT r.job AS job, r.status AS status, r.startedAt AS startedAt, r.finishedAt AS finishedAt
          FROM CollectionRun r
         WHERE r.startedAt >= :since""")
    List<DailyRow> findDailyRowsSince(@Param("since") LocalDateTime since);

    /** 보관 기간 지난 기록 일괄 삭제 */
    @Modifying
    @Transactional
    @Query("DELETE FROM CollectionRun r WHERE r.startedAt < :before")
    int deleteStartedBefore(@Param("before") LocalDateTime before);

    interface StatusCount {
        RunStatus getStatus();
        long getCnt();
    }

    interface DailyRow {
        CollectionJob getJob();
        RunStatus getStatus();
        LocalDateTime getStartedAt();
        LocalDateTime getFinishedAt();
    }
}
