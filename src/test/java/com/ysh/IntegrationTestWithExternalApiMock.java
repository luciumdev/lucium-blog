package com.ysh;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

public abstract class IntegrationTestWithExternalApiMock extends IntegrationTest{
    @Container
    static MockServerContainer mockServer = new MockServerContainer(
            DockerImageName.parse("mockserver/mockserver:5.15.0"));

    @DynamicPropertySource
    static void redirectAllExternalApis(DynamicPropertyRegistry r) {
        String mockUrl = "http://" + mockServer.getHost() + ":" + mockServer.getServerPort();
        r.add("news.hackernews.base-url", () -> mockUrl);
        r.add("news.hankyung.base-url",   () -> mockUrl);
        r.add("market.forex.base-url",    () -> mockUrl);
    }
}
