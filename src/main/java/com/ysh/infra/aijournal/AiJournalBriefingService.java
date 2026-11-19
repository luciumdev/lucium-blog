package com.ysh.infra.aijournal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ysh.infra.llm.ClaudeAiJournalProperties;
import com.ysh.infra.llm.ClaudeClient;
import com.ysh.infra.llm.ClaudePromptRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 저널 LLM 브리핑 서비스
 *
 * 설계 원칙:
 * 1. 수집된 원문 데이터만 요약 — 모델이 임의로 내용을 추가하는 것을 금지
 * 2. JSON 스키마 고정 — 자유 텍스트 출력 금지, 파싱 안정성 확보
 * 3. 중요도 판단 기준 명시 — "중요해 보임"이 아닌 구체적 기준으로 분류
 * 4. 불확실한 정보는 요약에서 제외 — 오보보다 누락이 낫다
 *
 * LLM 호출은 ClaudeClient 추상화를 통해 일원화 (마켓 브리핑과 동일 흐름).
 * 토큰 사용량/stop_reason 로깅은 ClaudeRestApiClient가 담당.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiJournalBriefingService {

    private static final int MAX_NEWS_TO_LLM = 15;

    private final ClaudeClient claudeClient;       // 인터페이스 주입 (prod=Rest, dev=Mock)
    private final ClaudeAiJournalProperties claudeProps;
    private final ObjectMapper objectMapper;

    public AiJournalBriefingResponse generateDailyBriefing(List<CollectedAiNews> newsList) {
        if (newsList == null || newsList.isEmpty()) {
            return AiJournalBriefingResponse.empty();
        }

        if (newsList.size() > MAX_NEWS_TO_LLM) {
            log.info("LLM 입력 {}건 → 상위 {}건으로 제한", newsList.size(), MAX_NEWS_TO_LLM);
            newsList = newsList.subList(0, MAX_NEWS_TO_LLM);
        }

        ClaudePromptRequest request = new ClaudePromptRequest(
                buildSystemPrompt(),
                buildUserPrompt(newsList),
                claudeProps.maxTokens(),
                claudeProps.temperature(),
                claudeProps.model()
        );

        String responseText = claudeClient.complete(request);
        if (responseText == null || responseText.isBlank()) {
            log.warn("LLM 응답이 비어있음 — 빈 브리핑 반환");
            return AiJournalBriefingResponse.empty();
        }
        return parseResponse(responseText);
    }

    /**
     * 시스템 프롬프트: 모델 역할·규칙·출력 스키마.
     * 매번 동일하므로 system 영역에 둔다 (Anthropic은 system 캐싱 시 비용 ↓).
     */
    private String buildSystemPrompt() {
        return """
            당신은 AI/ML 기술 뉴스를 정확하게 요약하는 편집자입니다.

            ## 절대 지켜야 할 규칙

            1. 아래 [수집된 기사] 내용만 바탕으로 작성하세요.
               - 학습 데이터에서 "알 것 같은" 내용을 추가하지 마세요.
               - 기사에 없는 수치, 날짜, 기능을 지어내지 마세요.

            2. 불확실하거나 루머성 내용은 요약에서 제외하세요.
               - 공식 발표, 공식 블로그, 공식 API 문서에서 온 내용만 포함하세요.
               - "~로 알려졌다", "~라고 전해진다" 같은 표현이 원문에 있으면
                 important = "LOW"로 분류하고 summary에 "(미확인)" 표기하세요.

            3. 요약은 사실만 담아야 합니다.
               - 과장, 감탄사, 홍보성 문구 없이 핵심 사실만 서술하세요.
               - 예) "혁신적인 GPT-5" (X) → "GPT-5 기술 리포트 공개" (O)
            
            4. 요약 문장 작성 규칙
                - 서로 다른 주제(다른 버전, 다른 제품)를 \\"~고,\\"로 한 문장에 묶지 마세요. 주제마다 문장을 나누세요.
                - items[].summary에는 날짜를 쓰지 마세요. 무엇이 바뀌었는지만 1~2문장으로 쓰세요.

            ## 중요도 분류 기준 (important 필드)

            HIGH: 새 모델/API 출시, 가격·정책 변경, API 스펙 변경, 보안 패치
            MEDIUM: 벤치마크, 기능 개선, 오픈소스 마이너 릴리즈, 기술 블로그
            LOW: 이벤트 예고, 파트너십, 미확인 내용

            ## 키워드 추출 기준

            - 기사에 실제로 등장한 고유명사, 모델명, 기술 용어만 추출
            - 추상적 단어 제외("혁신", "미래", "발전" 등)
            - 최대 10개

            ## 출력 형식 (JSON만 출력, 다른 텍스트 없이)

            {
              "daily_summary": "오늘 전체를 2~3문장으로 요약. 수치·모델명 등 사실만.",
              "highlight_count": {
                "total": 정수, "high": 정수, "medium": 정수, "low": 정수
              },
              "items": [
                {
                  "source": "기사 출처 그대로",
                  "title": "기사 제목 그대로 또는 더 명확한 표현",
                  "summary": "2문장 이내. 원문 근거 사실만.",
                  "important": "HIGH | MEDIUM | LOW",
                  "category": "모델 릴리즈 | API 업데이트 | 오픈소스 | 벤치마크 | 정책/가격 | 기술 블로그 | 기타",
                  "tags": ["태그1", "태그2"],
                  "url": "원문 URL 그대로"
                }
              ],
              "top_keywords": ["키워드1", "...최대 10개"]
            }
            """;
    }

    /**
     * 유저 프롬프트: 매일 달라지는 기사 본문.
     */
    private String buildUserPrompt(List<CollectedAiNews> newsList) {
        StringBuilder articles = new StringBuilder("## 수집된 기사\n\n");
        for (int i = 0; i < newsList.size(); i++) {
            CollectedAiNews news = newsList.get(i);
            articles.append("[기사 ").append(i + 1).append("]\n")
                    .append("출처: ").append(news.source()).append("\n")
                    .append("제목: ").append(news.title()).append("\n")
                    .append("내용: ").append(news.content()).append("\n")
                    .append("URL: ").append(news.url()).append("\n\n");
        }
        return articles.toString();
    }

    /**
     * JSON 파싱 — 실패 시 빈 응답 반환 (서비스 중단 방지).
     */
    private AiJournalBriefingResponse parseResponse(String text) {
        try {
            int start = text.indexOf('{');
            int end   = text.lastIndexOf('}') + 1;
            if (start < 0 || end <= start) {
                log.warn("LLM 응답에서 JSON을 찾지 못했습니다. 빈 브리핑 반환.");
                return AiJournalBriefingResponse.empty();
            }
            String json = text.substring(start, end);
            return objectMapper.readValue(json, AiJournalBriefingResponse.class);
        } catch (Exception e) {
            log.error("LLM 브리핑 파싱 실패", e);
            return AiJournalBriefingResponse.empty();
        }
    }
}