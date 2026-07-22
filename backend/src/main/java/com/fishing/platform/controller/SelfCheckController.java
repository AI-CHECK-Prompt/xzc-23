package com.fishing.platform.controller;

import com.fishing.platform.common.ApiResult;
import com.fishing.platform.service.AlertService;
import com.fishing.platform.service.VesselService;
import com.fishing.platform.service.VoyageService;
import com.fishing.platform.service.CatchService;
import com.fishing.platform.service.EnforcementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 自检接口
 */
@RestController
@RequestMapping("/api/selfcheck")
public class SelfCheckController {

    @Autowired private VesselService vesselService;
    @Autowired private VoyageService voyageService;
    @Autowired private AlertService alertService;
    @Autowired private CatchService catchService;
    @Autowired private EnforcementService enforcementService;

    @GetMapping("/overview")
    public ApiResult<Map<String, Object>> overview() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("vesselCount", vesselService.findAll().size());
        map.put("voyageCount", voyageService.findAll().size());
        map.put("pendingAlertCount", alertService.countPending());
        map.put("violationCount", enforcementService.findAll().size());
        return ApiResult.ok(map);
    }
}
