package com.fishing.platform.service;

import com.fishing.platform.entity.AlertEvent;
import com.fishing.platform.entity.VesselPosition;
import com.fishing.platform.entity.VoyageDeclaration;
import com.fishing.platform.repository.AlertEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 告警服务：越界、失联、关闭终端等
 */
@Service
public class AlertService {

    @Autowired private AlertEventRepository alertRepo;
    @Autowired private PositionService positionService;

    @Transactional
    public AlertEvent create(String vesselId, String vesselNo, String voyageId,
                             String alertType, String level, String description) {
        AlertEvent a = new AlertEvent();
        a.setId(UUID.randomUUID().toString().replace("-", ""));
        a.setVesselId(vesselId);
        a.setVesselNo(vesselNo);
        a.setVoyageId(voyageId);
        a.setAlertType(alertType);
        a.setLevel(level);
        a.setDescription(description);
        a.setStatus("待处理");
        a.setCreatedAt(LocalDateTime.now());
        return alertRepo.save(a);
    }

    @Transactional
    public AlertEvent handle(String alertId, String handler, String result) {
        AlertEvent a = alertRepo.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("告警不存在"));
        a.setStatus("已处置");
        a.setHandler(handler);
        a.setHandleResult(result);
        a.setHandledAt(LocalDateTime.now());
        return alertRepo.save(a);
    }

    public List<AlertEvent> pending() {
        return alertRepo.findPending();
    }

    public List<AlertEvent> findByVessel(String vesselId) {
        return alertRepo.findByVessel(vesselId);
    }

    public List<AlertEvent> findByVoyage(String voyageId) {
        return alertRepo.findByVoyage(voyageId);
    }

    /**
     * 检测船位异常：长时间失联 / 关闭终端 / 越界
     * 简易策略：最后上报时间距今 > 2 小时视为失联；无最后位置视为终端关闭
     */
    public void detectAbnormal(VoyageDeclaration voyage) {
        if (voyage == null || !"已出港".equals(voyage.getStatus())) {
            return;
        }
        LocalDateTime last = positionService.latestTime(voyage.getVesselId());
        if (last == null) {
            create(voyage.getVesselId(), voyage.getVesselNo(), voyage.getId(),
                    "关闭船位终端", "danger",
                    "船舶 " + voyage.getVesselNo() + " 长时间无船位回传，可能关闭了船位终端");
            return;
        }
        long minutes = java.time.Duration.between(last, LocalDateTime.now()).toMinutes();
        if (minutes > 120) {
            create(voyage.getVesselId(), voyage.getVesselNo(), voyage.getId(),
                    "长时间失联", "danger",
                    "船舶 " + voyage.getVesselNo() + " 已 " + minutes + " 分钟未回传船位，疑似失联");
        }
    }

    public long countPending() {
        return alertRepo.countByStatus("待处理");
    }
}
