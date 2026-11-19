package com.ysh.domain.news.collector;

import com.ysh.domain.news.entity.NewsCategory;
import com.ysh.domain.news.entity.NewsFeed;

import java.util.List;

public interface NewsCollector {
    NewsCategory getCategory();
    String getSource();
    List<NewsFeed> collect();
}
