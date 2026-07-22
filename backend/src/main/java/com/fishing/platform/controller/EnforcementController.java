package com.fishing.platform.controller;

import com.fishing.platform.common.ApiResult;
import com.fishing.platform.entity.ViolationNotice;
import com.fishing.platform.service.EnforcementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 执法办案
 */
@RestController
@RequestMapping("/api/enforcement")
public class EnforcementController {

    @Autowired private EnforcementService service;

    @PostMapping("/issue")
    public ApiResult<ViolationNotice> issue(@RequestBody ViolationNotice notice) {
        return ApiResult.ok(service.issue(notice));
    }

    @PostMapping("/applyQuotaDeduct/{id}")
    public ApiResult<ViolationNotice> applyDeduct(@PathVariable String id,
                                                  @RequestBody Map<String, Object> body) {
        BigDecimal deducted = new BigDecimal(String.valueOf(body.get("deducted")));
        return ApiResult.ok(service.applyQuotaDeduct(id, deducted));
    }

    @GetMapping("/list")
    public ApiResult<List<ViolationNotice>> list() {
        return ApiResult.ok(service.findAll());
    }

    @GetMapping("/byVessel/{vesselId}")
    public ApiResult<List<ViolationNotice>> byVessel(@PathVariable String vesselId) {
        return ApiResult.ok(service.findByVessel(vesselId));
    }
}
