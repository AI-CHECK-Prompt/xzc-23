package com.fishing.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 船舶档案
 */
@Data
@Entity
@Table(name = "vessel")
public class Vessel {

    @Id
    @Column(length = 32)
    private String id;

    /** 船号，例如 闽泉州渔运-001 */
    @Column(nullable = false, length = 64)
    private String vesselNo;

    /** 船名 */
    @Column(nullable = false, length = 64)
    private String vesselName;

    /** 船东 */
    @Column(nullable = false, length = 64)
    private String ownerName;

    /** 船长 */
    @Column(length = 64)
    private String captainName;

    /** 联系电话 */
    @Column(length = 32)
    private String phone;

    /** 所属渔港 */
    @Column(nullable = false, length = 64)
    private String portName;

    /** 所属海区 */
    @Column(nullable = false, length = 64)
    private String seaAreaName;

    /** 船舶证件有效期起 */
    private LocalDate certValidFrom;

    /** 船舶证件有效期止 */
    private LocalDate certValidTo;

    /** 是否处于违规停业整改期 */
    private boolean suspended;

    /** 停业截止日期 */
    private LocalDate suspendUntil;

    /** 状态：在港、出海、维修、停业 */
    @Column(length = 16)
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
