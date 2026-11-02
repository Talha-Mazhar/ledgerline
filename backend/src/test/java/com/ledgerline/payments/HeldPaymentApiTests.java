package com.ledgerline.payments;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static com.ledgerline.ApiRequests.post;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.ledgerline.ApiScenario;
import com.ledgerline.TestUsers;
import com.ledgerline.TestcontainersConfiguration;
import com.ledgerline.identity.SignedInUser;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Payments that the risk rules hold, end to end over HTTP: the four-eyes approval, rejection,
 * refunds, and the audit trail they leave.
 *
 * <p>Not {@code @Transactional}: approvals publish events that the audit module handles after
 * commit, so these tests commit. Each test signs up its own customers, so nothing they do is
 * visible to other tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class HeldPaymentApiTests {

    private static final OpenApiInteractionValidator CONTRACT = OpenApiInteractionValidator.createForSpecificationUrl(
                    Path.of("../api/openapi.yaml").toAbsolutePath().toUri().toString())
            .build();

    private final MockMvc mvc;
    private final ApiScenario scenario;
    private final JdbcClient jdbc;

    private SignedInUser alice = TestUsers.DEMO;
    private SignedInUser bob = TestUsers.JANA;
    private String alicesEuros = "";
    private String bobsEuros = "";

    HeldPaymentApiTests(@Autowired MockMvc mvc, @Autowired JdbcClient jdbc) {
        this.mvc = mvc;
        this.scenario = new ApiScenario(mvc);
        this.jdbc = jdbc;
    }

    @BeforeEach
    void twoCustomersWithEuroWallets() throws Exception {
        alice = scenario.signUp("Alice Held");
        bob = scenario.signUp("Bob Held");
        alicesEuros = scenario.openWallet(alice, "EUR");
        bobsEuros = scenario.openWallet(bob, "EUR");
        scenario.deposit(alice, alicesEuros, "5000");
    }

    @Test
    void aTransferAboveTheLimitWaitsForTwoDifferentStaffMembers() throws Exception {
        ResultActions sent = scenario.send(alice, alicesEuros, bob.email(), "1500")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("HELD"))
                .andExpect(jsonPath("$.activity.kind").value("HELD"))
                .andExpect(jsonPath("$.activity.balanceAfter.amount").value("3500.00"))
                .andExpect(openApi().isValid(CONTRACT));
        String paymentId = ApiScenario.read(sent, "$.paymentId");
        assertThat(balanceOf(bob, bobsEuros)).isEqualTo("0.00");

        // Newest first: other tests leave held payments behind in the shared database.
        mvc.perform(get("/api/v1/back-office/payments")
                        .param("status", "HELD")
                        .with(TestUsers.signedInAs(TestUsers.SUPPORT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == '" + paymentId + "')].holdReason")
                        .value(List.of("Above the single-payment limit of €1,000.00."
                                + " First payment to this recipient is above €500.00.")))
                .andExpect(openApi().isValid(CONTRACT));

        approve(TestUsers.SUPPORT, paymentId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_SECOND_APPROVAL"))
                .andExpect(jsonPath("$.firstApprover").value("Sam Support"))
                .andExpect(openApi().isValid(CONTRACT));
        assertThat(balanceOf(bob, bobsEuros)).as("one approval isn't enough").isEqualTo("0.00");

        approve(TestUsers.SUPPORT, paymentId)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("A second, different person has to approve this payment."))
                .andExpect(openApi().isValid(CONTRACT));

        approve(TestUsers.ADMIN, paymentId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.secondApprover").value("Alex Admin"));
        assertThat(balanceOf(bob, bobsEuros)).isEqualTo("1500.00");
        assertThat(balanceOf(alice, alicesEuros)).isEqualTo("3500.00");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(auditActionsFor(paymentId))
                .contains("PAYMENT_HELD", "PAYMENT_APPROVED_FIRST", "PAYMENT_APPROVED_FINAL"));
    }

    @Test
    void customersCantApprovePayments() throws Exception {
        String paymentId = ApiScenario.read(scenario.send(alice, alicesEuros, bob.email(), "1500"), "$.paymentId");

        approve(bob, paymentId).andExpect(status().isForbidden()).andExpect(openApi().isValid(CONTRACT));
    }

    @Test
    void aRejectedWithdrawalReturnsTheMoney() throws Exception {
        String paymentId = ApiScenario.read(
                scenario.withdraw(alice, alicesEuros, "1200")
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.kind").value("HELD")),
                "$.entryId");
        String heldPayment = jdbc.sql("SELECT id::text FROM payments WHERE entry_id = :entry")
                .param("entry", UUID.fromString(paymentId))
                .query(String.class)
                .single();

        mvc.perform(post("/api/v1/back-office/payments/{id}/reject", heldPayment)
                        .with(TestUsers.signedInAs(TestUsers.SUPPORT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"We couldn't confirm the bank account.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(openApi().isValid(CONTRACT));

        assertThat(balanceOf(alice, alicesEuros)).isEqualTo("5000.00");
        mvc.perform(get("/api/v1/wallets/{id}/activity", alicesEuros).with(TestUsers.signedInAs(alice)))
                .andExpect(jsonPath("$.items[0].kind").value("RELEASED"))
                .andExpect(jsonPath("$.items[0].description").value("Returned: withdrawal was not approved"));
        mvc.perform(get("/api/v1/payments").with(TestUsers.signedInAs(alice)))
                .andExpect(jsonPath("$.items[0].status").value("REJECTED"))
                .andExpect(jsonPath("$.items[0].rejectionReason").value("We couldn't confirm the bank account."))
                .andExpect(openApi().isValid(CONTRACT));
    }

    @Test
    void staffRefundATransferOnce() throws Exception {
        String paymentId = ApiScenario.read(
                scenario.send(alice, alicesEuros, bob.email(), "30")
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status").value("COMPLETED")),
                "$.paymentId");

        mvc.perform(post("/api/v1/back-office/payments/{id}/refund", paymentId)
                        .with(TestUsers.signedInAs(TestUsers.SUPPORT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.refundable").value(false))
                .andExpect(openApi().isValid(CONTRACT));
        assertThat(balanceOf(alice, alicesEuros)).isEqualTo("5000.00");
        assertThat(balanceOf(bob, bobsEuros)).isEqualTo("0.00");

        mvc.perform(post("/api/v1/back-office/payments/{id}/refund", paymentId)
                        .with(TestUsers.signedInAs(TestUsers.ADMIN)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Only a completed payment can be refunded."));
    }

    @Test
    void customersSeeTheirPaymentsWithTheirStatus() throws Exception {
        scenario.send(alice, alicesEuros, bob.email(), "1500");
        scenario.send(alice, alicesEuros, bob.email(), "20");

        mvc.perform(get("/api/v1/payments").with(TestUsers.signedInAs(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].description").value("Sent to Bob Held"))
                .andExpect(jsonPath("$.items[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.items[1].status").value("HELD"))
                .andExpect(jsonPath("$.items[2].description").value("Added money"))
                .andExpect(jsonPath("$.items[2].direction").value("IN"))
                .andExpect(openApi().isValid(CONTRACT));
        mvc.perform(get("/api/v1/payments").with(TestUsers.signedInAs(bob)))
                .andExpect(jsonPath("$.items[0].description").value("Received from Alice Held"))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    private ResultActions approve(SignedInUser staff, String paymentId) throws Exception {
        return mvc.perform(post("/api/v1/back-office/payments/{id}/approve", paymentId)
                .with(TestUsers.signedInAs(staff)));
    }

    private String balanceOf(SignedInUser owner, String walletId) throws Exception {
        return ApiScenario.read(
                mvc.perform(get("/api/v1/wallets/{id}", walletId).with(TestUsers.signedInAs(owner))),
                "$.balance.amount");
    }

    private List<@Nullable String> auditActionsFor(String paymentId) {
        return jdbc.sql("SELECT action FROM audit_log WHERE target_id = :id ORDER BY occurred_at")
                .param("id", UUID.fromString(paymentId))
                .query(String.class)
                .list();
    }
}
