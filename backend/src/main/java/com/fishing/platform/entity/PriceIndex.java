package com.fishing.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 渔获价格指数
 * <p>
 * 按 海区 + 品种 + 规格 + 季节 + 周期 维度计算，
 * 指数剔除异常高价与异常低价，保留中位数与分位数。
 */
@Data
@Entity
@Table(name = "price_index",
        uniqueConstraints = @UniqueConstraint(name = "uk_idx_dim_period",
                columnNames = {"seaArea", "species", "specification", "season", "periodType", "periodKey"}),
        indexes = {
                @Index(name = "idx_idx_period", columnList = "periodType,periodKey"),
                @Index(name = "idx_idx_species", columnList = "species,seaArea")
        })
public class PriceIndex {

    @Id
    @Column(length = 32)
    private String id;

    /** 海区 */
    @Column(nullable = false, length = 64)
    private String seaArea;

    /** 品种 */
    @Column(nullable = false, length = 32)
    private String species;

    /** 规格：按重量桶派生：小/中/大 */
    @Column(nullable = false, length = 16)
    private String specification;

    /** 季节：春/夏/秋/冬 */
    @Column(nullable = false, length = 16)
    private String season;

    /** 周期类型：DAY / WEEK / MONTH */
    @Column(nullable = false, length = 8)
    private String periodType;

    /** 周期键：YYYY-MM-DD / YYYY-Www / YYYY-MM */
    @Column(nullable = false, length = 16)
    private String periodKey;

    /** 中位数 (P50) */
    private BigDecimal median;

    private BigDecimal p25;
    private BigDecimal p75;
    private BigDecimal p5;
    private BigDecimal p95;

    /** 总样本数（含被剔除的异常值） */
    private Integer sampleSize;

    /** 被剔除的异常值数量 */
    private Integer anomalyFiltered;

    private LocalDateTime calculatedAt;
}
