package com.ledgerline.wallets;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.ledgerline.TestcontainersConfiguration;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * The wallets API over HTTP, against a real PostgreSQL.
 *
 * <p>Every successful or expected-error response is also checked against {@code api/openapi.yaml}:
 * if the backend returns a field the contract doesn't have, misses a required one, or uses another
 * status code or content type, the test fails. That keeps the hand-written backend and the
 * frontend's generated client in agreement.
 *
 * <p>Each test runs in a transaction that is rolled back afterwards, so tests don't see each
 * other's wallets.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class WalletApiTests {

    private static final OpenApiInteractionValidator CONTRACT = OpenApiInteractionValidator.createForSpecificationUrl(
                    Path.of("../api/openapi.yaml").toAbsolutePath().toUri().toString())
            .build();

    private final MockMvc mvc;

    WalletApiTests(@Autowired MockMvc mvc) {
        this.mvc = mvc;
    }

    @Test
    void aNewUserHasNoWallets() throws Exception {
        mvc.perform(get("/api/v1/wallets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", empty()))
                .andExpect(openApi().isValid(CONTRACT));
    }

    @Test
    void opensAWalletWithAZeroBalance() throws Exception {
        mvc.perform(openWallet("EUR"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/wallets/")))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.balance.amount").value("0.00"))
                .andExpect(jsonPath("$.balance.currency").value("EUR"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(openApi().isValid(CONTRACT));
    }

    @Test
    void listsEveryOpenedWallet() throws Exception {
        mvc.perform(openWallet("EUR")).andExpect(status().isCreated());
        mvc.perform(openWallet("CZK")).andExpect(status().isCreated());

        mvc.perform(get("/api/v1/wallets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].currency", containsInAnyOrder("EUR", "CZK")))
                .andExpect(openApi().isValid(CONTRACT));
    }

    @Test
    void getsAnOpenedWalletAtItsLocation() throws Exception {
        MvcResult opened = mvc.perform(openWallet("GBP"))
                .andExpect(status().isCreated())
                .andReturn();
        String location = Objects.requireNonNull(opened.getResponse().getHeader("Location"));

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("GBP"))
                .andExpect(openApi().isValid(CONTRACT));
    }

    @Test
    void refusesASecondWalletInTheSameCurrency() throws Exception {
        mvc.perform(openWallet("USD")).andExpect(status().isCreated());

        mvc.perform(openWallet("USD"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("You already have a USD wallet."))
                .andExpect(openApi().isValid(CONTRACT));
    }

    @Test
    void answersNotFoundForAWalletTheUserDoesNotHave() throws Exception {
        mvc.perform(get("/api/v1/wallets/{walletId}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Wallet not found"))
                .andExpect(openApi().isValid(CONTRACT));
    }

    // The next two requests break the contract on purpose, so only the response is checked.

    @Test
    void rejectsACurrencyLedgerlineDoesNotSupport() throws Exception {
        mvc.perform(openWallet("JPY"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void rejectsARequestWithoutACurrency() throws Exception {
        mvc.perform(post("/api/v1/wallets").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private static RequestBuilder openWallet(String currency) {
        return post("/api/v1/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currency\":\"" + currency + "\"}");
    }
}
