package com.fishing.platform.repository;

import com.fishing.platform.entity.QuotaRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface QuotaRuleRepository extends JpaRepository<QuotaRule, String> {

    @Query("select q from QuotaRule q where q.year = :year and q.seaAreaName = :area and q.species = :species")
    Optional<QuotaRule> findRule(@Param("year") Integer year,
                                 @Param("area") String area,
                                 @Param("species") String species);

    @Query("select q from QuotaRule q where q.year = :year")
    List<QuotaRule> findByYear(@Param("year") Integer year);
}
