package com.ledgerline;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The services the application needs, in containers: PostgreSQL, Kafka, Redis and RustFS (S3).
 * {@code @ServiceConnection} hands PostgreSQL's, Kafka's and Redis's addresses to Spring Boot;
 * RustFS's goes in through a property. Images match {@code infra/compose.yaml}. Spring reuses one application
 * context (and so one set of containers) across all test classes with the same configuration.
 *
 * <p>Public so that tests in module packages (e.g. {@code com.ledgerline.wallets}) can import it.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:18-alpine");
    static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("apache/kafka-native:4.1.0");
    static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:8-alpine");
    static final DockerImageName STORAGE_IMAGE = DockerImageName.parse("rustfs/rustfs:1.0.4");

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        return new KafkaContainer(KAFKA_IMAGE);
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(REDIS_IMAGE).withExposedPorts(6379);
    }

    @Bean
    GenericContainer<?> storageContainer() {
        return new GenericContainer<>(STORAGE_IMAGE)
                .withCommand("/data")
                .withEnv("RUSTFS_ACCESS_KEY", "ledgerline")
                .withEnv("RUSTFS_SECRET_KEY", "ledgerline-secret")
                .withExposedPorts(9000)
                .waitingFor(Wait.forHttp("/health").forPort(9000));
    }

    /** Lets a test cut the producer off from Kafka; see {@link KafkaSwitch}. */
    @Bean
    KafkaSwitch kafkaSwitch(KafkaTemplate<?, ?> kafkaTemplate) {
        return new KafkaSwitch(kafkaTemplate);
    }

    /** The application sends through the switch, so a test can make Kafka unreachable. */
    @Bean
    @Primary
    KafkaOperations<?, ?> switchableKafkaOperations(KafkaSwitch kafkaSwitch) {
        return kafkaSwitch.operations();
    }

    @Bean
    DynamicPropertyRegistrar storageProperties(GenericContainer<?> storageContainer) {
        return registry -> registry.add(
                "ledgerline.documents.endpoint",
                () -> "http://" + storageContainer.getHost() + ":" + storageContainer.getMappedPort(9000));
    }
}
