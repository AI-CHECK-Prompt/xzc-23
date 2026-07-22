package com.fishing.platform.controller;

import com.fishing.platform.common.ApiResult;
import com.fishing.platform.entity.PurchaseRecord;
import com.fishing.platform.service.PurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 采购回传
 */
@RestController
@RequestMapping("/api/purchase")
public class PurchaseController {

    @Autowired private PurchaseService service;

    @PostMapping("/report")
    public ApiResult<PurchaseRecord> report(@RequestBody PurchaseRecord r) {
        return ApiResult.ok(service.report(r));
    }

    @GetMapping("/byVessel/{vesselId}")
    public ApiResult<List<PurchaseRecord>> byVessel(@PathVariable String vesselId) {
        return ApiResult.ok(service.findByVessel(vesselId));
    }

    @GetMapping("/byVoyage/{voyageId}")
    public ApiResult<List<PurchaseRecord>> byVoyage(@PathVariable String voyageId) {
        return ApiResult.ok(service.findByVoyage(voyageId));
    }
}
