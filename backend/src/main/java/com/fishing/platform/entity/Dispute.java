package com.fishing.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 争议处理
 * <p>
 * 状态机：PENDING(待处理) → INSPECTING(检测中) → ARBITRATING(仲裁中) → CLOSED(已结案) / REJECTED(已驳回)
 * <p>
 * 由交易一方（买方或卖方）基于电子交易确认单发起，
 * 平台协调第三方检测机构出具检测报告后，依据报告进行仲裁结案。
 */
@Data
@Entity
@Table(name = "dispute", indexes = {
        @Index(name = "idx_dispute_no", columnList = "disputeNo", unique = true),
        @Index(name = "idx_dispute_conf", columnList = "confirmationId"),
        @Index(name = "idx_dispute_status", columnList = "status")
})
public class Dispute {

    @Id
    @Column(length = 32)
    private String id;

    /** 争议编号：DJ-YYYYMMDD-序号 */
    @Column(nullable = false, length = 64)
    private String disputeNo;

    @Column(nullable = false, length = 32)
    private String confirmationId;

    @Column(length = 64)
    private String confirmationNo;

    /** 发起方：买方 / 卖方 */
    @Column(nullable = false, length = 16)
    private String initiator;

    @Column(length = 64)
    private String initiatorName;

    @Column(length = 64)
    private String respondentName;

    /** 争议类型：WEIGHT(重量) / SPECIFICATION(规格) / QUALITY(品质) */
    @Column(nullable = false, length = 16)
    private String disputeType;

    @Column(length = 1024)
    private String description;

    /** 发起方提交的证据材料描述 */
    @Column(length = 2048)
    private String evidence;

    /** 状态：PENDING/INSPECTING/ARBITRATING/CLOSED/REJECTED */
    @Column(length = 16)
    private String status;

    /** 协调的第三方检测机构名称 */
    @Column(length = 128)
    private String assignedAgency;

    /** 最终仲裁结果（结案时填写） */
    @Column(length = 1024)
    private String arbitrateResult;

    @Column(length = 64)
    private String closedBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;
}
