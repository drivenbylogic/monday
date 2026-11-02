package com.monday.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.repository.config.BootstrapMode;

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(
    basePackages = "com.monday.app.intelligence.repository",
    bootstrapMode = BootstrapMode.DEFERRED
)
@EntityScan(basePackages = "com.monday.app.intelligence.entity")
public class MondayApplication {

    public static void main(String[] args) {
        SpringApplication.run(MondayApplication.class, args);
    }
}
