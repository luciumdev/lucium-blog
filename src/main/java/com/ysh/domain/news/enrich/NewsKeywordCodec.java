package com.ysh.domain.news.enrich;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * news_feeds.keywords (JSONB 배열 문자열) ↔ List 변환 + 정규화.
 *
 * <p>정규화 규칙: 앞뒤 공백·'#' 제거 → 빈 값 제외 → 길이 상한 → 중복 제거(순서 유지) → 개수 상한.
 * LLM 출력은 신뢰하지 않는다는 전제로 저장 직전에 항상 통과시킨다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsKeywordCodec {

    static final int MAX_KEYWORDS = 3;
    static final int MAX_KEYWORD_LENGTH = 30;
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public List<String> normalize(List<String> raw) {
        if (raw == null || raw.isEmpty()) return List.of();
        Set<String> unique = new LinkedHashSet<>();
        for (String keyword : raw) {
            if (keyword == null) continue;
            String k = keyword.strip().replaceFirst("^#+", "").strip();
            if (k.isEmpty() || k.length() > MAX_KEYWORD_LENGTH) continue;
            unique.add(k);
            if (unique.size() == MAX_KEYWORDS) break;
        }
        return List.copyOf(unique);
    }

    /** 정규화 후 JSON 배열 문자열. 직렬화 실패는 사실상 없지만 "[]"로 폴백. */
    public String toJson(List<String> keywords) {
        try {
            return objectMapper.writeValueAsString(normalize(keywords));
        } catch (Exception e) {
            log.warn("키워드 직렬화 실패: {}", e.getMessage());
            return "[]";
        }
    }

    /** DB 값 파싱. null·깨진 값은 빈 리스트 (응답을 깨뜨리지 않음). */
    public List<String> fromJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<String> parsed = objectMapper.readValue(json, STRING_LIST);
            return parsed == null ? List.of() : new ArrayList<>(parsed);
        } catch (Exception e) {
            log.warn("키워드 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }
}
