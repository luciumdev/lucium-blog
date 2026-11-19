package com.ysh.domain.market.entity;

import com.ysh.domain.market.domain.DataType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "market_snapshots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="snapshot_at", nullable = false)
    private LocalDateTime snapshotAt;

    @Enumerated(EnumType.STRING)
    @Column(name="data_type", nullable = false, length = 30)
    private DataType dataType;

    // JSONB 매핑
    @JdbcTypeCode(SqlTypes.JSON)              // Hibernate가 String을 JSONB로 캐스팅
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Builder
    private MarketSnapshot(LocalDateTime snapshotAt, DataType dataType, String payload) {
        this.snapshotAt = snapshotAt;
        this.dataType = dataType;
        this.payload = payload;
    }

    public static MarketSnapshot of(DataType dataType, String  payload) {
        return MarketSnapshot.builder()
                .snapshotAt(LocalDateTime.now())
                .dataType(dataType)
                .payload(payload)
                .build();
    }
}
