package com.ledgerline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Ledgerline API.
 *
 * <p>One deployable application, split into application modules: each direct sub-package of
 * {@code com.ledgerline} ({@code identity}, {@code wallets}, {@code ledger}, {@code payments},
 * {@code fx}) is a module with its own public API and internal code. {@code ModularityTests}
 * fails the build if a module uses another module's internals.
 */
@SpringBootApplication
public class LedgerlineApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerlineApplication.class, args);
    }
}
