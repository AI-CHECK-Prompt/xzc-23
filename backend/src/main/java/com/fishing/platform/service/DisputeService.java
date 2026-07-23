package com.fishing.platform.service;

import com.fishing.platform.common.BusinessException;
import com.fishing.platform.entity.Dispute;
import com.fishing.platform.entity.InspectionReport;
import com.fishing.platform.entity.TradeConfirmation;
import com.fishing.platform.repository.DisputeRepository;
import com.fishing.platform.repository.InspectionReportRepository;
import com.fishing.platform.repository.TradeConfirmationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 争议处理服务
 * <p>
 * 状态机：PENDING(待处理) → INSPECTING(检测中) → ARBITRATING(仲裁中) → CLOSED(已结案) / REJECTED(已驳回)
 * 1) open: 交易一方基于确认单发起争议
 * 2) assignAgency: 平台协调第三方检测机构
 * 3) submitReport: 检测机构出具检测报告
 * 4) arbitrate: 平台依据报告做出仲裁
 * 5) close: 结案
 */
@Service
public class DisputeService {

    private static final Logger log = LoggerFactory.getLogger(DisputeService.class);

    @Autowired private DisputeRepository disputeRepo;
    @Autowired private InspectionReportRepository reportRepo;
    @Autowired private TradeConfirmationRepository confRepo;

    // 序列号生成（按日期）
    private final AtomicInteger daySeq = new AtomicInteger(0);
    private volatile LocalDate seqDate = LocalDate.now();

    private synchronized String nextDisputeNo() {
        LocalDate today = LocalDate.now();
        if (!today.equals(seqDate)) {
            seqDate = today;
            daySeq.set(0);
        }
        int n = daySeq.incrementAndGet();
        return String.format("DJ-%s-%04d", today.toString().replace("-", ""), n);
    }

    private synchronized String nextReportNo(String agencyShort) {
        LocalDate today = LocalDate.now();
        return String.format("RP-%s-%s-%04d",
                agencyShort == null ? "AGY" : agencyShort,
                today.toString().replace("-", ""),
                (int) (System.currentTimeMillis() % 10000));
    }

    // ============ 1) 发起争议 ============

    @Transactional
    public Dispute open(Dispute d) {
        if (d.getConfirmationId() == null) throw new BusinessException("缺少确认单ID");
        if (d.getInitiator() == null) throw new BusinessException("缺少发起方");
        if (d.getDisputeType() == null) throw new BusinessException("缺少争议类型");
        TradeConfirmation conf = confRepo.findById(d.getConfirmationId())
                .orElseThrow(() -> new BusinessException("确认单不存在"));
        if (!"SIGNED".equals(conf.getStatus()) && !"ARRIVED".equals(conf.getStatus())) {
            throw new BusinessException("仅已签署或已到达的确认单可发起争议，当前=" + conf.getStatus());
        }
        // 同一确认单只允许存在 1 个未结案争议
        List<Dispute> openOnes = disputeRepo.findByConfirmationId(conf.getId());
        for (Dispute ex : openOnes) {
            if (!"CLOSED".equals(ex.getStatus()) && !"REJECTED".equals(ex.getStatus())) {
                throw new BusinessException("该确认单存在未结案争议：" + ex.getDisputeNo());
            }
        }
        d.setId(UUID.randomUUID().toString().replace("-", ""));
        d.setDisputeNo(nextDisputeNo());
        d.setConfirmationNo(conf.getConfirmationNo());
        if (d.getStatus() == null) d.setStatus("PENDING");
        d.setCreatedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        log.info("【争议-发起】{} 类型={} 发起方={} 确认单={}",
                d.getDisputeNo(), d.getDisputeType(), d.getInitiator(), conf.getConfirmationNo());
        return disputeRepo.save(d);
    }

    // ============ 2) 协调检测机构 ============

