package com.ysh.domain.collection.service;

import com.ysh.domain.collection.dto.CollectionStatusResponse;
import com.ysh.domain.collection.dto.CollectionStatusResponse.JobStatus;
import com.ysh.domain.collection.entity.CollectionJob;
import com.ysh.domain.collection.entity.CollectionRun;
import com.ysh.domain.collection.entity.RunStatus;
import com.ysh.domain.collection.repository.CollectionRunRepository;
import com.ysh.domain.news.entity.NewsCategory;
import com.ysh.domain.news.repository.NewsFeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 수집 현황 조회 (read model 조립).
 * 쿼리: 기사 수 2 + 상태별 집계 1 + 작업별 최근 실행 4 = 7회. 모두 인덱스 범위 조회라 가볍다.
 * 작업 목록·성공률 모두 화면에 보이는 작업({@link CollectionJob#displayedJobs()})만 대상으로 한다.
 */
@Service
@RequiredArgsConstructor
public class CollectionStatusService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final List<NewsCategory> ARTICLE_CATEGORIES = List.of(NewsCategory.IT, NewsCategory.ECONOMY);

    private final CollectionRunRepository runRepository;
    private final NewsFeedRepository newsFeedRepository;

    @Transactional(readOnly = true)
    public CollectionStatusResponse status() {
        LocalDateTime todayStart = LocalDate.now(KST).atStartOfDay();
        List<CollectionJob> displayed = CollectionJob.displayedJobs();

        long articles = newsFeedRepository.countFetchedSince(todayStart, ARTICLE_CATEGORIES);
        long translated = newsFeedRepository.countTranslatedSince(todayStart);
        Double rate = successRate(runRepository.countByStatusSince(LocalDateTime.now().minusDays(7), displayed));

        List<JobStatus> jobs = new ArrayList<>(displayed.size());
        for (CollectionJob job : displayed) {
            List<CollectionRun> runs = runRepository.findByJobOrderByStartedAtDesc(
                    job, PageRequest.of(0, job.recentSize()));
            CollectionRun last = runs.isEmpty() ? null : runs.get(0);
            List<RunStatus> recent = new ArrayList<>(runs.stream().map(CollectionRun::getStatus).toList());
            Collections.reverse(recent);   // 과거 → 최신
            jobs.add(new JobStatus(
                    job.name(), job.label(), job.cycle(),
                    last == null ? null : last.getStartedAt(),
                    last == null ? null : last.getStatus(),
                    recent));
        }
        return new CollectionStatusResponse(articles, translated, rate, jobs);
    }

    /** SUCCESS ÷ (전체 − EMPTY) × 100, 소수 1자리. 분모 0 이면 null */
    static Double successRate(List<CollectionRunRepository.StatusCount> counts) {
        long success = 0;
        long total = 0;
        for (CollectionRunRepository.StatusCount c : counts) {
            if (c.getStatus() == RunStatus.EMPTY) continue;
            total += c.getCnt();
            if (c.getStatus() == RunStatus.SUCCESS) success += c.getCnt();
        }
        if (total == 0) return null;
        return Math.round(success * 1000.0 / total) / 10.0;
    }
}
