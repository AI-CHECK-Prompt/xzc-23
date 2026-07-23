package com.fishing.platform.service;

import com.fishing.platform.common.BusinessException;
import com.fishing.platform.entity.Vessel;
import com.fishing.platform.entity.ViolationNotice;
import com.fishing.platform.repository.VesselRepository;
import com.fishing.platform.repository.ViolationNoticeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 执法办案服务
 */
@Service
public class EnforcementService {

    @Autowired private ViolationNoticeRepository noticeRepo;
    @Autowired private VesselRepository vesselRepo;

    @Transactional
    public ViolationNotice issue(ViolationNotice notice) {
        if (notice.getVesselId() != null) {
            Vessel v = vesselRepo.findById(notice.getVesselId()).orElse(null);
            if (v != null) {
                notice.setVesselNo(v.getVesselNo());
            }
        }
        if (notice.getId() == null) {
            notice.setId(UUID.randomUUID().toString().replace("-", ""));
        }
        if (notice.getNoticeNo() == null || notice.getNoticeNo().isEmpty()) {
            String dateStr = LocalDate.now().toString().replace("-", "");
            // 当事人船舶未登记或临时挂靠未纳入平台时，vesselNo 为空，使用 UNREG 占位，避免出现 WZ--xxxx 双横线
            String vesselPart = (notice.getVesselNo() == null || notice.getVesselNo().isEmpty())
                    ? "UNREG" : notice.getVesselNo();
            notice.setNoticeNo("WZ-" + vesselPart + "-" + dateStr + "-" + System.currentTimeMillis() % 10000);
        }
        notice.setIssueTime(LocalDateTime.now());
        notice.setCreatedAt(LocalDateTime.now());
        if (notice.getStatus() == null) notice.setStatus("已开具");
        return noticeRepo.save(notice);
    }

    /**
     * 配额扣减：违规后调用
     * 仅对待生效状态的告知书允许扣减；已撤销、已结案、已扣减的告知书拒绝再次扣减
     */
    @Transactional
    public ViolationNotice applyQuotaDeduct(String noticeId, BigDecimal deducted) {
        ViolationNotice n = noticeRepo.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("违规告知书不存在"));
        String status = n.getStatus();
        // 已生效 = 已完成扣减；已撤销 / 已结案 不可再扣减
        Set<String> blocked = Set.of("已生效", "已撤销", "已结案");
        if (status != null && blocked.contains(status)) {
            throw new BusinessException("告知书当前状态为「" + status + "」，不允许再次扣减配额");
        }
        n.setQuotaDeducted(deducted);
        n.setStatus("已生效");
        return noticeRepo.save(n);
    }

    public List<ViolationNotice> findByVessel(String vesselId) {
        return noticeRepo.findByVessel(vesselId);
    }

    public List<ViolationNotice> findAll() {
        return noticeRepo.findAll();
    }
}
