package com.fishing.platform.controller;

import com.fishing.platform.common.ApiResult;
import com.fishing.platform.entity.VoyageDeclaration;
import com.fishing.platform.service.QuotaService;
import com.fishing.platform.service.VoyageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 出海申报
 */
@RestController
@RequestMapping("/api/voyage")
public class VoyageController {

    @Autowired private VoyageService voyageService;
    @Autowired private QuotaService quotaService;

    @PostMapping("/submit")
    public ApiResult<VoyageDeclaration> submit(@RequestBody VoyageDeclaration declaration) {
        // 提交前检查配额
        quotaService.checkBeforeDeparture(declaration);
        return ApiResult.ok(voyageService.submit(declaration));
    }

    @PostMapping("/depart/{id}")
    public ApiResult<VoyageDeclaration> depart(@PathVariable String id) {
        return ApiResult.ok(voyageService.depart(id));
    }

    @PostMapping("/return/{id}")
    public ApiResult<VoyageDeclaration> returnToPort(@PathVariable String id) {
        return ApiResult.ok(voyageService.returnToPort(id));
    }

    @GetMapping("/list")
    public ApiResult<List<VoyageDeclaration>> list() {
        return ApiResult.ok(voyageService.findAll());
    }

    @GetMapping("/byVessel/{vesselId}")
    public ApiResult<List<VoyageDeclaration>> byVessel(@PathVariable String vesselId) {
        return ApiResult.ok(voyageService.findByVessel(vesselId));
    }

    @GetMapping("/detail/{id}")
    public ApiResult<VoyageDeclaration> detail(@PathVariable String id) {
        return ApiResult.ok(voyageService.findById(id));
    }

    @GetMapping("/byPort")
    public ApiResult<List<VoyageDeclaration>> byPort(@RequestParam String portName,
                                                     @RequestParam Integer year) {
        return ApiResult.ok(voyageService.findByPortAndYear(portName, year));
    }
}
