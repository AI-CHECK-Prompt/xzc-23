package com.fishing.platform.controller;

import com.fishing.platform.common.ApiResult;
import com.fishing.platform.entity.Dispute;
import com.fishing.platform.entity.InspectionReport;
import com.fishing.platform.service.DisputeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 争议处理
 */
@RestController
@RequestMapping("/api/dispute")
public class DisputeController {

    @Autowired private DisputeService service;

    // ---- 争议 ----
    @PostMapping("/open")
    public ApiResult<Dispute> open(@RequestBody Dispute d) {
        return ApiResult.ok(service.open(d));
    }

    @PostMapping("/assignAgency")
    public ApiResult<Dispute> assignAgency(@RequestParam String disputeId,
                                           @RequestParam String agencyName) {
        return ApiResult.ok(service.assignAgency(disputeId, agencyName));
    }

    @PostMapping("/close")
    public ApiResult<Dispute> close(@RequestParam String disputeId,
                                    @RequestParam String result,
                                    @RequestParam(required = false) String closedBy) {
        return ApiResult.ok(service.close(disputeId, result, closedBy));
    }

    @PostMapping("/reject")
    public ApiResult<Dispute> reject(@RequestParam String disputeId,
                                     @RequestParam(required = false) String reason,
                                     @RequestParam(required = false) String closedBy) {
        return ApiResult.ok(service.reject(disputeId, reason, closedBy));
    }

    @GetMapping("/{id}")
    public ApiResult<Map<String, Object>> detail(@PathVariable String id) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("dispute", service.get(id));
        r.put("reports", service.reportsOf(id));
        return ApiResult.ok(r);
    }

    @GetMapping("/byNo/{disputeNo}")
    public ApiResult<Dispute> byNo(@PathVariable String disputeNo) {
        return ApiResult.ok(service.getByNo(disputeNo));
    }

    @GetMapping("/list")
    public ApiResult<List<Dispute>> list(@RequestParam(required = false) String status) {
        return ApiResult.ok(service.list(status));
    }

    @GetMapping("/byConfirmation/{confirmationId}")
    public ApiResult<List<Dispute>> byConfirmation(@PathVariable String confirmationId) {
        return ApiResult.ok(service.listByConfirmation(confirmationId));
    }

    // ---- 检测报告 ----
    @PostMapping("/report/submit")
    public ApiResult<InspectionReport> submitReport(@RequestBody InspectionReport r) {
        return ApiResult.ok(service.submitReport(r));
    }

    @GetMapping("/report/list/{disputeId}")
    public ApiResult<List<InspectionReport>> listReports(@PathVariable String disputeId) {
        return ApiResult.ok(service.reportsOf(disputeId));
    }
}
