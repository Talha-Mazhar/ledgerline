package com.ledgerline.assistant;

import static com.ledgerline.ApiRequests.post;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ledgerline.ApiScenario;
import com.ledgerline.TestUsers;
import com.ledgerline.TestcontainersConfiguration;
import com.ledgerline.identity.SignedInUser;
import com.ledgerline.wallets.WalletReader;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The assistant's tools return the customer's real figures, and only theirs; the model itself is
 * not running in tests, which checks the answer the customer gets when it's down.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AssistantTests {

    private final MockMvc mvc;
    private final ApiScenario scenario;
    private final WalletReader wallets;

    AssistantTests(@Autowired MockMvc mvc, @Autowired WalletReader wallets) {
        this.mvc = mvc;
        this.scenario = new ApiScenario(mvc);
        this.wallets = wallets;
    }

    @Test
    void theToolsSummariseWhereTheMoneyWent() throws Exception {
        SignedInUser customer = scenario.signUp("Tom Tools");
        SignedInUser landlord = scenario.signUp("Lena Landlord");
        String euros = scenario.openWallet(customer, "EUR");
        scenario.openWallet(landlord, "EUR");
        scenario.deposit(customer, euros, "400");
        scenario.send(customer, euros, landlord.email(), "250");
        scenario.send(customer, euros, landlord.email(), "50");
        scenario.withdraw(customer, euros, "20");
        String today = LocalDate.now(ZoneOffset.UTC).toString();

        MoneyTools tools = new MoneyTools(customer.id(), wallets);

        assertThat(tools.balances()).containsExactly(new MoneyTools.Balance("EUR", "80.00"));
        MoneyTools.Summary summary = tools.summary(today, today);
        assertThat(summary.moneyIn()).containsEntry("EUR", "400.00");
        assertThat(summary.moneyOut()).containsEntry("EUR", "320.00");
        assertThat(summary.biggestOutgoing().getFirst())
                .isEqualTo(new MoneyTools.Spending("Sent to Lena Landlord", "EUR", "300.00"));
        assertThat(tools.activity(today, today)).hasSize(4);
        assertThat(new MoneyTools(landlord.id(), wallets).balances())
                .as("each customer's tools see only their own money")
                .containsExactly(new MoneyTools.Balance("EUR", "300.00"));
    }

    @Test
    void saysWhenTheModelIsUnavailable() throws Exception {
        mvc.perform(post("/api/v1/assistant/questions")
                        .with(TestUsers.signedInAs(TestUsers.DEMO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Where did my money go this month?\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Assistant unavailable"));
    }

    @Test
    void refusesAnEmptyQuestion() throws Exception {
        mvc.perform(post("/api/v1/assistant/questions")
                        .with(TestUsers.signedInAs(TestUsers.DEMO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }
}
