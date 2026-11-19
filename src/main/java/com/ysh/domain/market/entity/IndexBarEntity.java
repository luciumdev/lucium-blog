package com.ysh.domain.market.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * index_bars 정규화 read model 엔티티.
 *
 * <p>도메인 record {@code IndexBar}(수집 시 KIS 응답 봉)와 이름이 겹치지 않도록 Entity 접미사.
 * upsert 는 {@code IndexBarRepository.upsert()} 네이티브 쿼리로 처리하므로 이 엔티티는
 * 조회(projection 대신 표준 파생 쿼리 사용 시)와 스키마 매핑 용도.
 */
@Entity
@Table(name = "index_bars")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IndexBarEntity {

    @EmbeddedId
    private Pk id;

    @Column(nullable = false)
    private double close;

    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        @Column(length = 20)
        private String code;

        @Column(name = "bar_date")
        private LocalDate barDate;

        public Pk(String code, LocalDate barDate) {
            this.code = code;
            this.barDate = barDate;
        }
    }
}