package com.ysh.domain.collection.entity;

/**
 * SUCCESS  — 정상
 * PARTIAL  — 일부만 성공 (예: 지수 4개 중 3개, 뉴스 수집은 됐지만 번역 단계 실패)
 * EMPTY    — 오류는 아니지만 받을 데이터가 없음 (주말·공휴일 환율 등). 성공률 계산에서 제외
 * FAILED   — 실패
 */
public enum RunStatus {
    SUCCESS, PARTIAL, EMPTY, FAILED
}
