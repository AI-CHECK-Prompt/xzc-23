package com.fishing.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 第三方检测报告
 * <p>
 * 由协调的检测机构上传，作为争议仲裁的事实依据。
 * 报告编号唯一，便于监管留痕与下载归档。
 */
@Data
@Entity
@Table(name = "inspection_report", indexes = {
        @Index(name = "idx_report_no", columnList = "reportNo", unique = true),
        @Index(name = "idx_report_dispute", columnList = "disputeId")
})
public class InspectionReport {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 64)
    private String reportNo;

    @Column(nullable = false, length = 32)
    private String disputeId;

    @Column(nullable = false, length = 128)
    private String agencyName;

    /** 检测类型：WEIGHT / SPECIFICATION / QUALITY */
    @Column(length = 16)
    private String reportType;

    /** 实测重量（kg） */
    private BigDecimal measuredWeight;
    /** 实测规格（如：体长 cm） */
    @Column(length = 64)
    private String measuredSpec;
    /** 品质等级 */
    @Column(length = 32)
    private String qualityGrade;

    /** 检测结论摘要 */
    @Column(length = 2048)
    private String conclusion;

    /** 检测方法 / 依据标准 */
    @Column(length = 256)
    private String method;

    /** 报告文件 URL（无文件存储时存放摘要路径） */
    @Column(length = 512)
    private String reportUrl;

    @Column(length = 64)
    private String inspector;

    private LocalDateTime issuedAt;
    private LocalDateTime createdAt;
}
