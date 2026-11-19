package com.ysh.infra.search;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
/**
 * 검색 결과 단건 응답.
 * 1. content 필드는 하이라이팅된 스니펫만 (전체 본문 X).
 * 2. publicId만 노출
 * 3. score 노출 — 디버깅·관측성용
 */
@Getter
@Builder
public class SearchResponse {
    private UUID publicId;
    private String title;
    private String snippet;
    private String categoryName;
    private List<String> tags;
    private LocalDateTime publishedAt;
    private double score;

}
