package com.ysh.domain.collection.entity;

import java.util.Arrays;
import java.util.List;

/**
 * 기록 대상 수집 작업.
 * @param label      화면 표시명
 * @param cycle      실행 주기 (화면 표시용)
 * @param recentSize 메인 카드에 칸으로 보여줄 최근 실행 수
 * @param displayed  화면(메인 카드·/status·성공률) 노출 여부.
 *                   false 여도 수집·기록은 계속되며, 기존 기록을 읽어야 하므로 enum 값은 지우지 않는다.
 */
public enum CollectionJob {
    NEWS("뉴스", "30분", 24, true),
    FOREX("환율", "평일 매시", 20, true),
    INDICES("지수", "평일 2번", 14, true),
    AI_JOURNAL("AI 릴리즈 노트", "하루 1번", 14, true),
    COMMODITIES("원자재", "평일 1번", 14, false);   // 화면에서 제외됨 (2026-10)

    private static final List<CollectionJob> DISPLAYED =
            Arrays.stream(values()).filter(j -> j.displayed).toList();

    private final String label;
    private final String cycle;
    private final int recentSize;
    private final boolean displayed;

    CollectionJob(String label, String cycle, int recentSize, boolean displayed) {
        this.label = label;
        this.cycle = cycle;
        this.recentSize = recentSize;
        this.displayed = displayed;
    }

    public String label() { return label; }
    public String cycle() { return cycle; }
    public int recentSize() { return recentSize; }
    public boolean displayed() { return displayed; }

    /** 화면에 보여줄 작업 — 선언 순서 유지 (불변 목록) */
    public static List<CollectionJob> displayedJobs() {
        return DISPLAYED;
    }
}
