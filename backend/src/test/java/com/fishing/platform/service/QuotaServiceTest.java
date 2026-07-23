package com.fishing.platform.service;

import com.fishing.platform.common.BusinessException;
import com.fishing.platform.entity.CatchDeclaration;
import com.fishing.platform.entity.QuotaRule;
import com.fishing.platform.entity.Vessel;
import com.fishing.platform.entity.ViolationNotice;
import com.fishing.platform.entity.VoyageDeclaration;
import com.fishing.platform.repository.CatchDeclarationRepository;
import com.fishing.platform.repository.QuotaRuleRepository;
import com.fishing.platform.repository.VesselRepository;
import com.fishing.platform.repository.ViolationNoticeRepository;
import com.fishing.platform.repository.VoyageDeclarationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * QuotaService 回归测试
 * 覆盖场景：同一船东名下船只在多个海区捕获同一品种时，
 * 配额累计必须按 (海区 + 品种) 隔离，避免跨海区透支。
 */
@ExtendWith(MockitoExtension.class)
class QuotaServiceTest {

    @Mock private QuotaRuleRepository ruleRepo;
    @Mock private CatchDeclarationRepository catchRepo;
    @Mock private ViolationNoticeRepository violationRepo;
    @Mock private VoyageDeclarationRepository voyageRepo;
    @Mock private VesselRepository vesselRepo;

    @InjectMocks private QuotaService service;

    private final int year = LocalDate.now().getYear();

    private Vessel vessel(String id, String owner, String port, String seaArea) {
        Vessel v = new Vessel();
        v.setId(id);
        v.setVesselNo("V-" + id);
        v.setVesselName("船" + id);
        v.setOwnerName(owner);
        v.setPortName(port);
        v.setSeaAreaName(seaArea);
        v.setStatus("在港");
        return v;
    }

    private QuotaRule rule(String id, String seaArea, String species, String qty) {
        QuotaRule r = new QuotaRule();
        r.setId(id);
        r.setQuotaYear(year);
        r.setSeaAreaName(seaArea);
        r.setSpecies(species);
        r.setTotalQuota(new BigDecimal(qty));
        r.setBanned(false);
        return r;
    }

    private CatchDeclaration catchDecl(String id, String vesselId, String voyageId,
                                       String itemsJson, BigDecimal actualTotal, String status) {
        CatchDeclaration c = new CatchDeclaration();
        c.setId(id);
        c.setVesselId(vesselId);
        c.setVoyageId(voyageId);
        c.setItemsJson(itemsJson);
        c.setActualTotal(actualTotal);
        c.setStatus(status);
        return c;
    }

