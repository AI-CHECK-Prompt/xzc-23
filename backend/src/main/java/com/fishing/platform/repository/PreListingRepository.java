package com.fishing.platform.repository;

import com.fishing.platform.entity.PreListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PreListingRepository extends JpaRepository<PreListing, String> {

    @Query("select p from PreListing p " +
            "where (:seaArea is null or p.seaAreaName = :seaArea) " +
            "and (:species is null or p.species = :species) " +
            "and (:portName is null or p.portName = :portName) " +
            "and (:status is null or p.status = :status) " +
            "order by p.availableTime asc")
    List<PreListing> search(@Param("seaArea") String seaArea,
                            @Param("species") String species,
                            @Param("portName") String portName,
                            @Param("status") String status);

    @Query("select p from PreListing p where p.vesselId = :vesselId order by p.createdAt desc")
    List<PreListing> findByVessel(@Param("vesselId") String vesselId);
}
