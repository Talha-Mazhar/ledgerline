package com.ledgerline.payments;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static com.ledgerline.ApiRequests.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * Exchange over HTTP. The demo rates are EUR 1, CZK 25.15 per euro, with a 0.5% spread: 1 EUR buys
 * 25.024250 CZK, so 100 EUR buys 2,502.42 CZK (rounded down).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
@WithDemoUser
class ExchangeApiTests {

    private static final OpenApiInteractionValidator CONTRACT = OpenApiInteractionValidator.createForSpecificationUrl(
                    Path.of("../api/openapi.yaml").toAbsolutePath().toUri().toString())
            .build();

    private final MockMvc mvc;
    private String euros = "";
    private String koruna = "";

    ExchangeApiTests(@Autowired MockMvc mvc) {
        this.mvc = mvc;
    }

    @BeforeEach
    void openTwoWalletsAndAddEuros() throws Exception {
        euros = open("EUR");
        koruna = open("CZK");
        mvc.perform(post("/api/v1/wallets/{walletId}/deposits", euros)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"100\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void quotesTheRateWithTheSpread() throws Exception {
        mvc.perform(get("/api/v1/exchange-rates").param("from", "EUR").param("to", "CZK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.midRate").value("25.150000"))
                .andExpect(jsonPath("$.rate").value("25.024250"))
                .andExpect(openApi().isValid(CONTRACT));
    }

    @Test
    void previewsAnExchangeWithoutMovingMoney() throws Exception {
        mvc.perform(post("/api/v1/exchanges/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(euros, koruna, "100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buy.amount").value("2502.42"))
                .andExpect(openApi().isValid(CONTRACT));

        mvc.perform(get("/api/v1/wallets/{walletId}", euros))
                .andExpect(jsonPath("$.balance.amount").value("100.00"));
    }

    @Test
    void exchangesBetweenTwoWallets() throws Exception {
        mvc.perform(exchange(euros, koruna, "100"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sell.kind").value("EXCHANGE_OUT"))
                .andExpect(jsonPath("$.sell.balanceAfter.amount").value("0.00"))
                .andExpect(jsonPath("$.buy.kind").value("EXCHANGE_IN"))
                .andExpect(jsonPath("$.buy.amount.amount").value("2502.42"))
                .andExpect(openApi().isValid(CONTRACT));

        mvc.perform(get("/api/v1/wallets/{walletId}", koruna))
                .andExpect(jsonPath("$.balance.amount").value("2502.42"));
    }

    @Test
    void refusesToSellMoreThanTheWalletHolds() throws Exception {
        mvc.perform(exchange(euros, koruna, "100.01"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Not enough money"))
                .andExpect(openApi().isValid(CONTRACT));
    }

    @Test
    void refusesToExchangeWithinOneCurrency() throws Exception {
        mvc.perform(exchange(euros, euros, "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Choose two wallets in different currencies."));
    }

    private String open(String currency) throws Exception {
        String created = mvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"" + currency + "\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(created, "$.id");
    }

    private static RequestBuilder exchange(String from, String to, String amount) {
        return post("/api/v1/exchanges")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(from, to, amount));
    }

    private static String body(String from, String to, String amount) {
        return "{\"fromWalletId\":\"" + from + "\",\"toWalletId\":\"" + to + "\",\"amount\":\"" + amount + "\"}";
    }
}
