package com.ledgerline.statements;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.ledgerline.ApiScenario;
import com.ledgerline.TestUsers;
import com.ledgerline.TestcontainersConfiguration;
import com.ledgerline.documents.DocumentStore;
import com.ledgerline.identity.SignedInUser;
import com.ledgerline.ledger.Ledger;
import com.ledgerline.wallets.OwnedWallet;
import com.ledgerline.wallets.WalletDirectory;
import java.nio.file.Path;
import java.time.Duration;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

/** Monthly statements match the ledger to the cent, as data and as the PDF a customer downloads. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class StatementApiTests {

    private static final OpenApiInteractionValidator CONTRACT = OpenApiInteractionValidator.createForSpecificationUrl(
                    Path.of("../api/openapi.yaml").toAbsolutePath().toUri().toString())
            .build();

    private final MockMvc mvc;
    private final ApiScenario scenario;
    private final Statements statements;
    private final Ledger ledger;
    private final WalletDirectory wallets;
    private final MonthlyStatementJob job;
    private final DocumentStore documents;
    private final JdbcClient jdbc;

    private SignedInUser customer = TestUsers.DEMO;
    private String euros = "";

    StatementApiTests(
            @Autowired MockMvc mvc,
            @Autowired Statements statements,
            @Autowired Ledger ledger,
            @Autowired WalletDirectory wallets,
            @Autowired MonthlyStatementJob job,
            @Autowired DocumentStore documents,
            @Autowired JdbcClient jdbc) {
        this.mvc = mvc;
        this.scenario = new ApiScenario(mvc);
        this.statements = statements;
        this.ledger = ledger;
        this.wallets = wallets;
        this.job = job;
        this.documents = documents;
        this.jdbc = jdbc;
    }

    @BeforeEach
    void aMonthOfActivity() throws Exception {
        customer = scenario.signUp("Stella Statement");
        SignedInUser friend = scenario.signUp("Fred Friend");
        euros = scenario.openWallet(customer, "EUR");
        scenario.openWallet(friend, "EUR");
        scenario.deposit(customer, euros, "100");
        scenario.send(customer, euros, friend.email(), "30.25");
        scenario.withdraw(customer, euros, "19.50");
    }

    @Test
    void theStatementMatchesTheLedgerToTheCent() {
        MonthlyStatement statement = statements.statement(customer.id(), UUID.fromString(euros), thisMonth());

        assertThat(statement.opening().toPlainString()).isEqualTo("0.00");
        assertThat(statement.moneyIn().toPlainString()).isEqualTo("100.00");
        assertThat(statement.moneyOut().toPlainString()).isEqualTo("-49.75");
        assertThat(statement.closing()).isEqualTo(ledger.balanceOf(UUID.fromString(euros)));
        assertThat(statement.lines()).hasSize(3);
    }

    @Test
    void theCustomerDownloadsItAsAPdf() throws Exception {
        mvc.perform(get("/api/v1/wallets/{id}/statements", euros).with(TestUsers.signedInAs(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].month").value(thisMonth().toString()))
                .andExpect(openApi().isValid(CONTRACT));

        byte[] pdf = mvc.perform(get("/api/v1/wallets/{id}/statements/{month}", euros, thisMonth().toString())
                        .with(TestUsers.signedInAs(customer)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text)
                    .contains("Ledgerline statement")
                    .contains("Stella Statement")
                    .contains("Closing balance")
                    .contains("EUR 50.25")
                    .contains("Sent to Fred Friend");
        }
    }

    @Test
    void someoneElsesWalletHasNoStatements() throws Exception {
        mvc.perform(get("/api/v1/wallets/{id}/statements/{month}", euros, thisMonth().toString())
                        .with(TestUsers.signedInAs(TestUsers.JANA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void theMonthlyRunArchivesStatementsAndTellsTheCustomer() {
        OwnedWallet wallet = wallets.requireOwned(customer.id(), UUID.fromString(euros));

        assertThat(job.archive(thisMonth())).isPositive();

        assertThat(documents.get(MonthlyStatementJob.key(wallet, thisMonth()))).isPresent();
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(jdbc.sql(
                                "SELECT COUNT(*) FROM notifications WHERE user_id = :user AND kind = 'STATEMENT_READY'")
                        .param("user", customer.id())
                        .query(Integer.class)
                        .single())
                .isEqualTo(1));
    }

    private static YearMonth thisMonth() {
        return YearMonth.now(ZoneOffset.UTC);
    }
}
