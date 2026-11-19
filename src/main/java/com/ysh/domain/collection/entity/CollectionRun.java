package com.ysh.domain.collection.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 스케줄러 실행 1회 기록 (append-only) */
@Entity
@Table(name = "collection_runs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionRun {

    private static final int MESSAGE_MAX = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CollectionJob job;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    private RunTrigger trigger;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RunStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at", nullable = false)
    private LocalDateTime finishedAt;

    @Column(name = "item_count", nullable = false)
    private int itemCount;

    @Column(length = MESSAGE_MAX)
    private String message;

    public static CollectionRun of(CollectionJob job, RunTrigger trigger, RunOutcome outcome,
                                   LocalDateTime startedAt, LocalDateTime finishedAt) {
        CollectionRun run = new CollectionRun();
        run.job = job;
        run.trigger = trigger;
        run.status = outcome.status();
        run.itemCount = outcome.itemCount();
        run.message = truncate(outcome.message());
        run.startedAt = startedAt;
        run.finishedAt = finishedAt;
        return run;
    }

    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() <= MESSAGE_MAX ? s : s.substring(0, MESSAGE_MAX);
    }
}
