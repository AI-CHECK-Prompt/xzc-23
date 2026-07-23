package com.fishing.platform.repository;

import com.fishing.platform.entity.AlertHandleLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlertHandleLogRepository extends JpaRepository<AlertHandleLog, String> {

    /**
     * 查询某条告警的全部处置审计记录，按处置时间倒序
     */
    @Query("select l from AlertHandleLog l where l.alertId = :alertId order by l.handledAt desc")
    List<AlertHandleLog> findByAlertId(@Param("alertId") String alertId);
}
