package com.fishing.platform.controller;

import com.fishing.platform.common.ApiResult;
import com.fishing.platform.entity.AlertEvent;
import com.fishing.platform.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 告警事件
 */
@RestController
@RequestMapping("/api/alert")
public class AlertController {

    @Autowired private AlertService alertService;

    @GetMapping("/pending")
    public ApiResult<List<AlertEvent>> pending() {
        return ApiResult.ok(alertService.pending());
    }

    @PostMapping("/create")
    public ApiResult<AlertEvent> create(@RequestBody Map<String, String> body) {
        return ApiResult.ok(alertService.create(
                body.get("vesselId"),
                body.get("vesselNo"),
                body.get("voyageId"),
                body.getOrDefault("alertType", "异常"),
                body.getOrDefault("level", "warn"),
                body.getOrDefault("description", "")));
    }

    @PostMapping("/handle/{id}")
    public ApiResult<AlertEvent> handle(@PathVariable String id,
                                        @RequestBody Map<String, String> body) {
        return ApiResult.ok(alertService.handle(id, body.get("handler"), body.get("result")));
    }

    @GetMapping("/byVessel/{vesselId}")
    public ApiResult<List<AlertEvent>> byVessel(@PathVariable String vesselId) {
        return ApiResult.ok(alertService.findByVessel(vesselId));
    }

    @GetMapping("/byVoyage/{voyageId}")
    public ApiResult<List<AlertEvent>> byVoyage(@PathVariable String voyageId) {
        return ApiResult.ok(alertService.findByVoyage(voyageId));
    }
}
