package com.fishing.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 告警处置审计日志
 * 每一次「处置」操作均写入一条记录，防止后续处置覆盖历史经办人与处置过程，
 * 便于违规船舶责任认定时的审计追溯。
 */
@Data
@Entity
@Table(name = "alert_handle_log", indexes = {
        @Index(name = "idx_handle_log_alert", columnList = "alertId"),
        @Index(name = "idx_handle_log_handled_at", columnList = "handledAt")
})
public class AlertHandleLog {

    @Id
    @Column(length = 32)
    private String id;

    /** 关联告警ID */
    private String alertId;

    /** 处置人姓名（海警/渔政） */
    @Column(length = 64)
    private String handler;

    /** 处置结果 */
    @Column(length = 1024)
    private String handleResult;

    /** 处置时间 */
    private LocalDateTime handledAt;

    /** 操作类型：处置 / 变更 / 驳回 等，便于后续审计区分 */
    @Column(length = 16)
    private String action;
}
