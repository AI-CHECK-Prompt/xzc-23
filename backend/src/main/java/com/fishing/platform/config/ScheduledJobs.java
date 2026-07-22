package com.fishing.platform.config;

import com.fishing.platform.entity.VoyageDeclaration;
import com.fishing.platform.repository.VoyageDeclarationRepository;
import com.fishing.platform.service.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时任务：周期性检测出港船舶异常
 */
@Component
public class ScheduledJobs {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobs.class);

    @Autowired private AlertService alertService;
    @Autowired private VoyageDeclarationRepository voyageRepo;

    /** 每 5 分钟扫描一次 */
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void detectAbnormal() {
        List<VoyageDeclaration> active = voyageRepo.findAll().stream()
                .filter(v -> "已出港".equals(v.getStatus())).toList();
        for (VoyageDeclaration v : active) {
            try {
                alertService.detectAbnormal(v);
            } catch (Exception e) {
                log.warn("异常检测失败：{}", e.getMessage());
            }
        }
    }
}
