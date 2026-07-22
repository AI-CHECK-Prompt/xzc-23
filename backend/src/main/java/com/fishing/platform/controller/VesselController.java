package com.fishing.platform.controller;

import com.fishing.platform.common.ApiResult;
import com.fishing.platform.entity.Vessel;
import com.fishing.platform.service.VesselService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 船舶档案
 */
@RestController
@RequestMapping("/api/vessel")
public class VesselController {

    @Autowired private VesselService service;

    @PostMapping("/save")
    public ApiResult<Vessel> save(@RequestBody Vessel vessel) {
        return ApiResult.ok(service.save(vessel));
    }

    @GetMapping("/list")
    public ApiResult<List<Vessel>> list() {
        return ApiResult.ok(service.findAll());
    }

    @GetMapping("/detail/{id}")
    public ApiResult<Vessel> detail(@PathVariable String id) {
        return service.findById(id).map(ApiResult::ok)
                .orElse(ApiResult.fail(404, "船舶不存在"));
    }

    @GetMapping("/byNo")
    public ApiResult<Vessel> byNo(@RequestParam String vesselNo) {
        return service.findByVesselNo(vesselNo).map(ApiResult::ok)
                .orElse(ApiResult.fail(404, "船舶不存在"));
    }
}
