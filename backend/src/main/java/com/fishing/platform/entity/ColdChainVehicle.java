package com.fishing.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 冷链车辆
 */
@Data
@Entity
@Table(name = "cold_chain_vehicle", indexes = {
        @Index(name = "idx_ccv_vehicle_no", columnList = "vehicleNo", unique = true),
        @Index(name = "idx_ccv_status", columnList = "status")
})
public class ColdChainVehicle {

    @Id
    @Column(length = 32)
    private String id;

    /** 车号 */
    @Column(nullable = false, length = 32)
    private String vehicleNo;

    /** 司机 */
    @Column(length = 64)
    private String driverName;

    @Column(length = 32)
    private String driverPhone;

    /** 车型：小型冷藏车/中型/大型 */
    @Column(length = 32)
    private String vehicleType;

    /** 载重（kg） */
    private java.math.BigDecimal capacity;

    /** 状态：IDLE / LOADING / IN_TRANSIT / MAINTENANCE */
    @Column(length = 16)
    private String status;

    private LocalDateTime createdAt;
}
