package com.yourorg.eventdashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableCaching
public class EventDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventDashboardApplication.class, args);
    }
}
