package com.ledgerline.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ledgerline.ApiScenario;
import com.ledgerline.KafkaSwitch;
import com.ledgerline.TestcontainersConfiguration;
import com.ledgerline.identity.SignedInUser;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The transactional outbox: Kafka is unreachable while a transfer is made. The transfer still
 * completes, its event waits in the {@code event_publication} table, and once Kafka is back and
 * the event is resubmitted, the recipient is notified. No event is lost.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class KafkaOutageTests {

    private final ApiScenario scenario;
    private final KafkaSwitch kafka;
    private final IncompleteEventPublications outbox;
    private final JdbcClient jdbc;

    KafkaOutageTests(
            @Autowired MockMvc mvc,
            @Autowired KafkaSwitch kafka,
            @Autowired IncompleteEventPublications outbox,
            @Autowired JdbcClient jdbc) {
        this.scenario = new ApiScenario(mvc);
        this.kafka = kafka;
        this.outbox = outbox;
        this.jdbc = jdbc;
    }

    @AfterEach
    void kafkaComesBack() {
        kafka.up();
    }

    @Test
    void aTransferMadeWhileKafkaIsDownIsAnnouncedWhenItComesBack() throws Exception {
        SignedInUser sender = scenario.signUp("Olga Outage");
        SignedInUser recipient = scenario.signUp("Rudi Recipient");
        String sendersEuros = scenario.openWallet(sender, "EUR");
        scenario.openWallet(recipient, "EUR");
        scenario.deposit(sender, sendersEuros, "100");
        // The top-up's event goes to Kafka after the request returns; let it arrive first.
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(waitingTransferEvents(sender)).isZero());

        kafka.down();
        scenario.send(sender, sendersEuros, recipient.email(), "30")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(waitingTransferEvents(sender))
                .as("the event is stored, not lost")
                .isEqualTo(1));
        assertThat(notificationsOf(recipient, "MONEY_RECEIVED")).isZero();

        kafka.up();
        outbox.resubmitIncompletePublications(publication -> true);

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(notificationsOf(recipient, "MONEY_RECEIVED")).isEqualTo(1));
        assertThat(waitingTransferEvents(sender)).isZero();
    }

    private int waitingTransferEvents(SignedInUser sender) {
        return jdbc.sql("""
                        SELECT COUNT(*) FROM event_publication
                        WHERE completion_date IS NULL
                          AND event_type LIKE '%PaymentCompleted'
                          AND serialized_event LIKE :payer
                        """)
                .param("payer", "%" + sender.id() + "%")
                .query(Integer.class)
                .single();
    }

    private int notificationsOf(SignedInUser user, String kind) {
        return jdbc.sql("SELECT COUNT(*) FROM notifications WHERE user_id = :user AND kind = :kind")
                .param("user", user.id())
                .param("kind", kind)
                .query(Integer.class)
                .single();
    }
}
