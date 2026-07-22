package com.fishing.platform.config;

import com.fishing.platform.entity.*;
import com.fishing.platform.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 初始化种子数据：覆盖第一阶段省内主要渔港与海区
 */
@Configuration
public class DataInitializer {

    @Autowired private VesselRepository vesselRepo;
    @Autowired private QuotaRuleRepository quotaRepo;

    @Bean
    public CommandLineRunner initSeedData() {
        return args -> {
            if (vesselRepo.count() > 0) {
                return;
            }
            // 三个示范渔港
            String[][] ports = {
                    {"泉州祥芝渔港", "闽南近海渔区"},
                    {"福州连江黄岐渔港", "闽东近海渔区"},
                    {"漳州龙海港尾渔港", "闽南近海渔区"}
            };
            String[] species = {"带鱼", "黄花鱼", "鲳鱼", "墨鱼", "梭子蟹", "虾蛄"};

            // 创建配额规则
            int year = LocalDate.now().getYear();
            for (String[] port : ports) {
                for (String s : species) {
                    QuotaRule r = new QuotaRule();
                    r.setId(UUID.randomUUID().toString().replace("-", ""));
                    r.setYear(year);
                    r.setSeaAreaName(port[1]);
                    r.setSpecies(s);
                    r.setTotalQuota(new BigDecimal("50000"));
                    r.setMinSize("符合最小可捕规格");
                    r.setBanned(false);
                    quotaRepo.save(r);
                }
            }

            // 创建一些示范船
            for (int p = 0; p < ports.length; p++) {
                for (int i = 1; i <= 4; i++) {
                    Vessel v = new Vessel();
                    v.setId(UUID.randomUUID().toString().replace("-", ""));
                    v.setVesselNo(ports[p][0].substring(0, 2) + "-渔-" + (1000 + p * 10 + i));
                    v.setVesselName(ports[p][0] + "号船" + i);
                    v.setOwnerName("船东" + (p + 1) + "-" + i);
                    v.setCaptainName("船长" + i);
                    v.setPhone("1380000000" + (p * 10 + i));
                    v.setPortName(ports[p][0]);
                    v.setSeaAreaName(ports[p][1]);
                    v.setCertValidFrom(LocalDate.now().minusYears(1));
                    v.setCertValidTo(LocalDate.now().plusYears(2));
                    v.setSuspended(false);
                    v.setStatus("在港");
                    vesselRepo.save(v);
                }
            }
        };
    }
}
