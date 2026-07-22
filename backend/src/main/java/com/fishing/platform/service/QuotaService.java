package com.fishing.platform.service;

import com.fishing.platform.common.BusinessException;
import com.fishing.platform.entity.*;
import com.fishing.platform.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 配额管理服务
 */
@Service
public class QuotaService {

    @Autowired private QuotaRuleRepository ruleRepo;
    @Autowired private CatchDeclarationRepository catchRepo;
    @Autowired private ViolationNoticeRepository violationRepo;
    @Autowired private VoyageDeclarationRepository voyageRepo;
    @Autowired private VesselRepository vesselRepo;

    private final ObjectMapper mapper = new ObjectMapper();

    public QuotaRule saveRule(QuotaRule rule) {
        if (rule.getId() == null) {
            rule.setId(UUID.randomUUID().toString().replace("-", ""));
        }
        rule.setCreatedAt(LocalDateTime.now());
        return ruleRepo.save(rule);
    }

    public List<QuotaRule> listRules(Integer year) {
        if (year == null) year = LocalDate.now().getYear();
        return ruleRepo.findByYear(year);
    }

    /**
     * 计算某船东年度累计渔获与剩余配额
     */
    @Transactional(readOnly = true)
    public Map<String, Object> summaryForOwner(String owner, String portName, Integer year) {
        if (year == null) year = LocalDate.now().getYear();
        List<Vessel> vessels = vesselRepo.findAll();
        Map<String, BigDecimal> usedBySpecies = new HashMap<>();
        BigDecimal totalUsed = BigDecimal.ZERO;
        BigDecimal totalDeduct = BigDecimal.ZERO;

        for (Vessel v : vessels) {
            if (!Objects.equals(v.getOwnerName(), owner)) continue;
            if (portName != null && !Objects.equals(v.getPortName(), portName)) continue;
            for (CatchDeclaration c : catchRepo.findByVessel(v.getId())) {
                if (c.getActualTotal() == null) continue;
                if (v.getSeaAreaName() == null) continue;
                VoyageDeclaration voyage = voyageRepo.findById(c.getVoyageId()).orElse(null);
                if (voyage == null || voyage.getYear() == null || voyage.getYear() != year) continue;
                if (c.getItemsJson() != null) {
                    try {
                        JsonNode arr = mapper.readTree(c.getItemsJson());
                        for (JsonNode item : arr) {
                            String species = item.path("species").asText();
                            BigDecimal w = item.path("actualWeight").decimalValue();
                            usedBySpecies.merge(species, w, BigDecimal::add);
                            totalUsed = totalUsed.add(w);
                        }
                    } catch (Exception ignored) {}
                } else {
                    totalUsed = totalUsed.add(c.getActualTotal());
                }
            }
            for (ViolationNotice n : violationRepo.findByVessel(v.getId())) {
                if (n.getQuotaDeducted() != null) {
                    totalDeduct = totalDeduct.add(n.getQuotaDeducted());
                }
            }
        }

        List<QuotaRule> rules = ruleRepo.findByYear(year);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("owner", owner);
        result.put("portName", portName);
        result.put("year", year);
        result.put("totalUsed", totalUsed);
        result.put("totalDeduct", totalDeduct);
        List<Map<String, Object>> ruleList = new ArrayList<>();
        BigDecimal totalQuota = BigDecimal.ZERO;
        for (QuotaRule r : rules) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("species", r.getSpecies());
            m.put("seaArea", r.getSeaAreaName());
            m.put("totalQuota", r.getTotalQuota());
            m.put("used", usedBySpecies.getOrDefault(r.getSpecies(), BigDecimal.ZERO));
            m.put("remaining", r.getTotalQuota().subtract(usedBySpecies.getOrDefault(r.getSpecies(), BigDecimal.ZERO)));
            m.put("minSize", r.getMinSize());
            m.put("banned", r.isBanned());
            ruleList.add(m);
            totalQuota = totalQuota.add(r.getTotalQuota());
        }
        result.put("rules", ruleList);
        result.put("totalQuota", totalQuota);
        result.put("totalRemaining", totalQuota.subtract(totalUsed).subtract(totalDeduct));
        return result;
    }

    /**
     * 海区按年度聚合
     */
    public Map<String, Object> summaryBySeaArea(String seaArea, Integer year) {
        if (year == null) year = LocalDate.now().getYear();
        List<VoyageDeclaration> voyages = voyageRepo.findBySeaAreaAndYear(seaArea, year);
        Map<String, BigDecimal> usedBySpecies = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (VoyageDeclaration voyage : voyages) {
            for (CatchDeclaration c : catchRepo.findByVoyage(voyage.getId())) {
                if (c.getActualTotal() == null) continue;
                total = total.add(c.getActualTotal());
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("seaArea", seaArea);
        result.put("year", year);
        result.put("totalCatch", total);
        result.put("voyageCount", voyages.size());
        return result;
    }

    public Map<String, Object> summaryByPort(String portName, Integer year) {
        if (year == null) year = LocalDate.now().getYear();
        List<VoyageDeclaration> voyages = voyageRepo.findByPortAndYear(portName, year);
        BigDecimal total = BigDecimal.ZERO;
        for (VoyageDeclaration voyage : voyages) {
            for (CatchDeclaration c : catchRepo.findByVoyage(voyage.getId())) {
                if (c.getActualTotal() == null) continue;
                total = total.add(c.getActualTotal());
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("portName", portName);
        result.put("year", year);
        result.put("totalCatch", total);
        result.put("voyageCount", voyages.size());
        return result;
    }

    /**
     * 校验下一航次是否被配额限制
     */
    public void checkBeforeDeparture(VoyageDeclaration declaration) {
        Vessel vessel = vesselRepo.findById(declaration.getVesselId())
                .orElseThrow(() -> new BusinessException("船舶不存在"));
        int year = LocalDate.now().getYear();
        Map<String, Object> summary = summaryForOwner(vessel.getOwnerName(), vessel.getPortName(), year);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rules = (List<Map<String, Object>>) summary.get("rules");
        for (Map<String, Object> r : rules) {
            BigDecimal remaining = (BigDecimal) r.get("remaining");
            boolean banned = Boolean.TRUE.equals(r.get("banned"));
            if (banned) {
                throw new BusinessException("品种 " + r.get("species") + " 处于禁渔期，禁止出海");
            }
            if (remaining != null && remaining.signum() <= 0) {
                throw new BusinessException("船东 " + vessel.getOwnerName()
                        + " 品种 " + r.get("species") + " 配额已耗尽，禁止下一航次出海");
            }
        }
    }
}
