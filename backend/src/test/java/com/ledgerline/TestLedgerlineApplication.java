package com.ledgerline;

import org.springframework.boot.SpringApplication;

/**
 * Runs the application with a throwaway PostgreSQL from Testcontainers instead of Docker Compose.
 * Start it from the IDE, or with {@code ./mvnw spring-boot:test-run}.
 */
public class TestLedgerlineApplication {

    public static void main(String[] args) {
        SpringApplication.from(LedgerlineApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
