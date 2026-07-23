package com.fishing.platform.controller;

import com.fishing.platform.common.ApiResult;
import com.fishing.platform.entity.PriceIndex;
import com.fishing.platform.service.PriceIndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 渔获价格指数
 */
@RestController
@RequestMapping("/api/price-index")
public class PriceIndexController {

    @Autowired private PriceIndexService service;

    /** 触发最新一期（昨日/上周/上月） */
    @PostMapping("/calculate")
    public ApiResult<Map<String, Integer>> calculateLatest() {
        return ApiResult.ok(service.calculateLatest());
    }

    /** 对指定周期单次计算 */
    @PostMapping("/calculate/{periodType}/{periodKey}")
    public ApiResult<List<PriceIndex>> calculateOne(@PathVariable String periodType,
                                                    @PathVariable String periodKey) {
        return ApiResult.ok(service.calculate(periodType, periodKey));
    }

    /** 多维查询 */
    @GetMapping("/query")
    public ApiResult<List<PriceIndex>> query(@RequestParam(required = false) String seaArea,
                                             @RequestParam(required = false) String species,
                                             @RequestParam(required = false) String specification,
                                             @RequestParam(required = false) String periodType) {
        return ApiResult.ok(service.query(seaArea, species, specification, periodType));
    }

    /** 趋势：按维度取某 periodType 的时序 */
    @GetMapping("/trend")
    public ApiResult<List<PriceIndex>> trend(@RequestParam String seaArea,
                                             @RequestParam String species,
                                             @RequestParam String specification,
                                             @RequestParam(required = false, defaultValue = "全部") String season,
                                             @RequestParam(required = false, defaultValue = "DAY") String periodType) {
        return ApiResult.ok(service.trend(seaArea, species, specification, season, periodType));
    }

    /** 重建历史区间内所有 DAY/WEEK/MONTH */
    @PostMapping("/rebuild")
    public ApiResult<Map<String, Integer>> rebuild(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResult.ok(service.rebuild(from, to));
    }
}
