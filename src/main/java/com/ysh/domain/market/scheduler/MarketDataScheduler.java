package com.ysh.domain.market.scheduler;

import com.ysh.domain.collection.entity.RunTrigger;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 마켓·뉴스 수집 주기 정의. 작업 본문과 실행 기록은 {@link MarketCollectionJobs}.
 */
@Component
@RequiredArgsConstructor
public class MarketDataScheduler {

    private final MarketCollectionJobs jobs;

    /**
     * 평일 09~18시 매시 정각 — 환율.
     * 수출입은행 고시는 영업일 오전(11시 전후)에 한 번 나오므로 영업시간 안에서만 확인.
     */
    @Scheduled(cron = "0 0 9-18 * * MON-FRI", zone = "Asia/Seoul")
    public void collectForex() {
        jobs.forex(RunTrigger.SCHEDULED);
    }

    /** 30분마다 — 수집 → 저장 → 보강(번역·키워드) → ES 재색인 → 캐시 */
    @Scheduled(cron = "0 */30 * * * *", zone = "Asia/Seoul")
    public void collectNews() {
        jobs.news(RunTrigger.SCHEDULED);
    }

    /**
     * 화~토 06:00 KST — 4개 지수 수집.
     * 이 시각은 미국장 마감 직후라 해외(QQQ/SPY) 확정 종가 확보에 적합.
     * 토요일 06:00 = 미국 금요일 종가, 일·월 06:00 은 새 데이터가 없어 제외.
     * 단, 한국장 개장(09:00) 전이라 국내는 직전 종가가 당일 날짜로 저장됨(미확정).
     * → 국내 당일 확정 종가는 아래 collectIndicesAfterKrClose(16:00)에서 보정.
     */
    @Scheduled(cron = "0 0 6 * * TUE-SAT", zone = "Asia/Seoul")
    public void collectIndices() {
        jobs.indices(RunTrigger.SCHEDULED);
    }

    /**
     * 월~금 16:00 KST — 국내장 마감(15:30) 후 재수집.
     * 06:00 수집 시 국내가 장 전이라 직전 종가를 당일 날짜로 받는 문제 보정.
     * 이 시각 미국장은 개장 전이라 해외는 06:00과 동일한 확정 데이터.
     */
    @Scheduled(cron = "0 0 16 * * MON-FRI", zone = "Asia/Seoul")
    public void collectIndicesAfterKrClose() {
        jobs.indices(RunTrigger.SCHEDULED);
    }

    /** 화~토 06:00 KST — 원자재 (미국 기준 일간 종가, 토요일 = 금요일분) */
    @Scheduled(cron = "0 0 6 * * TUE-SAT", zone = "Asia/Seoul")
    public void collectCommodities() {
        jobs.commodities(RunTrigger.SCHEDULED);
    }
}
