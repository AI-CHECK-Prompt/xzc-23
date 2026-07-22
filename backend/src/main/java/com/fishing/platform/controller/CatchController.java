package com.fishing.platform.controller;

import com.fishing.platform.common.ApiResult;
import com.fishing.platform.entity.CatchDeclaration;
import com.fishing.platform.service.CatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 渔获预申报
 */
@RestController
@RequestMapping("/api/catch")
public class CatchController {

    @Autowired private CatchService catchService;

    @PostMapping("/submitPre")
    public ApiResult<CatchDeclaration> submitPre(@RequestBody CatchDeclaration d) {
        return ApiResult.ok(catchService.submitPre(d));
    }

    @PostMapping("/confirmWeigh/{id}")
    public ApiResult<CatchDeclaration> confirmWeigh(@PathVariable String id,
                                                    @RequestBody Map<String, Object> body) {
        BigDecimal actual = new BigDecimal(String.valueOf(body.get("actualTotal")));
        String reason = (String) body.getOrDefault("reason", "");
        String operator = (String) body.getOrDefault("operator", "电子秤台账");
        return ApiResult.ok(catchService.confirmWeigh(id, actual, reason, operator));
    }

    @PostMapping("/reviewDeviation/{id}")
    public ApiResult<CatchDeclaration> reviewDeviation(@PathVariable String id,
                                                       @RequestBody Map<String, Object> body) {
        String reviewReason = (String) body.getOrDefault("reviewReason", "");
        String reviewer = (String) body.getOrDefault("reviewer", "渔港管理员");
        return ApiResult.ok(catchService.reviewDeviation(id, reviewReason, reviewer));
    }

    @GetMapping("/byVoyage/{voyageId}")
    public ApiResult<List<CatchDeclaration>> byVoyage(@PathVariable String voyageId) {
        return ApiResult.ok(catchService.findByVoyage(voyageId));
    }

    @GetMapping("/byVessel/{vesselId}")
    public ApiResult<List<CatchDeclaration>> byVessel(@PathVariable String vesselId) {
        return ApiResult.ok(catchService.findByVessel(vesselId));
    }
}
