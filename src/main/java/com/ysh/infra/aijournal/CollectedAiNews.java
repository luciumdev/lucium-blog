package com.ysh.infra.aijournal;
/**
 * 수집된 AI 뉴스 원문 데이터
 */
public record CollectedAiNews(  String source,
                                String title,
                                String content,
                                String url,
                                String publishedAt
) {}
