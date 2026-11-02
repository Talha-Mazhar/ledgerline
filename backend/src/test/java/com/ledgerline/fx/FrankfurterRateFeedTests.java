package com.ledgerline.fx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ledgerline.ledger.CurrencyCode;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** The rate feed against a fake HTTP server: parsing, retries, and the circuit breaker. */
class FrankfurterRateFeedTests {

    private static final String LATEST = "https://feed.test/latest?base=EUR";
    private static final String ANSWER = """
            {"amount":1.0,"base":"EUR","date":"2026-10-30",
             "rates":{"CZK":24.31,"GBP":0.8612,"USD":1.0874,"JPY":160.1}}""";

    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://feed.test");
    private final MockRestServiceServer server =
            MockRestServiceServer.bindTo(builder).build();
    private final FrankfurterRateFeed feed = new FrankfurterRateFeed(builder.build(), Duration.ofMillis(1));

    @Test
    void readsTheEcbRatesPerEuro() {
        server.expect(requestTo(LATEST)).andRespond(withSuccess(ANSWER, MediaType.APPLICATION_JSON));

        RateFeed.Rates rates = feed.latest();

        assertThat(rates.unitsPerEur())
                .containsEntry(CurrencyCode.CZK, new BigDecimal("24.31"))
                .containsEntry(CurrencyCode.EUR, BigDecimal.ONE)
                .hasSize(4);
        assertThat(rates.asOf()).isEqualTo(Instant.parse("2026-10-30T14:00:00Z"));
    }

    @Test
    void retriesABlip() {
        server.expect(requestTo(LATEST)).andRespond(withServerError());
        server.expect(requestTo(LATEST)).andRespond(withSuccess(ANSWER, MediaType.APPLICATION_JSON));

        assertThat(feed.latest().unitsPerEur()).containsKey(CurrencyCode.USD);
        server.verify();
    }

    @Test
    void stopsCallingAFailingFeed() {
        // Four calls, each tried three times, all failing: the breaker has seen enough and opens.
        server.expect(times(12), requestTo(LATEST)).andRespond(withServerError());

        for (int call = 0; call < 4; call++) {
            assertThatThrownBy(feed::latest).isNotInstanceOf(CallNotPermittedException.class);
        }
        assertThat(feed.state()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThatThrownBy(feed::latest).isInstanceOf(CallNotPermittedException.class);
        server.verify();
    }
}
