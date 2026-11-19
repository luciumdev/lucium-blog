package com.ysh.infra.search.news;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface NewsSearchRepository extends ElasticsearchRepository<NewsSearchDocument, String> {
}
