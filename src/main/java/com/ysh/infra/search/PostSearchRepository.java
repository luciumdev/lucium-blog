package com.ysh.infra.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * ES 전용
 * 기본 crud
 */
public interface PostSearchRepository extends ElasticsearchRepository<PostSearchDocument, String> {
}
