package com.fishing.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 跨渔港撮合 - 采购商对预挂牌的报价
 * <p>
 * 状态：PENDING(待定) / ACCEPTED(已接受) / REJECTED(已拒绝) / WITHDRAWN(已撤销)
 */
@Data
@Entity
@Table(name = "bid", indexes = {
        @Index(name = "idx_bid_listing", columnList = "listingId,status"),
        @Index(name = "idx_bid_buyer", columnList = "buyerName,createdAt")
})
public class Bid {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String listingId;

    @Column(nullable = false, length = 64)
    private String buyerName;

    @Column(length = 32)
    private String buyerPhone;

    @Column(length = 256)
    private String destination;

    private BigDecimal bidPrice;
    private BigDecimal bidWeight;

    @Column(length = 256)
    private String message;

    /** PENDING / ACCEPTED / REJECTED / WITHDRAWN */
    @Column(length = 16)
    private String status;

    private LocalDateTime createdAt;
}