    private VoyageDeclaration voyage(String id, int yr) {
        VoyageDeclaration v = new VoyageDeclaration();
        v.setId(id);
        v.setDeclarationYear(yr);
        v.setStatus("已申报");
        return v;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> ruleBySeaAreaAndSpecies(List<Map<String, Object>> rules,
                                                       String seaArea, String species) {
        return rules.stream()
                .filter(m -> seaArea.equals(m.get("seaArea")) && species.equals(m.get("species")))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "未找到规则: 海区=" + seaArea + ", 品种=" + species));
    }

    /**
     * 核心回归：跨海区同一品种渔获不能互相透支
     * 场景：船东 A 在闽南 100kg（额度 100）+ 闽东 80kg（额度 100）
     * 修复后：闽南已用=100 剩余=0；闽东已用=80 剩余=20
     * 修复前：闽南已用=180 剩余=-80；闽东已用=180 剩余=-80（错误地把闽东的渔获算到闽南头上）
     */
    @Test
    void crossSeaAreaCatchShouldNotCauseOverdraft() {
        String owner = "船东A";
        Vessel vMinNan = vessel("v1", owner, "泉州祥芝渔港", "闽南近海渔区");
        Vessel vMinDong = vessel("v2", owner, "福州连江黄岐渔港", "闽东近海渔区");

        QuotaRule rMinNan = rule("r1", "闽南近海渔区", "带鱼", "100");
        QuotaRule rMinDong = rule("r2", "闽东近海渔区", "带鱼", "100");

        VoyageDeclaration voyage1 = voyage("vy1", year);
        VoyageDeclaration voyage2 = voyage("vy2", year);

        // v1 闽南带鱼 100kg；v2 闽东带鱼 80kg
        CatchDeclaration c1 = catchDecl("c1", "v1", "vy1",
                "[{\"species\":\"带鱼\",\"actualWeight\":100}]", new BigDecimal("100"), "已完成");
        CatchDeclaration c2 = catchDecl("c2", "v2", "vy2",
                "[{\"species\":\"带鱼\",\"actualWeight\":80}]", new BigDecimal("80"), "已完成");

        when(vesselRepo.findAll()).thenReturn(List.of(vMinNan, vMinDong));
        when(catchRepo.findByVessel("v1")).thenReturn(List.of(c1));
        when(catchRepo.findByVessel("v2")).thenReturn(List.of(c2));
        when(voyageRepo.findById("vy1")).thenReturn(Optional.of(voyage1));
        when(voyageRepo.findById("vy2")).thenReturn(Optional.of(voyage2));
        when(violationRepo.findByVessel("v1")).thenReturn(List.of());
        when(violationRepo.findByVessel("v2")).thenReturn(List.of());
        when(ruleRepo.findByYear(year)).thenReturn(List.of(rMinNan, rMinDong));

        Map<String, Object> summary = service.summaryForOwner(owner, null, year);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rules = (List<Map<String, Object>>) summary.get("rules");

        Map<String, Object> minNan = ruleBySeaAreaAndSpecies(rules, "闽南近海渔区", "带鱼");
        Map<String, Object> minDong = ruleBySeaAreaAndSpecies(rules, "闽东近海渔区", "带鱼");

        // 修复后：每个海区已用量只来自本海区
        assertEquals(new BigDecimal("100"), minNan.get("used"));
        assertEquals(new BigDecimal("0"), minNan.get("remaining"));
        assertEquals(new BigDecimal("80"), minDong.get("used"));
        assertEquals(new BigDecimal("20"), minDong.get("remaining"));

        // 总额 = 各海区之和，与分品种明细一致
        assertEquals(new BigDecimal("180"), summary.get("totalUsed"));
        assertEquals(new BigDecimal("20"), summary.get("totalRemaining"));
    }

    /**
     * 边界：某海区超额应被 checkBeforeDeparture 正确拦截
     */
    @Test
    void checkBeforeDepartureShouldBlockWhenAnySeaAreaOverdraft() {
        String owner = "船东A";
        Vessel vMinNan = vessel("v1", owner, "泉州祥芝渔港", "闽南近海渔区");

        QuotaRule rMinNan = rule("r1", "闽南近海渔区", "带鱼", "100");

        VoyageDeclaration voyage1 = voyage("vy1", year);

        CatchDeclaration c1 = catchDecl("c1", "v1", "vy1",
                "[{\"species\":\"带鱼\",\"actualWeight\":150}]", new BigDecimal("150"), "已完成");

        when(vesselRepo.findById("v1")).thenReturn(Optional.of(vMinNan));
        when(vesselRepo.findAll()).thenReturn(List.of(vMinNan));
        when(catchRepo.findByVessel("v1")).thenReturn(List.of(c1));
        when(voyageRepo.findById("vy1")).thenReturn(Optional.of(voyage1));
        when(violationRepo.findByVessel("v1")).thenReturn(List.of());
        when(ruleRepo.findByYear(year)).thenReturn(List.of(rMinNan));

        VoyageDeclaration next = new VoyageDeclaration();
        next.setVesselId("v1");
        ReflectionTestUtils.setField(next, "id", "next");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.checkBeforeDeparture(next));
        assertTrue(ex.getMessage().contains("配额已耗尽"),
                "应抛出配额耗尽异常，实际: " + ex.getMessage());
    }

    /**
     * 边界：多海区时，仅发生透支的海区触发拦截，其他海区正常放行语义
     * 即：A 海区透支 -50，next voyage 来自 B 海区船舶（且 B 海区无超额），
     * 修复前可能因 A 海区跨海区累计误判放过；修复后仍能正确按 per-rule 触发。
     * 验证点：只要存在透支的 per-rule，就抛异常（无论跨不跨海区），不会被其他规则稀释掉。
     */
    @Test
    void anyRuleOverdraftShouldTriggerBlock() {
        String owner = "船东A";
        Vessel v1 = vessel("v1", owner, "泉州祥芝渔港", "闽南近海渔区");
        Vessel v2 = vessel("v2", owner, "福州连江黄岐渔港", "闽东近海渔区");

        QuotaRule rMinNan = rule("r1", "闽南近海渔区", "带鱼", "100");
        QuotaRule rMinDong = rule("r2", "闽东近海渔区", "带鱼", "500");

        VoyageDeclaration vy1 = voyage("vy1", year);
        VoyageDeclaration vy2 = voyage("vy2", year);

        CatchDeclaration c1 = catchDecl("c1", "v1", "vy1",
                "[{\"species\":\"带鱼\",\"actualWeight\":120}]", new BigDecimal("120"), "已完成");
        CatchDeclaration c2 = catchDecl("c2", "v2", "vy2",
                "[{\"species\":\"带鱼\",\"actualWeight\":50}]", new BigDecimal("50"), "已完成");

        when(vesselRepo.findAll()).thenReturn(List.of(v1, v2));
        when(catchRepo.findByVessel("v1")).thenReturn(List.of(c1));
        when(catchRepo.findByVessel("v2")).thenReturn(List.of(c2));
        when(voyageRepo.findById("vy1")).thenReturn(Optional.of(vy1));
        when(voyageRepo.findById("vy2")).thenReturn(Optional.of(vy2));
        when(violationRepo.findByVessel("v1")).thenReturn(List.of());
        when(violationRepo.findByVessel("v2")).thenReturn(List.of());
        when(ruleRepo.findByYear(year)).thenReturn(List.of(rMinNan, rMinDong));

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = service.summaryForOwner(owner, null, year);
        List<Map<String, Object>> rules = (List<Map<String, Object>>) summary.get("rules");
        Map<String, Object> minNan = ruleBySeaAreaAndSpecies(rules, "闽南近海渔区", "带鱼");
        Map<String, Object> minDong = ruleBySeaAreaAndSpecies(rules, "闽东近海渔区", "带鱼");

        // 修复后：闽南已用=120 剩余=-20；闽东已用=50 剩余=450
        assertEquals(new BigDecimal("120"), minNan.get("used"));
        assertEquals(new BigDecimal("-20"), minNan.get("remaining"));
        assertEquals(new BigDecimal("50"), minDong.get("used"));
        assertEquals(new BigDecimal("450"), minDong.get("remaining"));

        // checkBeforeDeparture 必须能识别闽南透支
        when(vesselRepo.findById(eq("v1"))).thenReturn(Optional.of(v1));
        VoyageDeclaration next = new VoyageDeclaration();
        next.setVesselId("v1");
        ReflectionTestUtils.setField(next, "id", "next-voyage");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.checkBeforeDeparture(next));
        assertTrue(ex.getMessage().contains("闽南近海渔区") || ex.getMessage().contains("配额已耗尽"),
                "应正确指出透支海区或配额耗尽，实际: " + ex.getMessage());
    }

    /**
     * 状态过滤：仅「已完成」申报单计入累计
     */
    @Test
    void nonCompletedStatusShouldNotCount() {
        String owner = "船东A";
        Vessel v1 = vessel("v1", owner, "泉州祥芝渔港", "闽南近海渔区");
        QuotaRule r = rule("r1", "闽南近海渔区", "带鱼", "100");
        VoyageDeclaration vy = voyage("vy1", year);

        CatchDeclaration finished = catchDecl("c1", "v1", "vy1",
                "[{\"species\":\"带鱼\",\"actualWeight\":50}]", new BigDecimal("50"), "已完成");
        CatchDeclaration review = catchDecl("c2", "v1", "vy1",
                "[{\"species\":\"带鱼\",\"actualWeight\":30}]", new BigDecimal("30"), "偏差复核中");
        CatchDeclaration noWeigh = catchDecl("c3", "v1", "vy1",
                "[{\"species\":\"带鱼\",\"actualWeight\":30}]", null, "预申报已提交");

        when(vesselRepo.findAll()).thenReturn(List.of(v1));
        when(catchRepo.findByVessel("v1")).thenReturn(List.of(finished, review, noWeigh));
        when(voyageRepo.findById("vy1")).thenReturn(Optional.of(vy));
        when(violationRepo.findByVessel("v1")).thenReturn(List.of());
        when(ruleRepo.findByYear(year)).thenReturn(List.of(r));

        Map<String, Object> summary = service.summaryForOwner(owner, null, year);
        assertEquals(new BigDecimal("50"), summary.get("totalUsed"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rules = (List<Map<String, Object>>) summary.get("rules");
        assertEquals(new BigDecimal("50"),
                ruleBySeaAreaAndSpecies(rules, "闽南近海渔区", "带鱼").get("used"));
    }

    /**
     * 违规扣减：扣减总额不参与分品种 remaining 累计
     */
    @Test
    void totalDeductIsKeptSeparateFromPerRuleRemaining() {
        String owner = "船东A";
        Vessel v1 = vessel("v1", owner, "泉州祥芝渔港", "闽南近海渔区");
        QuotaRule r = rule("r1", "闽南近海渔区", "带鱼", "100");
        VoyageDeclaration vy = voyage("vy1", year);

        CatchDeclaration finished = catchDecl("c1", "v1", "vy1",
                "[{\"species\":\"带鱼\",\"actualWeight\":40}]", new BigDecimal("40"), "已完成");

        ViolationNotice notice = new ViolationNotice();
        notice.setQuotaDeducted(new BigDecimal("10"));

        when(vesselRepo.findAll()).thenReturn(List.of(v1));
        when(catchRepo.findByVessel("v1")).thenReturn(List.of(finished));
        when(voyageRepo.findById("vy1")).thenReturn(Optional.of(vy));
        when(violationRepo.findByVessel("v1")).thenReturn(List.of(notice));
        when(ruleRepo.findByYear(year)).thenReturn(List.of(r));

        Map<String, Object> summary = service.summaryForOwner(owner, null, year);

        assertEquals(new BigDecimal("10"), summary.get("totalDeduct"));
        // per-rule remaining 仍按 quota - used 计算，违规扣减不参与，避免虚高
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rules = (List<Map<String, Object>>) summary.get("rules");
        Map<String, Object> rule = ruleBySeaAreaAndSpecies(rules, "闽南近海渔区", "带鱼");
        assertEquals(new BigDecimal("60"), rule.get("remaining"));
        // 总额 = per-rule 之和，与分品种明细对齐
        assertEquals(new BigDecimal("60"), summary.get("totalRemaining"));
    }
}
