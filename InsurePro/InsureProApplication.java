package com.insurepro;

// ============================================================
// InsureProApplication.java
// Main entry point for the InsurePro Spring Boot Backend
// ============================================================

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing          // Enables @CreatedDate / @LastModifiedDate on entities
@EnableAsync                // Enables @Async for non-blocking notification dispatch
@EnableScheduling           // Enables @Scheduled for compliance report generation
public class InsureProApplication {

    public static void main(String[] args) {
        SpringApplication.run(InsureProApplication.class, args);
    }
}
