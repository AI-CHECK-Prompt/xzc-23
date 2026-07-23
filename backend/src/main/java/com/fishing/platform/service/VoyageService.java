package com.fishing.platform.service;

import com.fishing.platform.common.BusinessException;
import com.fishing.platform.entity.*;
import com.fishing.platform.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * 出海申报服务
 */
@Service
public class VoyageService {

    @Autowired private VesselRepository vesselRepo;
    @Autowired private VoyageDeclarationRepository voyageRepo;
    @Autowired private AlertEventRepository alertRepo;

    private static final int DEFAULT_MAX_DAYS = 30;

    @Transactional
    public VoyageDeclaration submit(VoyageDeclaration declaration) {
        Vessel vessel = vesselRepo.findById(declaration.getVesselId())
                .orElseThrow(() -> new BusinessException("船舶不存在"));

        // 校验一：证件有效期
        LocalDate today = LocalDate.now();
        if (vessel.getCertValidTo() != null && today.isAfter(vessel.getCertValidTo())) {
            throw new BusinessException("船舶证件已过期，无法申报");
        }
        if (vessel.getCertValidFrom() != null && today.isBefore(vessel.getCertValidFrom())) {
            throw new BusinessException("船舶证件尚未生效，无法申报");
        }

        // 校验二：违规停业整改期
        if (vessel.isSuspended() && vessel.getSuspendUntil() != null
                && !today.isAfter(vessel.getSuspendUntil())) {
            throw new BusinessException("船舶处于违规停业整改期，截止日期：" + vessel.getSuspendUntil());
        }

        // 校验三：超出前次申报未归港的最长允许出海天数
        int maxDays = declaration.getMaxAllowedDays() == null ? DEFAULT_MAX_DAYS : declaration.getMaxAllowedDays();
        List<VoyageDeclaration> actives = voyageRepo.findActiveByVessel(vessel.getId());
        for (VoyageDeclaration old : actives) {
            if (old.getPlanDepartureTime() != null) {
                long elapsed = ChronoUnit.DAYS.between(old.getPlanDepartureTime().toLocalDate(), today);
                if (elapsed > maxDays) {
                    throw new BusinessException("前次申报航次（" + old.getDeclarationNo()
                            + "）已超过最长允许出海天数 " + maxDays + " 天，请先办理归港");
                }
            }
        }

        declaration.setVesselNo(vessel.getVesselNo());
        declaration.setVesselName(vessel.getVesselName());
        declaration.setOwnerName(vessel.getOwnerName());
        declaration.setPortName(vessel.getPortName());
        declaration.setSeaAreaName(vessel.getSeaAreaName());
        declaration.setMaxAllowedDays(maxDays);
        declaration.setStatus("已申报");
        declaration.setDeclarationYear(LocalDateTime.now().getYear());
        if (declaration.getId() == null) {
            declaration.setId(UUID.randomUUID().toString().replace("-", ""));
        }
        if (declaration.getDeclarationNo() == null || declaration.getDeclarationNo().isEmpty()) {
            String dateStr = LocalDate.now().toString().replace("-", "");
            declaration.setDeclarationNo(vessel.getPortName() + "-" + vessel.getVesselNo()
                    + "-" + dateStr + "-" + System.currentTimeMillis() % 10000);
        }
        declaration.setCreatedAt(LocalDateTime.now());
        declaration.setUpdatedAt(LocalDateTime.now());
        return voyageRepo.save(declaration);
    }

    @Transactional
    public VoyageDeclaration depart(String id) {
        VoyageDeclaration d = voyageRepo.findById(id)
                .orElseThrow(() -> new BusinessException("申报单不存在"));
        d.setStatus("已出港");
        d.setActualDepartureTime(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        return voyageRepo.save(d);
    }

    @Transactional
    public VoyageDeclaration returnToPort(String id) {
        VoyageDeclaration d = voyageRepo.findById(id)
                .orElseThrow(() -> new BusinessException("申报单不存在"));
        d.setStatus("已归港");
        d.setActualReturnTime(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        return voyageRepo.save(d);
    }

    public List<VoyageDeclaration> findByVessel(String vesselId) {
        return voyageRepo.findByVesselIdOrderByCreatedAtDesc(vesselId);
    }

    public List<VoyageDeclaration> findAll() {
        return voyageRepo.findAll();
    }

    public List<VoyageDeclaration> findByPortAndYear(String portName, Integer year) {
        return voyageRepo.findByPortAndYear(portName, year);
    }

    public VoyageDeclaration findById(String id) {
        return voyageRepo.findById(id).orElse(null);
    }
}
