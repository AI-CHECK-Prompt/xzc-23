package com.fishing.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 跨渔港撮合 - 待售渔获预挂牌
 * <p>
 * 状态：OPEN(待报价) / DEAL(已成交) / CANCEL(已下架) / EXPIRED(已过期)
 */
@Data
@Entity
@Table(name = "pre_listing", indexes = {
        @Index(name = "idx_listing_status", columnList = "status,availableTime"),
        @Index(name = "idx_listing_species", columnList = "seaAreaName,species,status")
})
public class PreListing {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String vesselId;

    @Column(nullable = false, length = 64)
    private String vesselNo;

    @Column(length = 64)
    private String vesselName;

    @Column(length = 64)
    private String ownerName;

    /** 归港渔港 */
    @Column(length = 64)
    private String portName;

    @Column(length = 64)
    private String seaAreaName;

    @Column(nullable = false, length = 32)
    private String species;

    /** 期望可售重量（kg） */
    private BigDecimal expectedWeight;

    /** 期望单价（元/kg） */
    private BigDecimal expectedPrice;

    /** 预计可取货时间（船东归港时间） */
    private LocalDateTime availableTime;

    @Column(length = 512)
    private String remark;

    /** OPEN / DEAL / CANCEL / EXPIRED */
    @Column(length = 16)
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
