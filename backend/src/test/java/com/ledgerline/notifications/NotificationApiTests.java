package com.ledgerline.notifications;

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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Notifications end to end: an action publishes an event, it travels through Kafka, the consumer
 * stores a notification, and the user reads it through the API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class NotificationApiTests {

    private static final OpenApiInteractionValidator CONTRACT = OpenApiInteractionValidator.createForSpecificationUrl(
                    Path.of("../api/openapi.yaml").toAbsolutePath().toUri().toString())
            .build();

    private final MockMvc mvc;
    private final ApiScenario scenario;

    NotificationApiTests(@Autowired MockMvc mvc) {
        this.mvc = mvc;
        this.scenario = new ApiScenario(mvc);
    }

    @Test
    void bothSidesOfATransferAreNotified() throws Exception {
        SignedInUser sender = scenario.signUp("Nina Notify");
        SignedInUser recipient = scenario.signUp("Rita Receive");
        String euros = scenario.openWallet(sender, "EUR");
        scenario.openWallet(recipient, "EUR");
        scenario.deposit(sender, euros, "100");
        scenario.send(sender, euros, recipient.email(), "25").andExpect(status().isCreated());

        // Compared as text, so a failure shows every notification the recipient has.
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(mvc.perform(
                                get("/api/v1/notifications").with(TestUsers.signedInAs(recipient)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(StandardCharsets.UTF_8))
                .contains("\"unread\":2")
                .contains("\"title\":\"Money received\",\"body\":\"Nina Notify sent you €25.00.\"")
                .contains("Welcome to Ledgerline"));

        mvc.perform(get("/api/v1/notifications").with(TestUsers.signedInAs(recipient)))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(CONTRACT));

        mvc.perform(post("/api/v1/notifications/read-all").with(TestUsers.signedInAs(recipient)))
                .andExpect(status().isNoContent())
                .andExpect(openApi().isValid(CONTRACT));
        mvc.perform(get("/api/v1/notifications").with(TestUsers.signedInAs(recipient)))
                .andExpect(jsonPath("$.unread").value(0))
                .andExpect(jsonPath("$.items[0].read").value(true));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(mvc.perform(
                                get("/api/v1/notifications").with(TestUsers.signedInAs(sender)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(StandardCharsets.UTF_8))
                .contains("\"title\":\"Money sent\",\"body\":\"You sent €25.00 to Rita Receive.\""));
    }

    @Test
    void staffHearAboutHeldPayments() throws Exception {
        SignedInUser sender = scenario.signUp("Hugo Hold");
        SignedInUser recipient = scenario.signUp("Ida Inbox");
        String euros = scenario.openWallet(sender, "EUR");
        scenario.openWallet(recipient, "EUR");
        scenario.deposit(sender, euros, "3000");
        scenario.send(sender, euros, recipient.email(), "1500").andExpect(jsonPath("$.status").value("HELD"));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> mvc.perform(
                        get("/api/v1/notifications").param("limit", "100").with(TestUsers.signedInAs(TestUsers.SUPPORT)))
                .andExpect(jsonPath("$.items[?(@.body == \"Hugo Hold's transfer of €1,500.00 is waiting for two approvals.\")]")
                        .exists()));
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> mvc.perform(
                        get("/api/v1/notifications").with(TestUsers.signedInAs(sender)))
                .andExpect(jsonPath("$.items[?(@.title == 'Payment waiting for approval')]").exists()));
    }
}
