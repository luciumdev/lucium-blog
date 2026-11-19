package com.ysh.domain.news.dto;

/**
 *
 * @param id
 * @param type "story" | "job" | "poll" | "comment" 등
 * @param title
 * @param url
 * @param score Ask HN은 null
 * @param time unix timestamp (초 단위)
 * @param dead null 또는 true
 * @param deleted null 또는 true
 */
public record HackerNewsItem(
        Long id,
        String type,
        String title,
        String url,
        Integer score,
        Long time,
        Boolean dead,
        Boolean deleted
) {}
