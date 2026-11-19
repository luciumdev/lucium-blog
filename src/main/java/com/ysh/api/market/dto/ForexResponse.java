package com.ysh.api.market.dto;

import com.ysh.domain.market.domain.ForexRate;
import com.ysh.domain.market.domain.ForexRates;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record ForexResponse (
        LocalDateTime snapshotAt,
        List<ForexRateResponse> rates
) {
    public static ForexResponse from(ForexRates today, ForexRates yesterday) {
        Map<String, Double> yesterdayMap = yesterday.isEmpty()
                ? Map.of()
                : yesterday.rates().stream()
                  .collect(Collectors.toMap(
                          ForexRate::currencyCode,
                          ForexRate::dealBasR
                  ));
        return new ForexResponse(
                today.snapshotAt(),
                today.rates().stream()
                        .map(r -> ForexRateResponse.from(r, yesterdayMap.get(r.currencyCode())))
                        .toList()
        );
    }

    public record ForexRateResponse(
            String currencyCode,
            String currencyName,
            double rate,
            Double previousRate,    // null 가능 (어제 데이터 없음)
            Double change,          // null 가능
            Double changeRate       // null 가능 — % 단위
    ) {
        public static ForexRateResponse from(ForexRate today, Double yesterdayRate) {
            if (yesterdayRate == null) {
                return new ForexRateResponse(
                        today.currencyCode(),
                        today.currencyName(),
                        today.dealBasR(),
                        null, null, null
                );
            }
            double change = today.dealBasR() - yesterdayRate;
            double changeRate = (change / yesterdayRate) * 100.0;
            return new ForexRateResponse(
                    today.currencyCode(),
                    today.currencyName(),
                    today.dealBasR(),
                    yesterdayRate,
                    roundTo2(change),
                    roundTo2(changeRate)
            );
        }
    }

    /**
     * 2자리 반올림. 통화 change/등락률 표시 정밀도
     * @param value
     * @return
     */
    private static double roundTo2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
