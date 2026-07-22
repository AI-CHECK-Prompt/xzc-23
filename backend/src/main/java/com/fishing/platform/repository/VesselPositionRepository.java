package com.fishing.platform.repository;

import com.fishing.platform.entity.VesselPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface VesselPositionRepository extends JpaRepository<VesselPosition, Long> {

    @Query("select p from VesselPosition p where p.vesselId = :vesselId and p.reportTime between :from and :to order by p.reportTime asc")
    List<VesselPosition> findTrack(@Param("vesselId") String vesselId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    @Query("select max(p.reportTime) from VesselPosition p where p.vesselId = :vesselId")
    LocalDateTime findLatestTime(@Param("vesselId") String vesselId);
}
