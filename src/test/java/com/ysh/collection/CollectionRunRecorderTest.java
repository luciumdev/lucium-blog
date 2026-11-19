package com.ysh.collection;

import com.ysh.domain.collection.entity.*;
import com.ysh.domain.collection.repository.CollectionRunRepository;
import com.ysh.domain.collection.service.CollectionRunRecorder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CollectionRunRecorderTest {

    private final CollectionRunRepository repository = mock(CollectionRunRepository.class);
    private final CollectionRunRecorder recorder = new CollectionRunRecorder(repository);

    @Test
    void 작업_결과를_그대로_기록() {
        RunOutcome outcome = recorder.run(CollectionJob.NEWS, RunTrigger.SCHEDULED, () -> RunOutcome.success(12));

        CollectionRun saved = captureSaved();
        assertThat(outcome.status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(saved.getJob()).isEqualTo(CollectionJob.NEWS);
        assertThat(saved.getItemCount()).isEqualTo(12);
        assertThat(saved.getFinishedAt()).isAfterOrEqualTo(saved.getStartedAt());
    }

    @Test
    void 작업_예외는_FAILED_로_기록하고_밖으로_던지지_않음() {
        RunOutcome outcome = recorder.run(CollectionJob.FOREX, RunTrigger.BOOTSTRAP, () -> {
            throw new IllegalStateException("boom");
        });

        assertThat(outcome.status()).isEqualTo(RunStatus.FAILED);
        assertThat(captureSaved().getMessage()).contains("IllegalStateException").contains("boom");
    }

    @Test
    void 기록_저장이_실패해도_작업_결과는_반환() {
        when(repository.save(any())).thenThrow(new RuntimeException("db down"));

        RunOutcome outcome = recorder.run(CollectionJob.INDICES, RunTrigger.SCHEDULED, () -> RunOutcome.partial(3, "KOSDAQ"));

        assertThat(outcome.status()).isEqualTo(RunStatus.PARTIAL);
        assertThat(outcome.itemCount()).isEqualTo(3);
    }

    @Test
    void 긴_메시지는_500자로_자름() {
        recorder.run(CollectionJob.NEWS, RunTrigger.SCHEDULED, () -> RunOutcome.failed("x".repeat(800)));
        assertThat(captureSaved().getMessage()).hasSize(500);
    }

    private CollectionRun captureSaved() {
        ArgumentCaptor<CollectionRun> captor = ArgumentCaptor.forClass(CollectionRun.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }
}
