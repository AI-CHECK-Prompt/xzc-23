package com.fishing.platform.repository;

import com.fishing.platform.entity.Vessel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface VesselRepository extends JpaRepository<Vessel, String> {
    Optional<Vessel> findByVesselNo(String vesselNo);

    List<Vessel> findByPortName(String portName);

    List<Vessel> findBySeaAreaName(String seaAreaName);

    @Query("select count(v) from Vessel v")
    long countAll();
}
