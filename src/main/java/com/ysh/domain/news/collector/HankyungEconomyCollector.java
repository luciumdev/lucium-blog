package com.ysh.domain.news.collector;

import com.ysh.domain.news.entity.NewsCategory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile({"prod", "test"})
public class HankyungEconomyCollector extends RssNewsCollector{

    private static final String SOURCE = "Hankyung";

    public HankyungEconomyCollector (@Qualifier("hankyungRestClient") RestClient restClient,
                                     @Value("${news.hankyung.economy.feed-path}") String feedPath) {
        super(restClient, feedPath);
    }


    @Override
    public NewsCategory getCategory() {
        return NewsCategory.ECONOMY;
    }

    @Override
    public String getSource() {
        return SOURCE;
    }
}
