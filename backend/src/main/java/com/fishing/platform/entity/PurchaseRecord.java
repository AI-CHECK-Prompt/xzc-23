package com.fishing.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购回传记录
 */
@Data
@Entity
@Table(name = "purchase_record", indexes = {
        @Index(name = "idx_purchase_vessel", columnList = "vesselId")
})
public class PurchaseRecord {

    @Id
    @Column(length = 32)
    private String id;

    private String vesselId;
    private String vesselNo;
    private String voyageId;

    private String buyerName;
    private String species;
    private BigDecimal weight;
    private BigDecimal price;
    private BigDecimal amount;

    /** 目的地加工企业 */
    private String destination;

    private LocalDateTime purchaseTime;
    private LocalDateTime createdAt;
}
