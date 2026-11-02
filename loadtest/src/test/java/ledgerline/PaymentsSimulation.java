package ledgerline;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.rampUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.CookieKey;
import static io.gatling.javaapi.http.HttpDsl.getCookieValue;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.time.Duration;
import java.util.UUID;

/**
 * Customers using Ledgerline at the same time, through the frontend's nginx like a browser:
 * each signs up, opens a euro wallet, adds money, then sends money to the same recipient three
 * times while looking at their wallets and activity.
 *
 * <p>Every transfer goes to one recipient's wallet, so all virtual users compete for the same
 * ledger rows: the hardest case for the row locks that keep balances right.
 *
 * <p>Settings (system properties): {@code baseUrl} (default http://localhost:8000), {@code
 * usersPerSecond} (default 10) and {@code seconds} at full load (default 120). Run with {@code
 * ./mvnw gatling:test}; the report is in target/gatling.
 */
public class PaymentsSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8000");
    private static final double USERS_PER_SECOND = Double.parseDouble(System.getProperty("usersPerSecond", "10"));
    private static final long SECONDS = Long.parseLong(System.getProperty("seconds", "120"));

    private final HttpProtocolBuilder protocol = http.baseUrl(BASE_URL + "/api/v1")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .userAgentHeader("Ledgerline load test");

    /** The CSRF token the backend put in a cookie; every POST sends it back in a header. */
    private static final ChainBuilder READ_CSRF_TOKEN = exec(getCookieValue(CookieKey("XSRF-TOKEN").saveAs("csrf")));

    private static String newKey(Session session) {
        return UUID.randomUUID().toString();
    }

    private final ScenarioBuilder customer = scenario("Customer")
            .exec(session -> session.set("email", "load-" + UUID.randomUUID() + "@example.com"))
            // A first request, signed out, hands out the CSRF cookie.
            .exec(http("Who am I (signed out)").get("/auth/me").check(status().is(401)))
            .exec(READ_CSRF_TOKEN)
            .exec(http("Sign up")
                    .post("/auth/sign-up")
                    .header("X-XSRF-TOKEN", "#{csrf}")
                    .body(StringBody(
                            "{\"email\":\"#{email}\",\"displayName\":\"Load Tester\",\"password\":\"correct-horse-battery\"}"))
                    .check(status().is(201)))
            // Signing in replaces the CSRF token.
            .exec(READ_CSRF_TOKEN)
            .exec(http("Open wallet")
                    .post("/wallets")
                    .header("X-XSRF-TOKEN", "#{csrf}")
                    .body(StringBody("{\"currency\":\"EUR\"}"))
                    .check(status().is(201), jsonPath("$.id").saveAs("walletId")))
            .exec(http("Add money")
                    .post("/wallets/#{walletId}/deposits")
                    .header("X-XSRF-TOKEN", "#{csrf}")
                    .header("Idempotency-Key", PaymentsSimulation::newKey)
                    .body(StringBody("{\"amount\":\"500.00\"}"))
                    .check(status().is(201)))
            .repeat(3)
            .on(exec(http("Send money")
                            .post("/transfers")
                            .header("X-XSRF-TOKEN", "#{csrf}")
                            .header("Idempotency-Key", PaymentsSimulation::newKey)
                            .body(StringBody(
                                    "{\"fromWalletId\":\"#{walletId}\",\"recipientEmail\":\"jana@ledgerline.dev\",\"amount\":\"5.00\"}"))
                            .check(status().is(201)))
                    .exec(http("Wallets").get("/wallets").check(status().is(200)))
                    .exec(http("Activity")
                            .get("/wallets/#{walletId}/activity")
                            .check(status().is(200)))
                    .pause(Duration.ofMillis(200), Duration.ofMillis(800)))
            .exec(http("Payments").get("/payments").check(status().is(200)))
            .exec(http("Notifications").get("/notifications").check(status().is(200)));

    {
        setUp(customer.injectOpen(
                        rampUsersPerSec(1).to(USERS_PER_SECOND).during(Duration.ofSeconds(30)),
                        constantUsersPerSec(USERS_PER_SECOND).during(Duration.ofSeconds(SECONDS))))
                .protocols(protocol)
                .assertions(
                        global().failedRequests().percent().lt(1.0),
                        global().responseTime().percentile(95.0).lt(500),
                        details("Send money").responseTime().percentile(95.0).lt(300),
                        details("Add money").responseTime().percentile(95.0).lt(300));
    }
}
