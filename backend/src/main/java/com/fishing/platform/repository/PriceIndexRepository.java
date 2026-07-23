package com.fishing.platform.repository;

import com.fishing.platform.entity.PriceIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PriceIndexRepository extends JpaRepository<PriceIndex, String> {

    @Query("select p from PriceIndex p " +
            "where (:seaArea is null or p.seaArea = :seaArea) " +
            "and (:species is null or p.species = :species) " +
            "and (:specification is null or p.specification = :specification) " +
            "and (:periodType is null or p.periodType = :periodType) " +
            "order by p.periodType, p.periodKey desc, p.seaArea, p.species")
    List<PriceIndex> query(@Param("seaArea") String seaArea,
                           @Param("species") String species,
                           @Param("specification") String specification,
                           @Param("periodType") String periodType);

    @Query("select p from PriceIndex p " +
            "where p.seaArea = :seaArea and p.species = :species " +
            "and p.specification = :specification and p.season = :season " +
            "and p.periodType = :periodType " +
            "order by p.periodKey asc")
    List<PriceIndex> trend(@Param("seaArea") String seaArea,
                           @Param("species") String species,
                           @Param("specification") String specification,
                           @Param("season") String season,
                           @Param("periodType") String periodType);

    @Modifying
    @Transactional
    @Query("delete from PriceIndex p where p.periodType = :periodType and p.periodKey = :periodKey")
    int deleteByPeriod(@Param("periodType") String periodType, @Param("periodKey") String periodKey);
}
