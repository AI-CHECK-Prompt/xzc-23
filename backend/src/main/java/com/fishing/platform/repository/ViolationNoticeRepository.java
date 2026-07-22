package com.fishing.platform.repository;

import com.fishing.platform.entity.ViolationNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ViolationNoticeRepository extends JpaRepository<ViolationNotice, String> {

    @Query("select v from ViolationNotice v where v.vesselId = :vesselId order by v.issueTime desc")
    List<ViolationNotice> findByVessel(@Param("vesselId") String vesselId);

    @Query("select v from ViolationNotice v where v.voyageId = :voyageId")
    List<ViolationNotice> findByVoyage(@Param("voyageId") String voyageId);
}
