package com.fishing.platform.controller;

import com.fishing.platform.common.ApiResult;
import com.fishing.platform.entity.VesselPosition;
import com.fishing.platform.service.AlertService;
import com.fishing.platform.service.PositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 船位接入（兼容 JT/T 808 与通用 JSON）
 */
@RestController
@RequestMapping("/api/position")
public class PositionController {

    @Autowired private PositionService positionService;
    @Autowired private AlertService alertService;

    /** 通用 JSON 接入：移动端 / 卫星终端 / 模拟器 */
    @PostMapping("/ingest")
    public ApiResult<VesselPosition> ingest(@RequestBody VesselPosition position) {
        return ApiResult.ok(positionService.ingest(position));
    }

    /** 批量接入：模拟器、终端协议适配器 */
    @PostMapping("/ingestBatch")
    public ApiResult<Integer> ingestBatch(@RequestBody List<VesselPosition> positions) {
        int n = 0;
        for (VesselPosition p : positions) {
            positionService.ingest(p);
            n++;
        }
        return ApiResult.ok(n);
    }

    @GetMapping("/track")
    public ApiResult<List<VesselPosition>> track(@RequestParam String vesselId,
                                                 @RequestParam String from,
                                                 @RequestParam String to) {
        LocalDateTime f = LocalDateTime.parse(from);
        LocalDateTime t = LocalDateTime.parse(to);
        return ApiResult.ok(positionService.trackOf(vesselId, f, t));
    }
}