    @Transactional
    public Dispute assignAgency(String disputeId, String agencyName) {
        Dispute d = mustDispute(disputeId);
        if (!"PENDING".equals(d.getStatus())) {
            throw new BusinessException("仅 PENDING 状态可分派机构，当前=" + d.getStatus());
        }
        if (agencyName == null || agencyName.isEmpty()) {
            throw new BusinessException("缺少检测机构名称");
        }
        d.setAssignedAgency(agencyName);
        d.setStatus("INSPECTING");
        d.setUpdatedAt(LocalDateTime.now());
        log.info("【争议-分派】{} → 检测机构 {}", d.getDisputeNo(), agencyName);
        return disputeRepo.save(d);
    }

    // ============ 3) 检测机构出具报告 ============

    @Transactional
    public InspectionReport submitReport(InspectionReport r) {
        if (r.getDisputeId() == null) throw new BusinessException("缺少争议ID");
        if (r.getAgencyName() == null) throw new BusinessException("缺少检测机构");
        Dispute d = mustDispute(r.getDisputeId());
        if (!"INSPECTING".equals(d.getStatus())) {
            throw new BusinessException("仅 INSPECTING 状态可提交报告，当前=" + d.getStatus());
        }
        r.setId(UUID.randomUUID().toString().replace("-", ""));
        // 机构短码：从名称取首 3 个中文字符
        String shortCode = "AGY";
        if (r.getAgencyName().length() >= 3) {
            shortCode = r.getAgencyName().substring(0, 3);
        }
        r.setReportNo(nextReportNo(shortCode));
        r.setDisputeId(d.getId());
        if (r.getIssuedAt() == null) r.setIssuedAt(LocalDateTime.now());
        r.setCreatedAt(LocalDateTime.now());
        reportRepo.save(r);
        // 报告提交后进入仲裁阶段
        d.setStatus("ARBITRATING");
        d.setUpdatedAt(LocalDateTime.now());
        disputeRepo.save(d);
        log.info("【争议-报告】{} 检测机构={} 报告号={}",
                d.getDisputeNo(), r.getAgencyName(), r.getReportNo());
        return r;
    }

    // ============ 4) 仲裁结案 ============

    @Transactional
    public Dispute close(String disputeId, String result, String closedBy) {
        Dispute d = mustDispute(disputeId);
        if ("CLOSED".equals(d.getStatus()) || "REJECTED".equals(d.getStatus())) {
            throw new BusinessException("争议已结案，不可重复操作");
        }
        if (result == null || result.isEmpty()) {
            throw new BusinessException("缺少仲裁结果");
        }
        d.setArbitrateResult(result);
        d.setClosedBy(closedBy == null ? "平台仲裁" : closedBy);
        d.setStatus("CLOSED");
        d.setClosedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        log.info("【争议-结案】{} 结果={} 经办={}", d.getDisputeNo(), result, d.getClosedBy());
        return disputeRepo.save(d);
    }

    /** 驳回争议：用于证据不足或不属于平台受理范围 */
    @Transactional
    public Dispute reject(String disputeId, String reason, String closedBy) {
        Dispute d = mustDispute(disputeId);
        if ("CLOSED".equals(d.getStatus()) || "REJECTED".equals(d.getStatus())) {
            throw new BusinessException("争议已结案，不可重复操作");
        }
        d.setArbitrateResult(reason == null ? "驳回" : reason);
        d.setClosedBy(closedBy == null ? "平台仲裁" : closedBy);
        d.setStatus("REJECTED");
        d.setClosedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        log.info("【争议-驳回】{} 原因={}", d.getDisputeNo(), reason);
        return disputeRepo.save(d);
    }

    // ============ 查询 ============

    public Dispute get(String id) {
        return disputeRepo.findById(id).orElse(null);
    }

    public Dispute getByNo(String disputeNo) {
        return disputeRepo.findByDisputeNo(disputeNo).orElse(null);
    }

    public List<Dispute> list(String status) {
        return disputeRepo.search(status);
    }

    public List<Dispute> listByConfirmation(String confirmationId) {
        return disputeRepo.findByConfirmationId(confirmationId);
    }

    public List<InspectionReport> reportsOf(String disputeId) {
        return reportRepo.findByDisputeId(disputeId);
    }

    private Dispute mustDispute(String id) {
        return disputeRepo.findById(id)
                .orElseThrow(() -> new BusinessException("争议不存在: " + id));
    }
}
