package com.ysh.domain.collection.entity;

/**
 * 수집 작업 1회의 결과. 각 작업이 직접 판단해 반환한다.
 * @param itemCount 새로 저장된 건수
 * @param message   PARTIAL·EMPTY·FAILED 사유 (SUCCESS 는 null)
 */
public record RunOutcome(RunStatus status, int itemCount, String message) {

    public static RunOutcome success(int itemCount) {
        return new RunOutcome(RunStatus.SUCCESS, itemCount, null);
    }

    public static RunOutcome partial(int itemCount, String reason) {
        return new RunOutcome(RunStatus.PARTIAL, itemCount, reason);
    }

    public static RunOutcome empty(String reason) {
        return new RunOutcome(RunStatus.EMPTY, 0, reason);
    }

    public static RunOutcome failed(String reason) {
        return new RunOutcome(RunStatus.FAILED, 0, reason);
    }
}
