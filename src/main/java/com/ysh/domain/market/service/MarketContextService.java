package com.ysh.domain.market.service;

import com.ysh.domain.market.repository.IndexBarRepository;
import com.ysh.domain.market.repository.MarketSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * 뉴스 ↔ 지표 맥락 조회.
 *
 * <p>"이 기사가 나온 날 지표가 어땠나"를 보여주기 위한 읽기 전용 서비스.
 * 인과가 아니라 맥락(같은 날 등락)이다 — 화면 문구도 그렇게 표현할 것.
 *
 * <p>데이터: 지수 = index_bars 종가, 환율 = market_snapshots(FOREX) 일 마지막 스냅샷.
 * 등락률 = 해당 날짜(이전 가장 가까운 거래일 포함)의 종가 ÷ 직전 거래일 종가 − 1.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketContextService {

    public static final String USD = "USD";

    /** 주말·휴장일 기사는 직전 거래일 값을 쓰되, 이보다 오래된 값은 쓰지 않음 */
    private static final int MAX_STALE_DAYS = 4;
    /** 직전 거래일을 찾기 위한 여유 조회 기간 */
    private static final int LOOKBACK_PADDING_DAYS = 10;

    private final IndexBarRepository indexBarRepository;
    private final MarketSnapshotRepository snapshotRepository;

    /**
     * 기간 [from, to] 의 지표 시계열을 한 번에 읽어 lookup 객체로 반환.
     * 요청당 쿼리 2회 (지수 1 + 환율 1). 실패 시 빈 lookup — 뉴스 응답은 칩 없이 정상 반환.
     */
    @Transactional(readOnly = true)
    public Lookup load(LocalDate from, LocalDate to) {
        LocalDate since = from.minusDays(LOOKBACK_PADDING_DAYS);
        Map<String, NavigableMap<LocalDate, Double>> closes = new HashMap<>();
        try {
            for (IndexBarRepository.IndexBarPoint p : indexBarRepository.findPointsFrom(since)) {
                if (p.getBarDate().isAfter(to)) continue;
                closes.computeIfAbsent(p.getCode(), k -> new TreeMap<>()).put(p.getBarDate(), p.getClose());
            }
            for (MarketSnapshotRepository.DailyRate r :
                    snapshotRepository.findDailyClosingRates(since.atStartOfDay(), USD)) {
                if (r.getRate() <= 0 || r.getDay().isAfter(to)) continue;
                closes.computeIfAbsent(USD, k -> new TreeMap<>()).put(r.getDay(), r.getRate());
            }
        } catch (Exception e) {
            log.warn("지표 맥락 조회 실패 — 칩 없이 응답: {}", e.getMessage());
            return new Lookup(Map.of());
        }
        return new Lookup(closes);
    }

    /** code 별 일 종가 시계열 */
    public static final class Lookup {
        private final Map<String, NavigableMap<LocalDate, Double>> closes;

        Lookup(Map<String, NavigableMap<LocalDate, Double>> closes) {
            this.closes = closes;
        }

        /** 테스트·외부 조립용. 날짜 순서가 보장되도록 TreeMap 으로 복사 */
        public static Lookup of(Map<String, Map<LocalDate, Double>> closes) {
            Map<String, NavigableMap<LocalDate, Double>> copy = new HashMap<>();
            closes.forEach((code, series) -> copy.put(code, new TreeMap<>(series)));
            return new Lookup(copy);
        }

        /**
         * date 기준 등락률(%). date 이전 가장 가까운 거래일 값 사용.
         * 비어 있는 경우:
         *   - 데이터 없음 / 직전 거래일 없음
         *   - 기준 거래일이 date 보다 {@value #MAX_STALE_DAYS}일 넘게 오래됨
         *   - 종가가 직전과 완전히 같음 → 06:00 수집분이 직전 종가를 당일 날짜로 저장한 미확정 값일 가능성
         */
        public Optional<Double> changeRate(String code, LocalDate date) {
            NavigableMap<LocalDate, Double> series = closes.get(code);
            if (series == null) return Optional.empty();

            Map.Entry<LocalDate, Double> current = series.floorEntry(date);
            if (current == null || current.getKey().isBefore(date.minusDays(MAX_STALE_DAYS))) {
                return Optional.empty();
            }
            Map.Entry<LocalDate, Double> previous = series.lowerEntry(current.getKey());
            if (previous == null || previous.getValue() == 0) return Optional.empty();
            if (Double.compare(current.getValue(), previous.getValue()) == 0) return Optional.empty();

            double rate = (current.getValue() / previous.getValue() - 1) * 100;
            return Optional.of(Math.round(rate * 100) / 100.0);
        }
    }
}
