package com.fishing.platform.service;

import com.fishing.platform.entity.Vessel;
import com.fishing.platform.repository.VesselRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 船舶档案服务
 */
@Service
public class VesselService {

    @Autowired private VesselRepository repo;

    public Vessel save(Vessel v) {
        if (v.getId() == null) v.setId(UUID.randomUUID().toString().replace("-", ""));
        v.setUpdatedAt(LocalDateTime.now());
        if (v.getCreatedAt() == null) v.setCreatedAt(LocalDateTime.now());
        if (v.getStatus() == null) v.setStatus("在港");
        return repo.save(v);
    }

    public List<Vessel> findAll() {
        return repo.findAll();
    }

    public Optional<Vessel> findById(String id) {
        return repo.findById(id);
    }

    public Optional<Vessel> findByVesselNo(String vesselNo) {
        return repo.findByVesselNo(vesselNo);
    }

    public List<Vessel> findByPort(String portName) {
        return repo.findByPortName(portName);
    }
}
