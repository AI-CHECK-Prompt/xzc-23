package com.fishing.platform.service;

import com.fishing.platform.common.BusinessException;
import com.fishing.platform.entity.CatchDeclaration;
import com.fishing.platform.entity.VoyageDeclaration;
import com.fishing.platform.repository.CatchDeclarationRepository;
import com.fishing.platform.repository.VoyageDeclarationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 渔获预申报与过磅核对
 */
@Service
public class CatchService {

    @Autowired private CatchDeclarationRepository catchRepo;
    @Autowired private VoyageDeclarationRepository voyageRepo;

    /** 偏差告警阈值 10% */
    private static final BigDecimal DEVIATION_THRESHOLD = new BigDecimal("0.10");

    @Transactional
    public CatchDeclaration submitPre(CatchDeclaration d) {
        VoyageDeclaration voyage = voyageRepo.findById(d.getVoyageId())
                .orElseThrow(() -> new BusinessException("关联航次不存在"));
        d.setVesselId(voyage.getVesselId());
        d.setVesselNo(voyage.getVesselNo());
        d.setOwnerName(voyage.getOwnerName());
        d.setPortName(voyage.getPortName());
        d.setStatus("预申报已提交");
        d.setCreatedAt(LocalDateTime.now());
        if (d.getId() == null) {
            d.setId(UUID.randomUUID().toString().replace("-", ""));
        }
        if (d.getDeclarationNo() == null || d.getDeclarationNo().isEmpty()) {
            String dateStr = LocalDate.now().toString().replace("-", "");
            d.setDeclarationNo("YC-" + voyage.getVesselNo() + "-" + dateStr
                    + "-" + System.currentTimeMillis() % 10000);
        }
        return catchRepo.save(d);
    }

    @Transactional
    public CatchDeclaration confirmWeigh(String id, BigDecimal actualTotal, String reason, String operator) {
        CatchDeclaration d = catchRepo.findById(id)
                .orElseThrow(() -> new BusinessException("渔获申报单不存在"));
        d.setActualTotal(actualTotal);
        d.setConfirmedBy(operator);
        d.setConfirmedAt(LocalDateTime.now());

        if (d.getEstimatedTotal() != null && d.getEstimatedTotal().signum() > 0) {
            BigDecimal diff = actualTotal.subtract(d.getEstimatedTotal()).abs();
            BigDecimal ratio = diff.divide(d.getEstimatedTotal(), 4, RoundingMode.HALF_UP);
            d.setDeviationRatio(ratio.multiply(new BigDecimal("100")));
            if (ratio.compareTo(DEVIATION_THRESHOLD) > 0) {
                d.setStatus("偏差复核中");
                d.setDeviationReason(reason);
            } else {
                d.setStatus("已完成");
            }
        } else {
            d.setStatus("已完成");
        }
        return catchRepo.save(d);
    }

    /**
     * 偏差复核：管理员填写复核结论，将状态推进至「已完成」。
     * 仅当当前状态为「偏差复核中」时才允许提交。
     */
    @Transactional
    public CatchDeclaration reviewDeviation(String id, String reviewReason, String reviewer) {
        CatchDeclaration d = catchRepo.findById(id)
                .orElseThrow(() -> new BusinessException("渔获申报单不存在"));
        if (!"偏差复核中".equals(d.getStatus())) {
            throw new BusinessException("当前状态非「偏差复核中」，无需复核");
        }
        if (reviewReason == null || reviewReason.trim().isEmpty()) {
            throw new BusinessException("请填写复核结论");
        }
        // 复核结论追加到偏差原因，保留原异常原因便于追溯
        String origin = d.getDeviationReason() == null ? "" : d.getDeviationReason();
        String stamp = LocalDateTime.now() + " 复核人=" + (reviewer == null ? "" : reviewer)
                + " 结论=" + reviewReason;
        d.setDeviationReason(origin.isEmpty() ? stamp : origin + "\n---\n" + stamp);
        d.setStatus("已完成");
        return catchRepo.save(d);
    }

    public List<CatchDeclaration> findByVoyage(String voyageId) {
        return catchRepo.findByVoyage(voyageId);
    }

    public List<CatchDeclaration> findByVessel(String vesselId) {
        return catchRepo.findByVessel(vesselId);
    }
}
