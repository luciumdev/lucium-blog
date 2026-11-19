package com.ysh.infra.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * prod·local 이외(test 등) 프로파일용 — 실제 API 를 호출하지 않는다.
 *
 * <p>빈 문자열을 돌려주면 각 서비스가 "응답 없음"으로 처리한다.
 * (AI 저널 = 빈 브리핑 반환, 뉴스 보강 = 이번 회차 건너뜀·시도 횟수 증가 없음)
 * 특정 응답이 필요한 테스트는 ClaudeClient 를 @MockitoBean 으로 대체할 것.
 *
 * <p>이전에는 market-briefing-mock-output.txt 를 반환했으나 시장 브리핑 제거로 삭제.
 */
@Slf4j
@Component
@Profile("!prod & !local")
public class MockClaudeClient implements ClaudeClient {

    @Override
    public String complete(ClaudePromptRequest request) {
        log.debug("MockClaudeClient — 실제 호출 없이 빈 응답 반환. model={}", request.model());
        return "";
    }
}
