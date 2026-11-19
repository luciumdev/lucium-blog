package com.ysh.infra.llm;

import com.ysh.infra.llm.dto.ClaudeApiRequest;
import com.ysh.infra.llm.dto.ClaudeApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Slf4j
@Component
@Profile({"prod", "local"})
public class ClaudeRestApiClient implements ClaudeClient{

    private final RestClient restClient;

    public ClaudeRestApiClient(@Qualifier("claudeRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public String complete(ClaudePromptRequest request) {
        try {
            ClaudeApiRequest body = new ClaudeApiRequest(
                    request.model(),
                    request.maxTokens(),
                    request.systemPrompt(),
                    List.of(new ClaudeApiRequest.Message("user", request.userPrompt())),
                    request.temperature()
            );

            ClaudeApiResponse response = restClient.post()
                    .uri("/v1/messages")
                    .body(body)
                    .retrieve()
                    .body(ClaudeApiResponse.class);

            if (response == null) {
                log.warn("Claude API returned null response.");
                return "";
            }

            // 비용 모니터링 — 토큰 사용량 로그
            if (response.usage() != null) {
                log.info("Claude API call. model={}, input_tokens={}, output_tokens={}, stop={}",
                        response.model(),
                        response.usage().inputTokens(),
                        response.usage().outputTokens(),
                        response.stopReason());
            }

            return response.firstText();

        } catch (RestClientException e) {
            log.error("Claude API call failed: {}", e.getMessage());
            return "";
        }
    }
}
