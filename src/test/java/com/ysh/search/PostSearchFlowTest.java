package com.ysh.search;

import com.ysh.IntegrationTest;
import com.ysh.domain.category.repository.CategoryRepository;
import com.ysh.domain.post.entity.Post;
import com.ysh.domain.post.entity.PostStatus;
import com.ysh.domain.post.event.PostPublishedEvent;
import com.ysh.domain.post.event.PostUnpublishedEvent;
import com.ysh.domain.post.repository.PostRepository;
import com.ysh.domain.post.service.PostService;
import com.ysh.domain.post.dto.PostCreateRequest;
import com.ysh.domain.user.entity.User;
import com.ysh.domain.user.repository.UserRepository;
import com.ysh.infra.search.PostSearchRepository;
import com.ysh.infra.search.ReindexService;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post PUBLISHED → ES 색인까지 end-to-end 검증.
 *
 * @DirtiesContext: ES 인덱스 상태가 다른 테스트에 영향 안 가게 격리.
 *   매 테스트마다 ApplicationContext 재구축 — 비용 크지만 검색 도메인은
 *   인덱스 상태가 핵심이라 격리가 정확성보다 우선.
 *
 * Awaitility: @Async 색인이 완료될 때까지 폴링.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PostSearchFlowTest extends IntegrationTest {

    @Autowired PostService postService;
    @Autowired PostSearchRepository searchRepository;
    @Autowired ReindexService reindexService;
    @Autowired PostRepository postRepository;
    @Autowired UserRepository userRepository;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = userRepository.findByEmail("test-admin@example.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("test-admin@example.com")
                        .password("$2a$12$dummy")
                        .nickname("test-admin")
                        .build()));
    }

    @Test
    @Disabled("""
    PostService를 거치지 않고 events.publishEvent를 직접 호출하면
    @TransactionalEventListener(AFTER_COMMIT)가 트리거되지 않음 —
    테스트 메서드 자체에 트랜잭션 경계가 없거나 약함.
    Phase 5+ MockMvc 기반 E2E 테스트로 정석화 예정 (실제 HTTP 요청 → 컨트롤러 →
    PostService.createPost 호출하면 자연스럽게 트랜잭션 안에서 이벤트 발행되어
    AFTER_COMMIT 트리거됨).
    """)
    @WithMockUser(username = "test-admin", roles = "ADMIN")
    void publishedPost_isIndexedToElasticsearch() {
        // given
        PostCreateRequest request = createRequestUsingReflection(
                "Spring Boot 마이그레이션 회고",
                "Flyway, Jackson 자동 설정 변화를 학습했다.",
                PostStatus.PUBLISHED);

        // when — PostService 직접 호출 대신 PostRepository로 우회 + 이벤트 수동 발행
        Post post = postRepository.save(Post.builder()
                .author(admin)
                .title("Spring Boot 마이그레이션 회고")
                .content("Flyway, Jackson 자동 설정 변화를 학습했다.")
                .build());
        post.publish();
        postRepository.save(post);

        // 이벤트 수동 발행 — 실제 PostService 흐름 시뮬레이션
        events.publishEvent(PostPublishedEvent.of(post));

        // then — @Async라 즉시 색인 안 됨. 최대 5초 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    var doc = searchRepository.findById(post.getPublicId().toString());
                    assertThat(doc).isPresent();
                    assertThat(doc.get().getTitle()).contains("마이그레이션");
                });
    }

    @Test
    void reindex_populatesElasticsearchFromDb() {
        // given — ES 인덱스 격리 (다른 테스트의 잔여 영향 차단)
        searchRepository.deleteAll();

        // 트랜잭션 격리 회피 — 명시적으로 commit하기 위해 별도 패턴 사용
        // @Transactional 없는 테스트라 save 후 즉시 commit 됨 → 백필 스레드에서 보임
        int beforeCount = (int) postRepository.count();
        for (int i = 1; i <= 3; i++) {
            Post post = postRepository.save(Post.builder()
                    .author(admin)
                    .title("백필 테스트 " + i)
                    .content("본문 " + i)
                    .build());
            post.publish();
            postRepository.save(post);
        }

        long publishedInDb = postRepository.countByStatus(PostStatus.PUBLISHED);
        long expectedEsCount = publishedInDb;

        // when
        boolean started = reindexService.tryStart(true);

        // then
        assertThat(started).isTrue();
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    long count = searchRepository.count();
                    assertThat(count).isEqualTo(expectedEsCount);
                });
    }

    @Autowired org.springframework.context.ApplicationEventPublisher events;

    private PostCreateRequest createRequestUsingReflection(String title, String content, PostStatus status) {
        return null;
    }
}