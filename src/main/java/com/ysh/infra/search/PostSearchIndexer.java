package com.ysh.infra.search;

import com.ysh.domain.post.event.PostPublishedEvent;
import com.ysh.domain.post.event.PostUnpublishedEvent;
import com.ysh.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Post CRUD → ES 인덱스 동기화.
 *
 * 흐름: postService.publish → publishEvent → DB 트랜잭션 커밋 → AFTER_COMMIT → @Async
 * 이벤트 분리 — publish/재인덱싱은 {@link PostPublishedEvent}, unpublish/삭제는 {@link PostUnpublishedEvent}.
 * 단일 이벤트로 합치면 같은 인덱서의 save/delete가 동시 호출되는 race condition 발생.
 *
 * 실패 시 graceful — log.error만 남기고 swallow. 일관성은 SearchConsistencyRunner가 백그라운드로 보정.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostSearchIndexer {

    private final PostRepository postRepository;
    private final PostSearchRepository postSearchRepository;

    @Async("esIndexExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onPostPublished(PostPublishedEvent event) {
        try {
            postRepository.findByPublicId(event.publicId())
                    .map(PostSearchDocument::from)
                    .ifPresentOrElse(doc -> {
                                postSearchRepository.save(doc);
                                log.info("ES 색인 완료 - publicId={}", event.publicId());
                            },
                            () -> log.warn("ES 색인 대상 글 없음 - publicId={}", event.publicId()));
        } catch (Exception e) {
            log.error("ES 인덱싱 실패 - publicId={}, msg={}", event.publicId(), e.getMessage(), e);
        }
    }

    @Async("esIndexExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostUnpublished(PostUnpublishedEvent event) {
        try {
            String docId = event.publicId().toString();
            postSearchRepository.deleteById(docId);
            log.info("ES 색인 삭제 완료 - publicId={}", event.publicId());
        } catch (Exception e) {
            log.error("ES 색인 삭제 실패 - publicId={}, msg={}", event.publicId(), e.getMessage(), e);
        }
    }
}