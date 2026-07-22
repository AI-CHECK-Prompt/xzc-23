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

    long countByStatus(String status);
}
