package com.ledgerline.payments;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static com.ledgerline.ApiRequests.post;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.jayway.jsonpath.JsonPath;
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
 * Deposits, withdrawals and activity over HTTP, against a real PostgreSQL, every response checked
 * against {@code api/openapi.yaml}. Each test opens its own EUR wallet inside a rolled-back
 * transaction.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
@WithDemoUser
class MoneyMovementApiTests {

    private static final OpenApiInteractionValidator CONTRACT = OpenApiInteractionValidator.createForSpecificationUrl(
                    Path.of("../api/openapi.yaml").toAbsolutePath().toUri().toString())
            .build();

    private final MockMvc mvc;
    private String walletId = "";

    MoneyMovementApiTests(@Autowired MockMvc mvc) {
        this.mvc = mvc;
    }

    @BeforeEach
    void openAWallet() throws Exception {
        String body = mvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"EUR\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        walletId = JsonPath.read(body, "$.id");
    }

    @Test
    void addsMoneyToAWallet() throws Exception {
        mvc.perform(move("deposits", UUID.randomUUID(), "250.5"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kind").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount.amount").value("250.50"))
                .andExpect(jsonPath("$.balanceAfter.amount").value("250.50"))
                .andExpect(jsonPath("$.walletId").value(walletId))
                .andExpect(openApi().isValid(CONTRACT));

        mvc.perform(get("/api/v1/wallets/{walletId}", walletId))
                .andExpect(jsonPath("$.balance.amount").value("250.50"))
                .andExpect(openApi().isValid(CONTRACT));
    }

    @Test
    void withdrawsMoneyAndShowsTheBalanceAfterIt() throws Exception {
        mvc.perform(move("deposits", UUID.randomUUID(), "100")).andExpect(status().isCreated());

        mvc.perform(move("withdrawals", UUID.randomUUID(), "40"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kind").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.amount.amount").value("-40.00"))
                .andExpect(jsonPath("$.balanceAfter.amount").value("60.00"))
                .andExpect(openApi().isValid(CONTRACT));
    }

    @Test
    void refusesToWithdrawMoreThanTheWalletHolds() throws Exception {
        mvc.perform(move("deposits", UUID.randomUUID(), "30")).andExpect(status().isCreated());

        mvc.perform(move("withdrawals", UUID.randomUUID(), "30.01"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not enough money"))
                .andExpect(openApi().isValid(CONTRACT));
    }

    @Test
    void aRetryWithTheSameKeyMovesTheMoneyOnce() throws Exception {
        UUID key = UUID.randomUUID();
        String first = mvc.perform(move("deposits", key, "75"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        mvc.perform(move("deposits", key, "75"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(JsonPath.<String>read(first, "$.id")))
                .andExpect(openApi().isValid(CONTRACT));

        mvc.perform(get("/api/v1/wallets/{walletId}", walletId))
                .andExpect(jsonPath("$.balance.amount").value("75.00"));
    }

    @Test
    void refusesAKeyReusedForADifferentRequest() throws Exception {
        UUID key = UUID.randomUUID();
        mvc.perform(move("deposits", key, "75")).andExpect(status().isCreated());

        mvc.perform(move("deposits", key, "80"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Idempotency key reused"))
                .andExpect(openApi().isValid(CONTRACT));
    }

    @Test
    void listsActivityNewestFirst() throws Exception {
        mvc.perform(move("deposits", UUID.randomUUID(), "100")).andExpect(status().isCreated());
        mvc.perform(move("withdrawals", UUID.randomUUID(), "25")).andExpect(status().isCreated());

        mvc.perform(get("/api/v1/wallets/{walletId}/activity", walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].kind", contains("WITHDRAWAL", "DEPOSIT")))
                .andExpect(jsonPath("$.items[0].balanceAfter.amount").value("75.00"))
                .andExpect(openApi().isValid(CONTRACT));

        mvc.perform(get("/api/v1/activity").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].kind", contains("WITHDRAWAL")))
                .andExpect(openApi().isValid(CONTRACT));
    }

    // The requests below break the contract on purpose, so only the response is checked.

    @Test
    void refusesAnAmountOfZero() throws Exception {
        mvc.perform(move("deposits", UUID.randomUUID(), "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Enter an amount greater than zero."));
    }

    @Test
    void requiresAnIdempotencyKey() throws Exception {
        mvc.perform(post("/api/v1/wallets/{walletId}/deposits", walletId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"10\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private RequestBuilder move(String kind, UUID key, String amount) {
        return post("/api/v1/wallets/{walletId}/" + kind, walletId)
                .header("Idempotency-Key", key.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":\"" + amount + "\"}");
    }
}
