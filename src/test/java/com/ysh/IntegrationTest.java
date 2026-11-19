package com.ysh;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES;
    static final GenericContainer<?> REDIS;
    static final ElasticsearchContainer ELASTICSEARCH;

    static {
        POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("blog_test")
                .withUsername("blog")
                .withPassword("blog");
        POSTGRES.start();

        REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
        REDIS.start();

        // ── ES Testcontainer + nori 플러그인 (변경 없음) ───
        ImageFromDockerfile noriImage = new ImageFromDockerfile("blog-es-test", false)
                .withDockerfileFromBuilder(builder -> builder
                        .from("docker.elastic.co/elasticsearch/elasticsearch:9.2.8")
                        .run("bin/elasticsearch-plugin install --batch analysis-nori")
                        .build());

        ELASTICSEARCH = new ElasticsearchContainer(
                DockerImageName.parse(noriImage.get())
                        .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch")
        )
                .withEnv("xpack.security.enabled", "false")
                .withEnv("discovery.type", "single-node")
                .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");
        ELASTICSEARCH.start();

        // ── Spring 환경 변수 주입 ──────────────────────────
        System.setProperty("spring.datasource.url",      POSTGRES.getJdbcUrl());
        System.setProperty("spring.datasource.username", POSTGRES.getUsername());
        System.setProperty("spring.datasource.password", POSTGRES.getPassword());
        System.setProperty("spring.data.redis.host",     REDIS.getHost());
        System.setProperty("spring.data.redis.port",     String.valueOf(REDIS.getMappedPort(6379)));
        System.setProperty("spring.elasticsearch.uris",  "http://" + ELASTICSEARCH.getHttpHostAddress());

        // prod 프로필 통합 테스트(@ActiveProfiles("prod"))는 ClaudeRestApiClient 빈을 생성하므로 claude.api.key resolve 필요.
        // 실제 Claude 호출 없는 더미 값으로 부팅만 통과시킴.
        // test 프로필에선 ClaudeRestApiClient가 @Profile({"prod","local"})로 제외되어 evaluation 안 일어남.
        System.setProperty("claude.api.key", "test-dummy-key");
        System.setProperty("market.forex.api-key", "test-key");
        System.setProperty("market.kis.app-key", "test-kis-app-key");
        System.setProperty("market.kis.app-secret", "test-kis-app-secret");
        // prod 프로필 통합 테스트(@ActiveProfiles("prod"))는 KoreaEximForexClient 빈을 생성하므로 market.forex.api-key resolve 필요.
        // 실제 한국수출입은행 호출 없는 더미 값으로 부팅만 통과시킴.
        // test 프로필에선 KoreaEximForexClient가 @Profile("prod")로 제외되어 evaluation 안 일어남.
        System.setProperty("market.forex.api-key", "test-key");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            POSTGRES.stop();
            REDIS.stop();
            ELASTICSEARCH.stop();
        }));
    }
}