package com.fishing.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 跨渔港撮合 - 电子交易确认单
 * <p>
 * 状态：DRAFT(待签署) / SIGNED(已签署) / CANCELLED(已取消)
 * <p>
 * 符合市场监管要求：双方电子签字 + 确认单编号 + 全字段留痕
 */
@Data
@Entity
@Table(name = "trade_confirmation", indexes = {
        @Index(name = "idx_conf_listing", columnList = "listingId"),
        @Index(name = "idx_conf_no", columnList = "confirmationNo", unique = true)
})
public class TradeConfirmation {

    @Id
    @Column(length = 32)
    private String id;

    /** 确认单编号：渔港名-船号-YYYYMMDD-序号 */
    @Column(nullable = false, length = 64)
    private String confirmationNo;

    @Column(nullable = false, length = 32)
    private String listingId;

    @Column(nullable = false, length = 32)
    private String bidId;

    @Column(nullable = false, length = 32)
    private String vesselId;

    @Column(nullable = false, length = 64)
    private String vesselNo;

    @Column(length = 64)
    private String portName;

    @Column(length = 64)
    private String seaAreaName;

    @Column(nullable = false, length = 32)
    private String species;

    private BigDecimal weight;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;

    @Column(length = 64)
    private String buyerName;

    @Column(length = 64)
    private String sellerName;

    @Column(length = 256)
    private String destination;

    @Column(length = 64)
    private String signedByBuyer;
    private LocalDateTime signedAtByBuyer;

    @Column(length = 64)
    private String signedBySeller;
    private LocalDateTime signedAtBySeller;

    /** DRAFT / SIGNED / CANCELLED */
    @Column(length = 16)
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
