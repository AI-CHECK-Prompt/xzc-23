package com.fishing.platform.repository;

import com.fishing.platform.entity.InspectionReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InspectionReportRepository extends JpaRepository<InspectionReport, String> {

    Optional<InspectionReport> findByReportNo(String reportNo);

    List<InspectionReport> findByDisputeId(String disputeId);
}
