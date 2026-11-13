package com.smarttraffic.sensor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.sensor.TrafficSensorSimulatorApplication;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Prueba de integración real del simulador contra el Kafka de
 * {@code infrastructure/docker-compose.yml}.
 *
 * <p>Requiere que Kafka esté saludable en {@code localhost:9092}. Se ejecuta con
 * {@code mvn -Pintegration-tests verify}; no forma parte de {@code mvn test}.
 */
@SpringBootTest(
        classes = TrafficSensorSimulatorApplication.class,
        properties = {
                "spring.kafka.bootstrap-servers=localhost:9092",
                "spring.kafka.admin.operation-timeout=10s",
                "spring.kafka.admin.close-timeout=5s",
                "smarttraffic.simulator.enabled=true",
                "smarttraffic.simulator.interval-ms=500",
                "smarttraffic.simulator.zones=Zona IT A,Zona IT B",
                "smarttraffic.simulator.vehicle-count-min=30",
                "smarttraffic.simulator.vehicle-count-max=30",
                "smarttraffic.kafka.partitions=3",
                "smarttraffic.kafka.replicas=1"
        })
class SimulatorKafkaIT {

    private static final String TOPIC =
            "raw_traffic_data_it_" + UUID.randomUUID().toString().replace("-", "");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DynamicPropertySource
    static void topicProperty(DynamicPropertyRegistry registry) {
        registry.add("smarttraffic.kafka.topic", () -> TOPIC);
    }

    @Test
    void elSimuladorPublicaEventosValidosConLaZonaComoClave() throws Exception {
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties())) {
            consumer.subscribe(List.of(TOPIC));

            Map<String, ConsumerRecord<String, String>> firstRecordByZone = new HashMap<>();
            long deadline = System.currentTimeMillis() + 20_000;
            while (firstRecordByZone.size() < 2 && System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    firstRecordByZone.putIfAbsent(record.key(), record);
                }
            }

            assertThat(firstRecordByZone)
                    .as("Kafka debe recibir al menos una lectura de cada zona configurada")
                    .containsKeys("Zona IT A", "Zona IT B");

            for (Map.Entry<String, ConsumerRecord<String, String>> entry : firstRecordByZone.entrySet()) {
                JsonNode json = objectMapper.readTree(entry.getValue().value());

                assertThat(entry.getValue().key()).isEqualTo(json.get("zone_id").asText());
                assertThat(json.fieldNames()).toIterable()
                        .containsExactly("event_id", "zone_id", "vehicle_count", "timestamp");
                assertThat(UUID.fromString(json.get("event_id").asText())).isNotNull();
                assertThat(json.get("zone_id").asText()).isEqualTo(entry.getKey());
                assertThat(json.get("vehicle_count").asInt()).isEqualTo(30);
                assertThat(Instant.parse(json.get("timestamp").asText())).isNotNull();
            }
        }
    }

    private Properties consumerProperties() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "simulator-it-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return properties;
    }

    @AfterAll
    static void deleteTestTopic() {
        try (AdminClient admin = AdminClient.create(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092"))) {
            admin.deleteTopics(List.of(TOPIC)).all().get();
        } catch (Exception exception) {
            System.err.println("No se pudo eliminar el topic de prueba " + TOPIC
                    + ": " + exception.getMessage());
        }
    }
}
