package com.fishing.platform.service;

import com.fishing.platform.entity.VesselPosition;
import com.fishing.platform.repository.VesselPositionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 船位接入服务（兼容主流船位终端协议：JT/T 808 / 自有 JSON）
 */
@Service
public class PositionService {

    @Autowired private VesselPositionRepository positionRepo;

    /**
     * 接收船位数据（可对接 JT/T 808 协议适配器、卫星终端 JSON 等）
     */
    public VesselPosition ingest(VesselPosition position) {
        if (position.getReportTime() == null) {
            position.setReportTime(LocalDateTime.now());
        }
        position.setCreatedAt(LocalDateTime.now());
        return positionRepo.save(position);
    }

    public List<VesselPosition> trackOf(String vesselId, LocalDateTime from, LocalDateTime to) {
        return positionRepo.findTrack(vesselId, from, to);
    }

    public LocalDateTime latestTime(String vesselId) {
        return positionRepo.findLatestTime(vesselId);
    }
}
