package com.fishing.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 异常告警事件
 */
@Data
@Entity
@Table(name = "alert_event", indexes = {
        @Index(name = "idx_alert_vessel", columnList = "vesselId"),
        @Index(name = "idx_alert_type", columnList = "alertType"),
        // 用于「同船同航次同告警类型待处理」去重查询
        @Index(name = "idx_alert_dedup", columnList = "vesselId,voyageId,alertType,status")
})
public class AlertEvent {

    @Id
    @Column(length = 32)
    private String id;

    private String vesselId;
    private String vesselNo;
    private String voyageId;

    /** 告警类型：超出申报海域、长时间失联、关闭船位终端、预申报偏差等 */
    @Column(length = 32)
    private String alertType;

    /** 告警级别：info/warn/danger */
    @Column(length = 16)
    private String level;

    /** 描述（自然语言） */
    @Column(length = 512)
    private String description;

    /** 状态：待处理、已派发、已处置、已忽略 */
    @Column(length = 16)
    private String status;

    private String handler;
    private String handleResult;

    private LocalDateTime createdAt;
    private LocalDateTime handledAt;
}
