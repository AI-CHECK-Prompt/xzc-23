package com.fishing.platform.service;

import com.fishing.platform.common.BusinessException;
import com.fishing.platform.entity.*;
import com.fishing.platform.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 冷链配送协同服务
 * <p>
 * - 车辆登记
 * - 运单创建/起运/到达
 * - 温度上报：超出 [minTemp, maxTemp] 时自动生成告警（复用 AlertService）
 * - 温度阈值按品种区分
 */
@Service
public class ColdChainService {

    private static final Logger log = LoggerFactory.getLogger(ColdChainService.class);

    @Autowired private ColdChainVehicleRepository vehicleRepo;
    @Autowired private ColdChainShipmentRepository shipmentRepo;
    @Autowired private ColdChainTempReadingRepository readingRepo;
    @Autowired private TradeConfirmationRepository confRepo;
    @Autowired private AlertService alertService;

    /** 默认品种温度阈值（℃） */
    private static final Map<String, double[]> SPECIES_TEMP = new HashMap<>();
    static {
        SPECIES_TEMP.put("带鱼", new double[]{-1, 4});
        SPECIES_TEMP.put("黄花鱼", new double[]{0, 4});
        SPECIES_TEMP.put("鲳鱼", new double[]{0, 4});
        SPECIES_TEMP.put("墨鱼", new double[]{0, 4});
        SPECIES_TEMP.put("梭子蟹", new double[]{0, 2});
        SPECIES_TEMP.put("虾蛄", new double[]{0, 4});
    }

    // ============ 车辆 ============

    public ColdChainVehicle registerVehicle(ColdChainVehicle v) {
        if (v.getVehicleNo() == null) throw new BusinessException("缺少车号");
        // 幂等：车号已存在则返回已有车辆（避免 E2E 脚本因残留数据失败）
        var existing = vehicleRepo.findByVehicleNo(v.getVehicleNo());
        if (existing.isPresent()) {
            log.info("【冷链-车辆】车号 {} 已存在，返回已有车辆 ID={}",
                    v.getVehicleNo(), existing.get().getId());
            return existing.get();
        }
        v.setId(UUID.randomUUID().toString().replace("-", ""));
        if (v.getStatus() == null) v.setStatus("IDLE");
        v.setCreatedAt(LocalDateTime.now());
        log.info("【冷链-车辆】注册 {} 司机={}", v.getVehicleNo(), v.getDriverName());
        return vehicleRepo.save(v);
    }

    public List<ColdChainVehicle> listVehicles() {
        return vehicleRepo.findAll();
    }

    // ============ 运单 ============

    @Transactional
    public ColdChainShipment createShipment(String confirmationId, String vehicleId,
                                            LocalDateTime plannedDeparture,
                                            LocalDateTime plannedArrival) {
        TradeConfirmation conf = confRepo.findById(confirmationId)
                .orElseThrow(() -> new BusinessException("确认单不存在"));
        if (!"SIGNED".equals(conf.getStatus())) {
            throw new BusinessException("仅已签署的确认单可创建冷链运单，当前状态=" + conf.getStatus());
        }
        ColdChainVehicle vehicle = vehicleRepo.findById(vehicleId)
                .orElseThrow(() -> new BusinessException("车辆不存在"));
        if (shipmentRepo.findByConfirmationId(confirmationId).isPresent()) {
            throw new BusinessException("该确认单已存在运单");
        }
        ColdChainShipment s = new ColdChainShipment();
        s.setId(UUID.randomUUID().toString().replace("-", ""));
        s.setConfirmationId(confirmationId);
        s.setConfirmationNo(conf.getConfirmationNo());
        s.setVehicleId(vehicle.getId());
        s.setVehicleNo(vehicle.getVehicleNo());
        s.setDriverName(vehicle.getDriverName());
        s.setOriginPort(conf.getPortName());
        s.setDestination(conf.getDestination());
        s.setSpecies(conf.getSpecies());
        s.setWeight(conf.getWeight());
        // 按品种设置温度阈值
        double[] th = SPECIES_TEMP.getOrDefault(conf.getSpecies(), new double[]{0, 4});
        s.setMinTemp(BigDecimal.valueOf(th[0]));
        s.setMaxTemp(BigDecimal.valueOf(th[1]));
        s.setPlannedDepartureTime(plannedDeparture);
        s.setPlannedArrivalTime(plannedArrival);
        s.setStatus("CREATED");
        s.setAnomalyCount(0);
        s.setCreatedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        vehicle.setStatus("LOADING");
        vehicleRepo.save(vehicle);
        log.info("【冷链-运单】{} 分配车辆 {} 温度阈值 [{},{}]°C",
                conf.getConfirmationNo(), vehicle.getVehicleNo(), th[0], th[1]);
        return shipmentRepo.save(s);
    }

