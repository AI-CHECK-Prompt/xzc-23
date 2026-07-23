package com.fishing.platform.repository;

import com.fishing.platform.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DisputeRepository extends JpaRepository<Dispute, String> {

    Optional<Dispute> findByDisputeNo(String disputeNo);

    List<Dispute> findByConfirmationId(String confirmationId);

    @Query("select d from Dispute d " +
            "where (:status is null or d.status = :status) " +
            "order by d.createdAt desc")
    List<Dispute> search(@Param("status") String status);
}
