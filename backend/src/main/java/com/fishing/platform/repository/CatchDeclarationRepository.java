package com.fishing.platform.repository;

import com.fishing.platform.entity.CatchDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CatchDeclarationRepository extends JpaRepository<CatchDeclaration, String> {

    @Query("select c from CatchDeclaration c where c.vesselId = :vesselId order by c.createdAt desc")
    List<CatchDeclaration> findByVessel(@Param("vesselId") String vesselId);

    @Query("select c from CatchDeclaration c where c.voyageId = :voyageId")
    List<CatchDeclaration> findByVoyage(@Param("voyageId") String voyageId);

    @Query("select c from CatchDeclaration c where c.ownerName = :owner and c.portName = :port")
    List<CatchDeclaration> findByOwnerAndPort(@Param("owner") String owner, @Param("port") String port);
}
