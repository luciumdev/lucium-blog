package com.ysh.auth;

import com.ysh.IntegrationTest;
import com.ysh.domain.user.entity.User;
import com.ysh.domain.user.entity.UserRole;
import com.ysh.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
class AuthFlowTest extends IntegrationTest {

    @LocalServerPort int port;

    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    WebTestClient client;
    String adminEmail = "admin-test@lucium.dev";
    String adminPassword = "admin1234";

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        userRepository.deleteAll();
        userRepository.save(User.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .nickname("admin")
                .role(UserRole.ADMIN)
                .build());
    }

    @Test
    void login_success_returnsTokens() {
        client.post().uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", adminEmail, "password", adminPassword))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.accessToken").isNotEmpty()
                .jsonPath("$.data.refreshToken").isNotEmpty();
    }

    @Test
    void login_wrongPassword_returns401() {
        client.post().uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", adminEmail, "password", "wrong-password"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void login_nonExistingUser_returns401_sameAsWrongPassword() {
        String wrongPwBody = client.post().uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", adminEmail, "password", "wrong-password"))
                .exchange()
                .expectStatus().isUnauthorized()
                .returnResult(String.class)
                .getResponseBody().blockFirst();

        String nonExistingBody = client.post().uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "nobody@nowhere.com", "password", "anything"))
                .exchange()
                .expectStatus().isUnauthorized()
                .returnResult(String.class)
                .getResponseBody().blockFirst();

        assertThat(nonExistingBody)
                .as("user enumeration: 두 응답이 동일해야 함")
                .isEqualTo(wrongPwBody);
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() {
        client.post().uri("/v1/files/presigned-url")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("fileName", "x.png", "contentType", "image/png"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedEndpoint_withAdminToken_returns200() {
        @SuppressWarnings("unchecked")
        Map<String, Object> loginResp = client.post().uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", adminEmail, "password", adminPassword))
                .exchange()
                .expectStatus().isOk()
                .returnResult(Map.class)
                .getResponseBody()
                .blockFirst();

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) loginResp.get("data");
        String token = (String) data.get("accessToken");

        assertThat(token).isNotNull();

        client.post().uri("/v1/files/presigned-url")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("fileName", "x.png", "contentType", "image/png"))
                .exchange()
                .expectStatus().isOk();
    }
}