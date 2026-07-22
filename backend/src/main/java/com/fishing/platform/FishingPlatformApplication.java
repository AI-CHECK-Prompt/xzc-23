package com.fishing.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 近海小型渔船渔获登记与配额管控平台 启动入口
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class FishingPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(FishingPlatformApplication.class, args);
    }
}
