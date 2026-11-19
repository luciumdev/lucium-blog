package com.ysh.infra.search.news.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 테크·경제 공통 키워드.
 * @param days     집계 기간(일)
 * @param from     집계 시작 시각 (KST)
 * @param keywords 양쪽 모두 등장한 키워드 — 겹치는 정도 순
 */
public record CommonKeywordsResponse(
        int days,
        LocalDateTime from,
        List<KeywordCount> keywords
) {
    public CommonKeywordsResponse {
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
    }

    public static CommonKeywordsResponse empty(int days, LocalDateTime from) {
        return new CommonKeywordsResponse(days, from, List.of());
    }

    /** tech = IT 기사 수, economy = ECONOMY 기사 수 */
    public record KeywordCount(String keyword, long tech, long economy) {
        public long total() {
            return tech + economy;
        }
    }
}