    public ColdChainShipment depart(String id) {
        ColdChainShipment s = mustShipment(id);
        if (!"CREATED".equals(s.getStatus())) {
            throw new BusinessException("当前状态 " + s.getStatus() + " 不可起运");
        }
        s.setStatus("IN_TRANSIT");
        s.setActualDepartureTime(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        ColdChainVehicle v = vehicleRepo.findById(s.getVehicleId()).orElse(null);
        if (v != null) {
            v.setStatus("IN_TRANSIT");
            vehicleRepo.save(v);
        }
        log.info("【冷链-运单】{} 起运", s.getConfirmationNo());
        return shipmentRepo.save(s);
    }

    public ColdChainShipment arrive(String id) {
        ColdChainShipment s = mustShipment(id);
        if (!"IN_TRANSIT".equals(s.getStatus())) {
            throw new BusinessException("当前状态 " + s.getStatus() + " 不可标记到达");
        }
        s.setStatus("ARRIVED");
        s.setActualArrivalTime(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        ColdChainVehicle v = vehicleRepo.findById(s.getVehicleId()).orElse(null);
        if (v != null) {
            v.setStatus("IDLE");
            vehicleRepo.save(v);
        }
        log.info("【冷链-运单】{} 已到达", s.getConfirmationNo());
        return shipmentRepo.save(s);
    }

    public ColdChainShipment getShipment(String id) {
        return shipmentRepo.findById(id).orElse(null);
    }

    public List<ColdChainShipment> listShipments(String status) {
        return shipmentRepo.search(status);
    }

    public List<ColdChainTempReading> getReadings(String shipmentId) {
        return readingRepo.findByShipment(shipmentId);
    }

    // ============ 温度上报 + 异常告警 ============

    @Transactional
    public ColdChainTempReading reportTemperature(String shipmentId, BigDecimal temperature, String source) {
        ColdChainShipment s = mustShipment(shipmentId);
        ColdChainTempReading r = new ColdChainTempReading();
        r.setId(UUID.randomUUID().toString().replace("-", ""));
        r.setShipmentId(shipmentId);
        r.setTemperature(temperature);
        r.setSource(source);
        r.setRecordedAt(LocalDateTime.now());
        boolean anomaly = isAnomaly(s, temperature);
        r.setAnomaly(anomaly);
        readingRepo.save(r);
        if (anomaly) {
            // 累加异常次数
            s.setAnomalyCount((s.getAnomalyCount() == null ? 0 : s.getAnomalyCount()) + 1);
            s.setUpdatedAt(LocalDateTime.now());
            shipmentRepo.save(s);
            // 复用 AlertService：使用 vesselNo="冷链-车号" 区分告警源
            String desc = String.format("冷链运单 %s 温度异常：当前 %.1f°C（阈值 [%s, %s]°C）",
                    s.getConfirmationNo(),
                    temperature.doubleValue(),
                    s.getMinTemp().toPlainString(),
                    s.getMaxTemp().toPlainString());
            alertService.createIfAbsent(
                    "COLDCHAIN:" + s.getVehicleId(),
                    "冷链-" + s.getVehicleNo(),
                    s.getId(),  // voyageId 字段承载 shipmentId
                    "冷链温度异常",
                    "danger",
                    desc);
            log.warn("【冷链-告警】{} 温度={}°C 阈值=[{},{}]°C",
                    s.getConfirmationNo(), temperature, s.getMinTemp(), s.getMaxTemp());
        }
        return r;
    }

    public static boolean isAnomaly(ColdChainShipment s, BigDecimal temperature) {
        if (temperature == null) return false;
        return temperature.compareTo(s.getMinTemp()) < 0
                || temperature.compareTo(s.getMaxTemp()) > 0;
    }

    private ColdChainShipment mustShipment(String id) {
        return shipmentRepo.findById(id)
                .orElseThrow(() -> new BusinessException("运单不存在: " + id));
    }
}
