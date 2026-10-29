package com.ledgerline;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Boots the whole application against a real PostgreSQL (Testcontainers) and runs Flyway. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class LedgerlineApplicationTests {

    @Test
    void contextLoads() {}
}
