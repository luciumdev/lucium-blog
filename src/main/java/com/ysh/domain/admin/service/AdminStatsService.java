package com.ysh.domain.admin.service;

import com.ysh.domain.admin.dto.PostStatsResponse;
import com.ysh.domain.post.entity.PostStatus;
import com.ysh.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatsService {

    private final PostRepository postRepository;

    public PostStatsResponse getPostStats() {
        long published = postRepository.countByStatus(PostStatus.PUBLISHED);
        long draft = postRepository.countByStatus(PostStatus.DRAFT);
        return PostStatsResponse.of(published, draft);
    }
}