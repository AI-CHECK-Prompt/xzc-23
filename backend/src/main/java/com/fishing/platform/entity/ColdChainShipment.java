package com.fishing.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 冷链运单（关联撮合成交的电子交易确认单）
 */
@Data
@Entity
@Table(name = "cold_chain_shipment", indexes = {
        @Index(name = "idx_shipment_confirmation", columnList = "confirmationId"),
        @Index(name = "idx_shipment_vehicle", columnList = "vehicleId,status")
})
public class ColdChainShipment {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String confirmationId;

    @Column(nullable = false, length = 64)
    private String confirmationNo;

    @Column(nullable = false, length = 32)
    private String vehicleId;

    @Column(nullable = false, length = 32)
    private String vehicleNo;

    @Column(length = 64)
    private String driverName;

    @Column(length = 64)
    private String originPort;

    @Column(length = 256)
    private String destination;

    @Column(length = 32)
    private String species;

    private BigDecimal weight;

    /** 温度阈值（按品种区分，例如带鱼 0~4°C，梭子蟹 0~2°C） */
    private BigDecimal minTemp;
    private BigDecimal maxTemp;

    private LocalDateTime plannedDepartureTime;
    private LocalDateTime actualDepartureTime;
    private LocalDateTime plannedArrivalTime;
    private LocalDateTime actualArrivalTime;

    /** 状态：CREATED / IN_TRANSIT / ARRIVED / CANCELLED */
    @Column(length = 16)
    private String status;

    /** 温度异常次数（用于通知与统计） */
    private Integer anomalyCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
