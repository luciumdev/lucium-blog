package com.ysh.domain.post.service;

import com.ysh.domain.post.repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountFlushJob {
    private static final String BUFFER_KEY = ViewCountService.BUFFER_KEY;

    private final StringRedisTemplate redis;
    private final PostService postService;
    private final PostRepository postRepository;

    @Scheduled(fixedDelayString = "${viewcount.flush-delay-ms:60000}", initialDelayString = "${viewcount.flush-initial-delay-ms:60000}")
    @Transactional
    public void flush() {
        String snapshotKey = BUFFER_KEY + ":flushing" + Instant.now().toEpochMilli();

        Boolean hasExists = redis.hasKey(BUFFER_KEY);
        if (!hasExists) {
            return;
        }
        try{
            redis.rename(BUFFER_KEY, snapshotKey);
        } catch (Exception e ){
            log.debug("view_count buffer rename failed: {}", e.getMessage());
            return;
        }
        Map<Object, Object> entries = redis.opsForHash().entries(snapshotKey);
        redis.delete(snapshotKey);

        if (entries.isEmpty()) {
            return;
        }

        int totalRows = 0;
        long totalCount = 0;

        for (Map.Entry<Object,Object> e : entries.entrySet()) {
            try {
                long postId = Long.parseLong(e.getKey().toString());
                long delta = Long.parseLong(e.getValue().toString());
                if (delta <= 0) {
                    continue;
                }

                int updated = postRepository.incrementViewCount(postId, delta);
                if (updated > 0) {
                    totalRows++;
                    totalCount += delta;
                } else {
                    log.warn("view_count post_id={} view_count={} not found", postId, delta);
                }
            } catch (NumberFormatException nfe){
                log.warn("view_count flush — invalid entry: {} ->{}",e.getKey(), nfe.getMessage());
            }
        }
        log.info("view_count flush — posts={}, totalIncrement={}", totalRows, totalCount);
    }
}
