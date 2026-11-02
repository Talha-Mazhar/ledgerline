package com.ledgerline.messaging;

import org.apache.kafka.clients.admin.NewTopic;
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
 *   <li>Each topic has 3 partitions. Messages of one payment share a key, so they stay in order.
 *       Each partition is copied to {@code ledgerline.kafka.replication-factor} brokers: 1 on a
 *       laptop, 3 on a production cluster.
 *   <li>A consumer that fails retries after 1, 2 and 4 seconds on retry topics, without blocking
 *       the messages behind it; after the fourth failure the message goes to a dead-letter topic
 *       ({@code <topic>-dlt}) for someone to look at.
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
class KafkaConfiguration {

    // Static: Spring Modulith asks for this while it sets up event listeners, before ordinary beans
    // can be created.
    @Bean
    static EventExternalizationConfiguration eventExternalizationConfiguration(JsonMapper json) {
        return EventExternalizationConfiguration.externalizing()
                .select(EventExternalizationConfiguration.annotatedAsExternalized())
                .mapping(Object.class, json::writeValueAsString)
                .build();
    }

    @Bean
    KafkaAdmin.NewTopics ledgerlineTopics(@Value("${ledgerline.kafka.replication-factor:1}") short replicationFactor) {
        return new KafkaAdmin.NewTopics(Topics.ALL.stream()
                .map(name -> TopicBuilder.name(name).partitions(3).replicas(replicationFactor).build())
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
