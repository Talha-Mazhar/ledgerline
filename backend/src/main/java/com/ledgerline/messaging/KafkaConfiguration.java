package com.ledgerline.messaging;

import java.lang.reflect.RecordComponent;
import java.util.List;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Kafka setup.
 *
 * <ul>
 *   <li>Every message is JSON text. Events are turned into JSON here, before Spring Modulith hands
 *       them to Kafka, so producers and consumers only ever deal with strings, and a consumer never
 *       needs the producer's Java classes.
 *   <li>Each topic has 3 partitions. Messages of one payment (or, for events about a person, one
 *       user) share a key, so they stay in order.
 *       Each partition is copied to {@code ledgerline.kafka.replication-factor} brokers: 1 on a
 *       laptop, 3 on a production cluster.
 *   <li>A consumer that fails retries after 1, 2 and 4 seconds on retry topics, without blocking
 *       the messages behind it; after the fourth failure the message goes to a dead-letter topic
 *       ({@code <topic>-dlt}) for someone to look at.
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
class KafkaConfiguration {

    // Spring Modulith asks for this while it sets up event listeners, before ordinary beans can be
    // created. So the method is static, and the JSON mapper is looked up only when the first event
    // is sent.
    @Bean
    static EventExternalizationConfiguration eventExternalizationConfiguration(ObjectProvider<JsonMapper> json) {
        return EventExternalizationConfiguration.externalizing()
                .select(EventExternalizationConfiguration.annotatedAsExternalized())
                // The key is taken from the event before it becomes JSON text.
                .routeKey(Record.class, KafkaConfiguration::keyOf)
                .mapping(Object.class, event -> json.getObject().writeValueAsString(event))
                .build();
    }

    /** The fields an event's key comes from, in order of preference. */
    private static final List<String> KEY_FIELDS = List.of("paymentId", "userId");

    /**
     * An event's Kafka key: its payment id, or else its user id. Messages with the same key go to
     * the same partition, so all of one payment's events, or one user's, are read in order.
     */
    static String keyOf(Record event) {
        for (String field : KEY_FIELDS) {
            for (RecordComponent component : event.getClass().getRecordComponents()) {
                if (component.getName().equals(field)) {
                    try {
                        return String.valueOf(component.getAccessor().invoke(event));
                    } catch (ReflectiveOperationException unreadable) {
                        throw new IllegalStateException("Can't read the key of " + event, unreadable);
                    }
                }
            }
        }
        throw new IllegalArgumentException(
                event.getClass().getSimpleName() + " has no paymentId or userId to use as its Kafka key");
    }

    @Bean
    KafkaAdmin.NewTopics ledgerlineTopics(@Value("${ledgerline.kafka.replication-factor:1}") short replicationFactor) {
        return new KafkaAdmin.NewTopics(Topics.ALL.stream()
                .map(name -> TopicBuilder.name(name)
                        .partitions(3)
                        .replicas(replicationFactor)
                        .build())
                .toArray(NewTopic[]::new));
    }

    @Bean
    RetryTopicConfiguration retryTopics(
            KafkaOperations<?, ?> kafka, @Value("${ledgerline.kafka.replication-factor:1}") short replicationFactor) {
        return RetryTopicConfigurationBuilder.newInstance()
                .exponentialBackoff(1_000, 2, 4_000)
                .maxAttempts(4)
                .dltSuffix("-dlt")
                .autoCreateTopicsWith(3, replicationFactor)
                .includeTopics(Topics.ALL)
                .create(kafka);
    }
}
