package com.ledgerline.payments;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static com.ledgerline.ApiRequests.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.jayway.jsonpath.JsonPath;
import com.ledgerline.TestUsers;
import com.ledgerline.TestcontainersConfiguration;
import com.ledgerline.WithDemoUser;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sending money from the demo user to Jana (both created by the V5 migration; Jana has EUR and CZK
 * wallets). Every response is checked against {@code api/openapi.yaml}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
@WithDemoUser
class TransferApiTests {

    private static final OpenApiInteractionValidator CONTRACT = OpenApiInteractionValidator.createForSpecificationUrl(
                    Path.of("../api/openapi.yaml").toAbsolutePath().toUri().toString())
            .build();

    private static final String JANAS_EUR_WALLET = "00000000-0000-4000-8000-0000000a0001";

    private final MockMvc mvc;
    private String euros = "";

    TransferApiTests(@Autowired MockMvc mvc) {
        this.mvc = mvc;
    }

    @BeforeEach
    void openAWalletWithMoney() throws Exception {
        euros = openWithMoney("EUR", "100");
    }

    @Test
    void sendsMoneyToAnotherUser() throws Exception {
        mvc.perform(send(euros, "jana@ledgerline.dev", "30"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activity.kind").value("TRANSFER_OUT"))
                .andExpect(jsonPath("$.activity.amount.amount").value("-30.00"))
                .andExpect(jsonPath("$.activity.balanceAfter.amount").value("70.00"))
                .andExpect(jsonPath("$.activity.description").value("Sent to Jana Nováková"))
                .andExpect(jsonPath("$.recipient.displayName").value("Jana Nováková"))
                .andExpect(openApi().isValid(CONTRACT));

        mvc.perform(get("/api/v1/wallets/{walletId}/activity", JANAS_EUR_WALLET)
                        .with(TestUsers.signedInAs(TestUsers.JANA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].kind").value("TRANSFER_IN"))
                .andExpect(jsonPath("$.items[0].amount.amount").value("30.00"))
                .andExpect(jsonPath("$.items[0].description").value("Received from Demo user"))
                .andExpect(jsonPath("$.items[0].balanceAfter.amount").value("30.00"))
                .andExpect(openApi().isValid(CONTRACT));
    }

    @Test
    void aRetryWithTheSameKeySendsTheMoneyOnce() throws Exception {
        UUID key = UUID.randomUUID();
        mvc.perform(send(euros, "jana@ledgerline.dev", "30", key)).andExpect(status().isCreated());
        mvc.perform(send(euros, "jana@ledgerline.dev", "30", key))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activity.balanceAfter.amount").value("70.00"));

        mvc.perform(get("/api/v1/wallets/{walletId}", euros)).andExpect(jsonPath("$.balance.amount").value("70.00"));
    }

    @Test
    void cantSendMoreThanTheWalletHolds() throws Exception {
        mvc.perform(send(euros, "jana@ledgerline.dev", "100.01"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Not enough money"))
                .andExpect(openApi().isValid(CONTRACT));
    }

    @Test
    void theRecipientMustExist() throws Exception {
        mvc.perform(send(euros, "nobody@example.com", "10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recipient not found"))
                .andExpect(openApi().isValid(CONTRACT));
    }

    @Test
    void theRecipientNeedsAWalletInTheCurrency() throws Exception {
        String dollars = openWithMoney("USD", "50");
        mvc.perform(send(dollars, "jana@ledgerline.dev", "10"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Jana Nováková has no USD wallet yet."))
                .andExpect(openApi().isValid(CONTRACT));
    }

    @Test
    void cantSendToYourself() throws Exception {
        mvc.perform(send(euros, "demo@ledgerline.dev", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(openApi().isValid(CONTRACT));
    }

    private String openWithMoney(String currency, String amount) throws Exception {
        String created = mvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"" + currency + "\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String walletId = JsonPath.read(created, "$.id");
        mvc.perform(post("/api/v1/wallets/{walletId}/deposits", walletId)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"" + amount + "\"}"))
                .andExpect(status().isCreated());
        return walletId;
    }

    private static RequestBuilder send(String from, String recipientEmail, String amount) {
        return send(from, recipientEmail, amount, UUID.randomUUID());
    }

    private static RequestBuilder send(String from, String recipientEmail, String amount, UUID key) {
        return post("/api/v1/transfers")
                .header("Idempotency-Key", key.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromWalletId\":\"" + from + "\",\"recipientEmail\":\"" + recipientEmail
                        + "\",\"amount\":\"" + amount + "\"}");
    }
}
