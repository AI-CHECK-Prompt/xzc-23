package com.fishing.platform.repository;

import com.fishing.platform.entity.AlertEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AlertEventRepository extends JpaRepository<AlertEvent, String> {

    @Query("select a from AlertEvent a where a.status = '待处理' order by a.createdAt desc")
    List<AlertEvent> findPending();

    @Query("select a from AlertEvent a where a.vesselId = :vesselId order by a.createdAt desc")
    List<AlertEvent> findByVessel(@Param("vesselId") String vesselId);

    @Query("select a from AlertEvent a where a.voyageId = :voyageId")
    List<AlertEvent> findByVoyage(@Param("voyageId") String voyageId);

    /**
     * 查询同船同航次同告警类型下处于「待处理」的告警。
     * 用于异常检测去重：同一异常在管理员处置前只保留一条待处理告警。
     */
    @Query("select a from AlertEvent a where a.vesselId = :vesselId " +
           "and a.voyageId = :voyageId and a.alertType = :alertType " +
           "and a.status = '待处理' order by a.createdAt desc")
    List<AlertEvent> findPendingByVesselVoyageType(@Param("vesselId") String vesselId,
                                                   @Param("voyageId") String voyageId,
                                                   @Param("alertType") String alertType);

    long countByStatus(String status);
}
