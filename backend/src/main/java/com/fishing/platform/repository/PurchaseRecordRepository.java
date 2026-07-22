package com.fishing.platform.repository;

import com.fishing.platform.entity.PurchaseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PurchaseRecordRepository extends JpaRepository<PurchaseRecord, String> {

    @Query("select p from PurchaseRecord p where p.vesselId = :vesselId order by p.purchaseTime desc")
    List<PurchaseRecord> findByVessel(@Param("vesselId") String vesselId);

    @Query("select p from PurchaseRecord p where p.voyageId = :voyageId")
    List<PurchaseRecord> findByVoyage(@Param("voyageId") String voyageId);
}
