package com.ysh.domain.tag.service;

import com.ysh.domain.tag.dto.TagResponse;
import com.ysh.domain.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {
    private final TagRepository tagRepository;

    /**
     * 사이드바용 공개 태그 목록.
     * PUBLISHED 글이 연결된 태그만, 발행 글 개수 내림차순.
     * postCount 물리 컬럼(현재 미사용)이 아닌 조회 시점 집계값을 반환한다.
     */
    public List<TagResponse> getAllTags() {
        return tagRepository.findAllWithPublishedPosts().stream()
                .map(TagResponse::from)
                .toList();
    }
}