package com.ysh;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationContextTest extends IntegrationTest {

    @Autowired ApplicationContext context;
    @Autowired DataSource dataSource;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @Test
    void flywayMigrationApplied() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class);

        // V1 — 블로그 코어
        assertThat(tables)
                .as("V1 — 블로그 코어 테이블")
                .contains("users", "posts", "categories", "tags",
                        "comments", "post_tags", "post_views", "daily_visitors");

        // V2 — 마켓 대시보드
        assertThat(tables)
                .as("V2 — 마켓 스키마")
                .contains("market_snapshots", "news_feeds");

        // 시장 브리핑 기능 삭제 — 테이블 제거 마이그레이션 적용 확인
        assertThat(tables)
                .as("llm_briefings 제거됨")
                .doesNotContain("llm_briefings");

        // Flyway 메타
        assertThat(tables).contains("flyway_schema_history");
    }

}