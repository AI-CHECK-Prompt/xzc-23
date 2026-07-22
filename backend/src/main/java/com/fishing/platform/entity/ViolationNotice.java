package com.fishing.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 电子违规告知书
 */
@Data
@Entity
@Table(name = "violation_notice", indexes = {
        @Index(name = "idx_violation_vessel", columnList = "vesselId")
})
public class ViolationNotice {

    @Id
    @Column(length = 32)
    private String id;

    @Column(length = 64, unique = true)
    private String noticeNo;

    private String vesselId;
    private String vesselNo;
    private String voyageId;

    /** 违规类型：越界、禁渔期捕捞、幼鱼比例超标、未开启船位终端等 */
    @Column(length = 32)
    private String violationType;

    /** 违规描述（自然语言） */
    @Column(length = 1024)
    private String description;

    /** 扣减配额 公斤 */
    private java.math.BigDecimal quotaDeducted;

    /** 关联的航次编号 */
    private String relatedDeclarationNo;

    /** 开具人（海警/渔政） */
    private String officerName;

    /** 状态：已开具、已送达、已申诉、已生效 */
    @Column(length = 16)
    private String status;

    private LocalDateTime issueTime;
    private LocalDateTime createdAt;
}
