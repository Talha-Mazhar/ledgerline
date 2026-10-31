package com.ledgerline;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The application reads the time from an injected {@link Clock} instead of calling {@code
 * Instant.now()}, so tests can fix the time and timestamps are always UTC.
 */
@Configuration(proxyBeanMethods = false)
class ClockConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
