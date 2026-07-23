package com.fishing.platform.repository;

import com.fishing.platform.entity.ColdChainShipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ColdChainShipmentRepository extends JpaRepository<ColdChainShipment, String> {

    Optional<ColdChainShipment> findByConfirmationId(String confirmationId);

    @Query("select s from ColdChainShipment s " +
            "where (:status is null or s.status = :status) " +
            "order by s.createdAt desc")
    List<ColdChainShipment> search(@Param("status") String status);
}
