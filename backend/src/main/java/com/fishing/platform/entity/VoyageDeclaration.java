package com.fishing.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 出海申报单
 */
@Data
@Entity
@Table(name = "voyage_declaration", indexes = {
        @Index(name = "idx_vessel_year", columnList = "vesselId,year"),
        @Index(name = "idx_status", columnList = "status")
})
public class VoyageDeclaration {

    @Id
    @Column(length = 32)
    private String id;

    /** 申报单号：航次编号，自然语言格式 本渔港-船号-YYYYMMDD-序号 */
    @Column(nullable = false, length = 64, unique = true)
    private String declarationNo;

    /** 船舶ID */
    @Column(nullable = false, length = 32)
    private String vesselId;

    private String vesselNo;
    private String vesselName;
    private String ownerName;
    private String portName;
    private String seaAreaName;

    /** 作业人员名单（JSON 字符串） */
    @Column(length = 2000)
    private String crewListJson;

    /** 计划作业海域（自然语言） */
    @Column(length = 256)
    private String planSeaArea;

    /** 计划作业天数 */
    private Integer planDays;

    /** 计划作业方式（刺网 / 围网 / 拖网 / 钓具） */
    @Column(length = 32)
    private String planMethod;

    /** 申报携带的网具规格（自然语言） */
    @Column(length = 256)
    private String netSpec;

    /** 最长允许出海天数（系统规则） */
    private Integer maxAllowedDays;

    /** 申报年份（用于按年度聚合） */
    private Integer year;

    /** 计划出港时间 */
    private LocalDateTime planDepartureTime;

    /** 实际出港时间 */
    private LocalDateTime actualDepartureTime;

    /** 实际归港时间 */
    private LocalDateTime actualReturnTime;

    /** 状态：已申报、已出港、已归港、已取消、已违规 */
    @Column(length = 16)
    private String status;

    /** 审核人 */
    private String approvedBy;

    /** 备注 */
    @Column(length = 512)
    private String remark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
