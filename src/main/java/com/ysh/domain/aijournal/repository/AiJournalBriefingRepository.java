package com.ysh.domain.aijournal.repository;

import com.ysh.domain.aijournal.entity.AiJournalBriefing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AiJournalBriefingRepository extends JpaRepository<AiJournalBriefing, Long> {
    Optional<AiJournalBriefing> findByJournalDate(LocalDate journalDate);

    /** 기준일 이하 가장 최근 브리핑 — 오늘 수집 실패 시 이전 내용 fallback */
    Optional<AiJournalBriefing> findTopByJournalDateLessThanEqualOrderByJournalDateDesc(LocalDate journalDate);
}
