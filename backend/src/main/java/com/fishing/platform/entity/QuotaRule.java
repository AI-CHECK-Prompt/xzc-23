package com.fishing.platform.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
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
        @Index(name = "idx_quota_unique", columnList = "quota_year,seaAreaName,species", unique = true)
})
public class QuotaRule {

    @Id
    @Column(length = 32)
    private String id;

    /** 年度（重命名以避免与 H2 保留字 year 冲突；JSON 上保留 "year" 以兼容现有前端） */
    @Column(name = "quota_year")
    @JsonProperty("year")
    private Integer quotaYear;
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
