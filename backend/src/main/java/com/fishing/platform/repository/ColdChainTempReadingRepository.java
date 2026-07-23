package com.fishing.platform.repository;

import com.fishing.platform.entity.ColdChainTempReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ColdChainTempReadingRepository extends JpaRepository<ColdChainTempReading, String> {

    @Query("select t from ColdChainTempReading t where t.shipmentId = :shipmentId order by t.recordedAt asc")
    List<ColdChainTempReading> findByShipment(@Param("shipmentId") String shipmentId);

    @Query("select count(t) from ColdChainTempReading t where t.shipmentId = :shipmentId and t.anomaly = true")
    long countAnomaly(@Param("shipmentId") String shipmentId);
}
