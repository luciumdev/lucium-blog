package com.ysh.infra.search;

import com.ysh.domain.post.entity.Post;
import com.ysh.domain.post.entity.PostStatus;
import com.ysh.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


@Slf4j
@Service
@RequiredArgsConstructor
public class ReindexService {
    private static final int BATCH_SIZE = 100;

    private final PostRepository postRepository;
    private final PostSearchRepository postSearchRepository;
    private final ElasticsearchOperations operations;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public boolean tryStart (boolean clearBefore) {
        if (!running.compareAndSet(false, true)) {
            log.warn("Reindex already running");
            return false;
        }
        runAsync(clearBefore);
        return true;
    }
    @Async("reindexExecutor")
    @Transactional(readOnly = true)
    public void runAsync(boolean clearBefore) {
        long startMs = System.currentTimeMillis();
        long totalIndexed = 0;
        try {
            if (clearBefore) {
                clearIndex();
            }
            int pageNumber = 0;
            while (true) {
                Page<Post> page = postRepository.findAllByStatusForReindex(
                        PostStatus.PUBLISHED,
                        PageRequest.of(pageNumber, BATCH_SIZE)
                );
                if (page.isEmpty()) {
                    break;
                }
                List<PostSearchDocument> documents = page.getContent().stream()
                        .map(PostSearchDocument::from)
                        .toList();
                postSearchRepository.saveAll(documents);
                totalIndexed += documents.size();
                log.info("Indexed {} posts in {} seconds", totalIndexed, (System.currentTimeMillis() - startMs) / 1000);
                if (!page.hasNext()){
                    break;
                }
                pageNumber++;
            }
            long elasped = System.currentTimeMillis() - startMs;
            log.info("백필 완료 - total={}, elaspedMs={}", totalIndexed, elasped);
        } catch (Exception e) {
            log.error("백필 실패 - totalIndexed={}, msg={}", totalIndexed, e.getMessage(), e);
        } finally {
            running.set(false);
        }
    }

    private void clearIndex() {
        IndexOperations indexOps = operations.indexOps(PostSearchDocument.class);
        if (indexOps.exists()) {
            indexOps.delete();
            indexOps.create();
            indexOps.putMapping();
            log.info("백필 사전 정리 — 인덱스 삭제 후 재생성 (settings/mapping 재적용)");
        } else {
            indexOps.create();
            indexOps.putMapping();
            log.info("백필 사전 정리 — 인덱스 미존재, 신규 생성");
        }
    }
    /**
     * DB↔ES 정합성 체크.
     *
     * count만 비교하면 우연히 같은 개수일 때 false negative (예: DB=[A,B,C], ES=[A,B,D] 둘 다 3건).
     * publicId 셋 차집합으로 missing/stale 정확히 식별.
     *
     * ES 페이지네이션 — from+size 한계 10000 (index.max_result_window 기본값).
     * 1만 글 초과 시 search_after 또는 ElasticsearchOperations.searchForStream으로 리팩토링 필요.
     */
    public ConsistencyReport checkConsistency() {
        long dbCount = postRepository.countByStatus(PostStatus.PUBLISHED);
        long esCount = postSearchRepository.count();

        Set<String> dbIds = postRepository.findPublicIdsByStatus(PostStatus.PUBLISHED).stream()
                .map(UUID::toString)
                .collect(Collectors.toSet());

        Set<String> esIds = new HashSet<>();
        int pageNumber = 0;
        int pageSize = 1000;
        Page<PostSearchDocument> esPage;
        do {
            esPage = postSearchRepository.findAll(PageRequest.of(pageNumber++, pageSize));
            esPage.forEach(doc -> esIds.add(doc.getPublicId()));
        } while (esPage.hasNext());

        Set<String> missingInEs = new HashSet<>(dbIds);
        missingInEs.removeAll(esIds);

        Set<String> staleInEs = new HashSet<>(esIds);
        staleInEs.removeAll(dbIds);

        boolean consistent = missingInEs.isEmpty() && staleInEs.isEmpty();
        return new ConsistencyReport(dbCount, esCount, consistent, missingInEs, staleInEs);
    }

    public record ConsistencyReport(
            long dbCount,
            long esCount,
            boolean consistent,
            Set<String> missingInEs,
            Set<String> staleInEs
    ) {}
}
