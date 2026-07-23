package com.fishing.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 冷链温度采样
 */
@Data
@Entity
@Table(name = "cold_chain_temp_reading", indexes = {
        @Index(name = "idx_temp_shipment", columnList = "shipmentId,recordedAt")
})
public class ColdChainTempReading {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String shipmentId;

    private BigDecimal temperature;

    /** 设备/上报人 */
    @Column(length = 64)
    private String source;

    /** 是否触发告警（冗余存储便于查询统计） */
    private Boolean anomaly;

    private LocalDateTime recordedAt;
}
