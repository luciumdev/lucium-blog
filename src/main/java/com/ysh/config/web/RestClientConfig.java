package com.ysh.config.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {
    /**
     * - RSS 수집, GitHub API, Hacker News, 금융 API 모두 공용 사용
     * - 타임아웃 설정으로 외부 API 지연 시 서버 블로킹 방지
     */
    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .defaultHeader("User-Agent", "tno-blog-crawler/1.0")
                .defaultHeader("Accept", "application/json, application/xml, text/xml, */*")
                .build();
    }

    @Bean("forexRestClient")
    public RestClient forexRestClient(
            @Value("${market.forex.base-url}") String baseUrl,
            @Value("${market.forex.connect-timeout}") Duration connectTimeout,
            @Value("${market.forex.read-timeout}") Duration readTimeout
    ) {
        // 1. JDK HttpClient — connectTimeout은 여기서 설정
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();

        // 2. JdkClientHttpRequestFactory — readTimeout은 여기서 설정
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "ysh-blog-market/1.0")
                .build();
    }

    @Bean("hackerNewsRestClient")
    public RestClient hackerNewsRestClient(
            @Value("${news.hackernews.base-url}") String baseUrl,
            @Value("${news.hackernews.connect-timeout}") Duration connectTimeout,
            @Value("${news.hackernews.read-timeout}") Duration readTimeout
    ) {
        // 1. JDK HttpClient — connectTimeout은 여기서 설정
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();

        // 2. JdkClientHttpRequestFactory — readTimeout은 여기서 설정
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "ysh-blog-news/1.0")
                .build();
    }

    @Bean("hankyungRestClient")
    public RestClient hankyungRestClient(
            @Value("${news.hankyung.base-url}") String baseUrl,
            @Value("${news.hankyung.connect-timeout}") Duration connectTimeout,
            @Value("${news.hankyung.read-timeout}") Duration readTimeout
    ) {
        // 1. JDK HttpClient — connectTimeout은 여기서 설정
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();

        // 2. JdkClientHttpRequestFactory — readTimeout은 여기서 설정
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "ysh-blog-news/1.0")
                .defaultHeader("Accept", "application/rss+xml, application/xml, text/xml")
                .build();
    }

    @Bean("claudeRestClient")
    @Profile({"prod", "local"})
    public RestClient claudeRestClient(
            @Value("${claude.api.base-url}") String baseUrl,
            @Value("${claude.api.key}") String apiKey,
            @Value("${claude.api.connect-timeout}") Duration connectTimeout,
            @Value("${claude.api.read-timeout}") Duration readTimeout
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("content-type", "application/json")
                .build();
    }

    /**
     * 주의) kis는 요청마다 appkey/appsecret/access_token 다른 헤더로 들어감
     * default 헤더로 처리 안함
     * @param baseUrl
     * @param connectTimeout
     * @param readTimeout
     * @return
     */
    @Bean
    public RestClient kisRestClient(@Value("${market.kis.base-url}") String baseUrl,
                                    @Value("${market.kis.connect-timeout}") Duration connectTimeout,
                                    @Value("${market.kis.read-timeout}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "ysh-blog-market/1.0")
                .defaultHeader("Content-Type", "application/json; charset=utf-8")
                .build();
    }

    @Bean("goldApiRestClient")
    public RestClient goldApiRestClient(
            @Value("${market.commodities.goldapi.base-url}") String baseUrl,
            @Value("${market.commodities.goldapi.connect-timeout}") Duration connectTimeout,
            @Value("${market.commodities.goldapi.read-timeout}") Duration readTimeout
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Bean("alphaVantageApiRestClient")
    public RestClient alphaVantageApiRestClient (
            @Value("${market.commodities.alphavantage.base-url}") String baseUrl,
            @Value("${market.commodities.alphavantage.connect-timeout}") Duration connectTimeout,
            @Value("${market.commodities.alphavantage.read-timeout}") Duration readTimeout
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
