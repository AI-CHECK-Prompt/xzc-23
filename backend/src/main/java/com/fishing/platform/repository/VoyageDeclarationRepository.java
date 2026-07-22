package com.fishing.platform.repository;

import com.fishing.platform.entity.VoyageDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface VoyageDeclarationRepository extends JpaRepository<VoyageDeclaration, String> {

    List<VoyageDeclaration> findByVesselIdOrderByCreatedAtDesc(String vesselId);

    @Query("select v from VoyageDeclaration v where v.vesselId = :vesselId and v.status in ('已申报','已出港') order by v.planDepartureTime desc")
    List<VoyageDeclaration> findActiveByVessel(@Param("vesselId") String vesselId);

    @Query("select v from VoyageDeclaration v where v.status in ('已申报','已出港') and v.planDepartureTime < :threshold")
    List<VoyageDeclaration> findOverdue(@Param("threshold") LocalDateTime threshold);

    @Query("select v from VoyageDeclaration v where v.year = :year")
    List<VoyageDeclaration> findByYear(@Param("year") Integer year);

    @Query("select v from VoyageDeclaration v where v.portName = :portName and v.year = :year")
    List<VoyageDeclaration> findByPortAndYear(@Param("portName") String portName, @Param("year") Integer year);

    @Query("select v from VoyageDeclaration v where v.seaAreaName = :area and v.year = :year")
    List<VoyageDeclaration> findBySeaAreaAndYear(@Param("area") String area, @Param("year") Integer year);
}
