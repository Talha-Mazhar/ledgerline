package com.ledgerline.fx;

import com.ledgerline.ledger.CurrencyCode;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * The ECB's daily reference rates through the Frankfurter API ({@code GET /latest?base=EUR}).
 *
 * <p>An external API fails sometimes, so each call is wrapped twice (Resilience4j):
 *
 * <ul>
 *   <li><b>Retry</b>: up to 3 attempts, 2 seconds apart, for a blip.
 *   <li><b>Circuit breaker</b>: if most recent calls failed, stop calling for 5 minutes and fail at
 *       once. A struggling API isn't hammered, and the caller doesn't wait on timeouts.
 * </ul>
 *
 * Either way a failure leaves the stored rates in place; the app keeps working on the last ones.
 */
@Component
class FrankfurterRateFeed implements RateFeed {

    /** The ECB publishes around 16:00 Frankfurt time; the date alone is stored as 14:00 UTC. */
    private static final int PUBLICATION_HOUR_UTC = 14;

    /** The shape of Frankfurter's answer. */
    record Latest(String base, LocalDate date, Map<String, BigDecimal> rates) {}

    private final RestClient client;
    private final CircuitBreaker breaker;
    private final Retry retry;

    @Autowired
    FrankfurterRateFeed(RestClient.Builder builder, FxProperties properties) {
        this(client(builder, properties), Duration.ofSeconds(2));
    }

    /** For tests: a client against a fake server, and a short wait between retries. */
    FrankfurterRateFeed(RestClient client, Duration retryWait) {
        this.client = client;
        this.breaker = CircuitBreaker.of(
                "ecb-rates",
                CircuitBreakerConfig.custom()
                        .slidingWindowSize(4)
                        .minimumNumberOfCalls(4)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofMinutes(5))
                        .build());
        this.retry = Retry.of(
                "ecb-rates",
                RetryConfig.custom().maxAttempts(3).waitDuration(retryWait).build());
    }

    private static RestClient client(RestClient.Builder builder, FxProperties properties) {
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory();
        requests.setReadTimeout(properties.timeout());
        return builder.baseUrl(properties.feedUrl()).requestFactory(requests).build();
    }

    @Override
    public Rates latest() {
        Supplier<Rates> call = Retry.decorateSupplier(retry, this::fetch);
        return CircuitBreaker.decorateSupplier(breaker, call).get();
    }

    CircuitBreaker.State state() {
        return breaker.getState();
    }

    private Rates fetch() {
        Latest latest = client.get()
                .uri(uri -> uri.path("/latest").queryParam("base", "EUR").build())
                .retrieve()
                .body(Latest.class);
        if (latest == null || !"EUR".equals(latest.base())) {
            throw new IllegalStateException("Unexpected answer from the rate feed");
        }
        Map<CurrencyCode, BigDecimal> perEur = new EnumMap<>(CurrencyCode.class);
        perEur.put(CurrencyCode.EUR, BigDecimal.ONE);
        for (CurrencyCode currency : CurrencyCode.values()) {
            @Nullable BigDecimal rate = latest.rates().get(currency.name());
            if (currency != CurrencyCode.EUR) {
                if (rate == null) {
                    throw new IllegalStateException("The rate feed has no " + currency + " rate");
                }
                perEur.put(currency, rate);
            }
        }
        return new Rates(perEur, latest.date().atTime(PUBLICATION_HOUR_UTC, 0).toInstant(ZoneOffset.UTC));
    }
}
