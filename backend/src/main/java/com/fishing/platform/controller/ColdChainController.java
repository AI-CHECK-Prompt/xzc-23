package com.fishing.platform.controller;

import com.fishing.platform.common.ApiResult;
import com.fishing.platform.entity.ColdChainShipment;
import com.fishing.platform.entity.ColdChainTempReading;
import com.fishing.platform.entity.ColdChainVehicle;
import com.fishing.platform.service.ColdChainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 冷链配送协同
 */
@RestController
@RequestMapping("/api/cold-chain")
public class ColdChainController {

    @Autowired private ColdChainService service;

    // ---- 车辆 ----
    @PostMapping("/vehicle/register")
    public ApiResult<ColdChainVehicle> registerVehicle(@RequestBody ColdChainVehicle v) {
        return ApiResult.ok(service.registerVehicle(v));
    }

    @GetMapping("/vehicle/list")
    public ApiResult<List<ColdChainVehicle>> listVehicles() {
        return ApiResult.ok(service.listVehicles());
    }

    // ---- 运单 ----
    @PostMapping("/shipment/create")
    public ApiResult<ColdChainShipment> createShipment(@RequestParam String confirmationId,
                                                       @RequestParam String vehicleId,
                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime plannedDeparture,
                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime plannedArrival) {
        return ApiResult.ok(service.createShipment(confirmationId, vehicleId, plannedDeparture, plannedArrival));
    }

    @PostMapping("/shipment/depart/{id}")
    public ApiResult<ColdChainShipment> depart(@PathVariable String id) {
        return ApiResult.ok(service.depart(id));
    }

    @PostMapping("/shipment/arrive/{id}")
    public ApiResult<ColdChainShipment> arrive(@PathVariable String id) {
        return ApiResult.ok(service.arrive(id));
    }

    @GetMapping("/shipment/{id}")
    public ApiResult<Map<String, Object>> detail(@PathVariable String id) {
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("shipment", service.getShipment(id));
        r.put("readings", service.getReadings(id));
        return ApiResult.ok(r);
    }

    @GetMapping("/shipment/list")
    public ApiResult<List<ColdChainShipment>> listShipments(@RequestParam(required = false) String status) {
        return ApiResult.ok(service.listShipments(status));
    }

    // ---- 温度 ----
    @PostMapping("/temperature/report")
    public ApiResult<ColdChainTempReading> reportTemperature(@RequestParam String shipmentId,
                                                             @RequestParam BigDecimal temperature,
                                                             @RequestParam(required = false) String source) {
        return ApiResult.ok(service.reportTemperature(shipmentId, temperature, source));
    }
}
