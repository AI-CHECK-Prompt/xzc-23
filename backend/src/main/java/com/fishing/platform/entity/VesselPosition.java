package com.fishing.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 船位轨迹点（按时间序回传）
 */
@Data
@Entity
@Table(name = "vessel_position", indexes = {
        @Index(name = "idx_vessel_time", columnList = "vesselId,reportTime")
})
public class VesselPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String vesselId;
    private String vesselNo;

    private Double longitude;
    private Double latitude;

    /** 航速 节 */
    private Double speed;
    /** 航向 度 */
    private Double heading;

    private LocalDateTime reportTime;
    private LocalDateTime createdAt;
}
