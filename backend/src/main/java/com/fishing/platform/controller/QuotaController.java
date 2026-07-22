package com.fishing.platform.controller;

import com.fishing.platform.common.ApiResult;
import com.fishing.platform.entity.QuotaRule;
import com.fishing.platform.service.QuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 配额管控
 */
@RestController
@RequestMapping("/api/quota")
public class QuotaController {

    @Autowired private QuotaService service;

    @PostMapping("/rule/save")
    public ApiResult<QuotaRule> saveRule(@RequestBody QuotaRule rule) {
        return ApiResult.ok(service.saveRule(rule));
    }

    @GetMapping("/rule/list")
    public ApiResult<List<QuotaRule>> listRules(@RequestParam(required = false) Integer year) {
        return ApiResult.ok(service.listRules(year));
    }

    @GetMapping("/summary/owner")
    public ApiResult<Map<String, Object>> summaryForOwner(
            @RequestParam String owner,
            @RequestParam(required = false) String portName,
            @RequestParam(required = false) Integer year) {
        return ApiResult.ok(service.summaryForOwner(owner, portName, year));
    }

    @GetMapping("/summary/seaArea")
    public ApiResult<Map<String, Object>> summaryBySeaArea(
            @RequestParam String seaArea,
            @RequestParam(required = false) Integer year) {
        return ApiResult.ok(service.summaryBySeaArea(seaArea, year));
    }

    @GetMapping("/summary/port")
    public ApiResult<Map<String, Object>> summaryByPort(
            @RequestParam String portName,
            @RequestParam(required = false) Integer year) {
        return ApiResult.ok(service.summaryByPort(portName, year));
    }
}
