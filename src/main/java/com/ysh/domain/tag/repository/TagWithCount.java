package com.ysh.domain.tag.repository;

/**
 * 태그 + 발행 글 개수 프로젝션.
 * 조회 시점에 PUBLISHED 글 개수를 실시간 집계한다.
 */
public record TagWithCount(
        Long id,
        String name,
        String slug,
        long publishedCount
) {}