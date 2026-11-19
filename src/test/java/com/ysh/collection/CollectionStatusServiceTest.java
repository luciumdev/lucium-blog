package com.ysh.collection;

import com.ysh.domain.collection.entity.CollectionJob;
import com.ysh.domain.collection.entity.CollectionRun;
import com.ysh.domain.collection.entity.RunOutcome;
import com.ysh.domain.collection.entity.RunStatus;
import com.ysh.domain.collection.entity.RunTrigger;
import com.ysh.domain.collection.repository.CollectionRunRepository;
import com.ysh.domain.collection.service.CollectionStatusService;
import com.ysh.domain.news.repository.NewsFeedRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CollectionStatusServiceTest {

    private final CollectionRunRepository runRepository = mock(CollectionRunRepository.class);
    private final NewsFeedRepository newsFeedRepository = mock(NewsFeedRepository.class);
    private final CollectionStatusService service = new CollectionStatusService(runRepository, newsFeedRepository);

    @Test
    void 성공률은_EMPTY_를_빼고_계산() {
        when(runRepository.countByStatusSince(any(), anyCollection())).thenReturn(List.of(
                count(RunStatus.SUCCESS, 18), count(RunStatus.FAILED, 1), count(RunStatus.PARTIAL, 1),
                count(RunStatus.EMPTY, 30)));

        assertThat(service.status().successRate7d()).isEqualTo(90.0);   // 18 / 20
    }

    @Test
    void 기록이_없으면_성공률_null() {
        when(runRepository.countByStatusSince(any(), anyCollection())).thenReturn(List.of(count(RunStatus.EMPTY, 5)));
        assertThat(service.status().successRate7d()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void 성공률과_작업_목록은_화면에_보이는_작업만_대상() {
        when(runRepository.countByStatusSince(any(), anyCollection())).thenReturn(List.of());

        var response = service.status();

        ArgumentCaptor<Collection<CollectionJob>> jobs = ArgumentCaptor.forClass(Collection.class);
        verify(runRepository).countByStatusSince(any(), jobs.capture());
        assertThat(jobs.getValue())
                .containsExactlyElementsOf(CollectionJob.displayedJobs())
                .doesNotContain(CollectionJob.COMMODITIES);
        assertThat(response.jobs()).extracting(j -> j.job()).doesNotContain("COMMODITIES");
        verify(runRepository, never()).findByJobOrderByStartedAtDesc(eq(CollectionJob.COMMODITIES), any());
    }

    @Test
    void 최근_실행은_과거에서_최신_순() {
        LocalDateTime now = LocalDateTime.now();
        CollectionRun newest = CollectionRun.of(CollectionJob.NEWS, RunTrigger.SCHEDULED, RunOutcome.failed("x"), now, now);
        CollectionRun older = CollectionRun.of(CollectionJob.NEWS, RunTrigger.SCHEDULED, RunOutcome.success(3), now.minusMinutes(30), now.minusMinutes(30));
        when(runRepository.countByStatusSince(any(), anyCollection())).thenReturn(List.of());
        when(runRepository.findByJobOrderByStartedAtDesc(eq(CollectionJob.NEWS), any())).thenReturn(List.of(newest, older));

        var news = service.status().jobs().get(0);

        assertThat(news.job()).isEqualTo("NEWS");
        assertThat(news.recent()).containsExactly(RunStatus.SUCCESS, RunStatus.FAILED);
        assertThat(news.lastStatus()).isEqualTo(RunStatus.FAILED);
    }

    private static CollectionRunRepository.StatusCount count(RunStatus status, long cnt) {
        return new CollectionRunRepository.StatusCount() {
            @Override public RunStatus getStatus() { return status; }
            @Override public long getCnt() { return cnt; }
        };
    }
}