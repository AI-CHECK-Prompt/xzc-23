package com.fishing.platform.repository;

import com.fishing.platform.entity.ColdChainVehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ColdChainVehicleRepository extends JpaRepository<ColdChainVehicle, String> {
    Optional<ColdChainVehicle> findByVehicleNo(String vehicleNo);
    List<ColdChainVehicle> findByStatus(String status);
}
