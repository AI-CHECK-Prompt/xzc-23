package com.fishing.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 渔获预申报单
 */
@Data
@Entity
@Table(name = "catch_declaration", indexes = {
        @Index(name = "idx_catch_voyage", columnList = "voyageId")
})
public class CatchDeclaration {

    @Id
    @Column(length = 32)
    private String id;

    @Column(length = 64, unique = true)
    private String declarationNo;

    private String voyageId;
    private String vesselId;
    private String vesselNo;
    private String ownerName;
    private String portName;

    /** 渔获明细 JSON：[{species,estimatedWeight,isProtected,juvenileRatio}] */
    @Column(length = 4000)
    private String itemsJson;

    /** 预申报总重 */
    private BigDecimal estimatedTotal;

    /** 实际过磅总重 */
    private BigDecimal actualTotal;

    /** 偏差比例（百分比） */
    private BigDecimal deviationRatio;

    /** 状态：预申报已提交、过磅已确认、偏差复核中、已完成 */
    @Column(length = 16)
    private String status;

    /** 偏差原因说明（人工复核） */
    @Column(length = 512)
    private String deviationReason;

    private String confirmedBy;

    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
}
