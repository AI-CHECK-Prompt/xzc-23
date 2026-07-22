package com.fishing.platform.service;

import com.fishing.platform.entity.PurchaseRecord;
import com.fishing.platform.repository.PurchaseRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 采购回传
 */
@Service
public class PurchaseService {

    @Autowired private PurchaseRecordRepository repo;

    public PurchaseRecord report(PurchaseRecord r) {
        if (r.getAmount() == null && r.getWeight() != null && r.getPrice() != null) {
            r.setAmount(r.getWeight().multiply(r.getPrice()));
        }
        if (r.getPurchaseTime() == null) {
            r.setPurchaseTime(LocalDateTime.now());
        }
        r.setCreatedAt(LocalDateTime.now());
        if (r.getId() == null) {
            r.setId(UUID.randomUUID().toString().replace("-", ""));
        }
        return repo.save(r);
    }

    public List<PurchaseRecord> findByVessel(String vesselId) {
        return repo.findByVessel(vesselId);
    }

    public List<PurchaseRecord> findByVoyage(String voyageId) {
        return repo.findByVoyage(voyageId);
    }
}
