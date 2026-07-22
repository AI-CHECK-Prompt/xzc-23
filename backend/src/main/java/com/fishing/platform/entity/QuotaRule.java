package com.fishing.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配额规则（按海区/品种/年度）
 */
@Data
@Entity
@Table(name = "quota_rule", indexes = {
        @Index(name = "idx_quota_unique", columnList = "year,seaAreaName,species", unique = true)
})
public class QuotaRule {

    @Id
    @Column(length = 32)
    private String id;

    private Integer year;
    private String seaAreaName;
    private String species;

    /** 年度可捕捞总量 公斤 */
    private BigDecimal totalQuota;

    /** 最小可捕规格（自然语言） */
    @Column(length = 64)
    private String minSize;

    /** 是否禁渔 */
    private boolean banned;

    private LocalDateTime createdAt;
}
