package com.ysh.post;

import com.ysh.IntegrationTest;
import com.ysh.domain.category.entity.Category;
import com.ysh.domain.category.repository.CategoryRepository;
import com.ysh.domain.post.entity.Post;
import com.ysh.domain.post.repository.PostRepository;
import com.ysh.domain.user.entity.User;
import com.ysh.domain.user.entity.UserRole;
import com.ysh.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

class PostFlowTest extends IntegrationTest{

    @LocalServerPort int port;

    @Autowired UserRepository userRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired PostRepository postRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired StringRedisTemplate redis;

    WebTestClient client;
    Post seededPost;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        postRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        redis.getConnectionFactory().getConnection().serverCommands().flushDb();

        User author = userRepository.save(User.builder()
                .email("author@lucium.dev")
                .password(passwordEncoder.encode("pw"))
                .nickname("author")
                .role(UserRole.ADMIN)
                .build());

        Category category = categoryRepository.save(Category.builder()
                .name("Tech")
                .build());

        seededPost = postRepository.save(Post.builder()
                .author(author)
                .category(category)
                .title("integration test post")
                .content("body")
                .build());
        seededPost.publish();
        seededPost = postRepository.save(seededPost);
    }

    @Test
    void getPost_unknownVisitor_issuesCookie() {
        client.get().uri("/v1/posts/{publicId}", seededPost.getPublicId())
                .exchange()
                .expectStatus().isOk()
                .expectCookie().exists("visitor_id")     // VisitorCookieFilter 동작 검증
                .expectBody()
                .jsonPath("$.data.title").isEqualTo("integration test post")
                .jsonPath("$.data.alreadyRead").isEqualTo(false);
    }

    @Test
    void getPost_sameVisitor_dedupBlocksSecondIncrement() {
        // 1차 호출 — visitor_id 발급 + 카운트 +1
        var firstResp = client.get().uri("/v1/posts/{publicId}", seededPost.getPublicId())
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class);

        String visitorCookie = firstResp.getResponseCookies()
                .getFirst("visitor_id").getValue();

        // 2차/3차 호출 -> count 업데이트x
        for (int i = 0; i < 2; i++) {
            client.get().uri("/v1/posts/{publicId}", seededPost.getPublicId())
                    .cookie("visitor_id", visitorCookie)
                    .exchange()
                    .expectStatus().isOk();
        }

        // Redis 버퍼 확인 — postId의 누적 = 1
        Object delta = redis.opsForHash()
                .get("view_count_buffer", String.valueOf(seededPost.getId()));

        assertThat(delta).isEqualTo("1");
    }

    @Test
    void getPost_threeDifferentVisitors_buffersThree() {
        for (int i = 0; i < 3; i++) {
            client.get().uri("/v1/posts/{publicId}", seededPost.getPublicId())
                    .exchange()
                    .expectStatus().isOk();
        }

        Object delta = redis.opsForHash()
                .get("view_count_buffer", String.valueOf(seededPost.getId()));

        assertThat(delta).isEqualTo("3");
    }
}