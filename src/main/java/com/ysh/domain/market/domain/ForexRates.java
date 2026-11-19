package com.ysh.domain.market.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.Comparator;          // ← 추가
import java.util.List;

/**
 * 주의) JPY는 100엔당 가격임 -> front에서 처리
 * 역직렬화
 * @param snapshotAt
 * @param rates
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ForexRates(
        LocalDateTime snapshotAt,
        List<ForexRate> rates
) {
    /**
     * 메인 통화 우선순위. UI 일관성을 위해 도메인 차원에서 고정
     */
    private static final List<String> PRIORITY = List.of("USD", "EUR", "JPY(100)", "CNH");

    public static ForexRates empty() {
        return new ForexRates(LocalDateTime.now(), List.of());
    }

    @JsonIgnore
    public boolean isEmpty() { return rates == null || rates.isEmpty(); }

    /**
     * 도메인 우선순위(USD → EUR → JPY → CNH) 순으로 정렬한 새 ForexRates 반환.
     */
    public ForexRates sortedByPriority() {
        if (isEmpty()) return this;
        List<ForexRate> sorted = rates.stream()
                .sorted(Comparator.comparingInt(r -> {
                    int idx = PRIORITY.indexOf(r.currencyCode());
                    return idx >= 0 ? idx : Integer.MAX_VALUE;
                }))
                .toList();
        return new ForexRates(snapshotAt, sorted);
    }
}