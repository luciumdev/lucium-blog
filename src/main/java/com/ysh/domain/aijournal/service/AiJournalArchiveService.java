package com.ysh.domain.aijournal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ysh.domain.aijournal.entity.AiJournalBriefing;
import com.ysh.domain.aijournal.repository.AiJournalBriefingRepository;
import com.ysh.infra.aijournal.AiJournalBriefingResponse;
import com.ysh.infra.aijournal.AiJournalDailyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiJournalArchiveService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AiJournalBriefingRepository briefingRepository;
    private final ObjectMapper objectMapper;

    /**
     * 오늘 날짜로 DB 저장 (있으면 갱신).
     * 호출 전 briefing.hasContent() 확인은 스케줄러 책임 — 빈 결과로 덮어쓰지 않기 위함.
     */
    @Transactional
    public void persist(AiJournalBriefingResponse briefing) {
        LocalDate today = LocalDate.now(KST);
        String itemsJson;
        try {
            itemsJson = objectMapper.writeValueAsString(briefing.items());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize briefing items for date={}", today, e);
            return;
        }

        AiJournalBriefingResponse.HighlightCount count = briefing.highlightCount();
        Optional<AiJournalBriefing> existing = briefingRepository.findByJournalDate(today);
        if (existing.isPresent()) {
            existing.get().updateFrom(
                    briefing.dailySummary(),
                    itemsJson,
                    briefing.topKeywords(),
                    count.total(),
                    count.high(),
                    count.medium(),
                    count.low()
            );
            log.info("Updated AI Journal briefing for date={}", today);
        } else {
            briefingRepository.save(AiJournalBriefing.builder()
                    .journalDate(today)
                    .dailySummary(briefing.dailySummary())
                    .itemsJson(itemsJson)
                    .topKeywords(briefing.topKeywords())
                    .totalCount(count.total())
                    .highCount(count.high())
                    .mediumCount(count.medium())
                    .lowCount(count.low())
                    .build());
            log.info("Saved AI Journal briefing for date={}", today);
        }
    }

    @Transactional(readOnly = true)
    public Optional<AiJournalDailyResponse> findByDate(LocalDate date) {
        return briefingRepository.findByJournalDate(date)
                .map(this::toDaily);
    }

    /**
     * 기준일 이하 가장 최근 브리핑.
     * 오늘 수집/LLM 호출이 실패했거나 아직 06:00 전이면 어제(또는 그 이전) 내용을 반환.
     */
    @Transactional(readOnly = true)
    public Optional<AiJournalDailyResponse> findLatestOnOrBefore(LocalDate date) {
        return briefingRepository.findTopByJournalDateLessThanEqualOrderByJournalDateDesc(date)
                .map(this::toDaily);
    }

    private AiJournalDailyResponse toDaily(AiJournalBriefing entity) {
        return AiJournalDailyResponse.onlyBriefing(toResponse(entity), entity.getJournalDate());
    }

    private AiJournalBriefingResponse toResponse(AiJournalBriefing entity) {
        try {
            var items = objectMapper.readValue(
                    entity.getItemsJson(),
                    new TypeReference<List<AiJournalBriefingResponse.AiNewsItem>>() {}
            );
            return new AiJournalBriefingResponse(
                    entity.getDailySummary(),
                    new AiJournalBriefingResponse.HighlightCount(
                            entity.getTotalCount(),
                            entity.getHighCount(),
                            entity.getMediumCount(),
                            entity.getLowCount()
                    ),
                    items,
                    entity.getTopKeywords()
            );
        } catch (Exception e) {
            log.error("Failed to deserialize items_json from entity id={}", entity.getId(), e);
            return AiJournalBriefingResponse.empty();
        }
    }
}