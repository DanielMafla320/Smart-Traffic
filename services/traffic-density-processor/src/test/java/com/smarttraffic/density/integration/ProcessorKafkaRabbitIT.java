package com.smarttraffic.density.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.density.TrafficDensityProcessorApplication;
import com.smarttraffic.density.domain.TrafficReadingEvent;
import com.smarttraffic.density.domain.TrafficStatus;
import com.smarttraffic.density.domain.ZoneSnapshot;
import com.smarttraffic.density.service.ZoneStateStore;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Prueba de integración real del procesador contra Kafka y RabbitMQ de
 * {@code infrastructure/docker-compose.yml}.
 *
 * <p>Envía eventos controlados a un topic temporal, espera a que el procesador
 * los consuma y verifica los mensajes JSON publicados en una cola temporal
 * vinculada al exchange fanout {@code traffic_updates}.
 *
 * <p>Requiere Kafka en {@code localhost:9092} y RabbitMQ en
 * {@code localhost:5672}. Se ejecuta con {@code mvn -Pintegration-tests verify}.
 */
@SpringBootTest(
        classes = TrafficDensityProcessorApplication.class,
        properties = {
                "spring.kafka.bootstrap-servers=localhost:9092",
                "spring.kafka.consumer.group-id=traffic-density-processor",
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "spring.kafka.consumer.enable-auto-commit=false",
                "spring.kafka.listener.concurrency=1",
                "spring.kafka.admin.operation-timeout=10s",
                "smarttraffic.density.strategy=simple-average",
                "smarttraffic.density.window-size=10",
                "smarttraffic.density.moderada-threshold=30.0",
                "smarttraffic.density.congestionada-threshold=60.0",
                "smarttraffic.rabbit.exchange=traffic_updates",
                "smarttraffic.rabbit.publish-interval-ms=1000",
                "spring.rabbitmq.host=localhost",
                "spring.rabbitmq.port=5672",
                "spring.rabbitmq.username=smarttraffic",
                "spring.rabbitmq.password=smarttraffic_dev",
                "spring.rabbitmq.virtual-host=/",
                "spring.rabbitmq.connection-timeout=5s"
        })
@Import(ProcessorKafkaRabbitIT.TestConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProcessorKafkaRabbitIT {

    private static final String TOPIC =
            "raw_traffic_data_it_" + UUID.randomUUID().toString().replace("-", "");
    private static final String QUEUE =
            "traffic-updates-it-" + UUID.randomUUID().toString().replace("-", "");
    private static final String EXCHANGE = "traffic_updates";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ZoneStateStore zoneStateStore;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void topicProperty(DynamicPropertyRegistry registry) {
        registry.add("smarttraffic.kafka.topic", () -> TOPIC);
    }

    @BeforeEach
    void declareTemporaryQueue() {
        FanoutExchange exchange = new FanoutExchange(EXCHANGE, true, false);
        amqpAdmin.declareExchange(exchange);

        // Sin autoDelete: RabbitTemplate.receive cancela su consumidor tras el
        // timeout y RabbitMQ eliminaría la cola antes de terminar la prueba.
        Queue queue = QueueBuilder.nonDurable(QUEUE).build();
        amqpAdmin.declareQueue(queue);

        Binding binding = BindingBuilder.bind(queue).to(exchange);
        amqpAdmin.declareBinding(binding);
    }

    @AfterEach
    void deleteTemporaryQueue() {
        amqpAdmin.deleteQueue(QUEUE);
    }

    @Test
    @Order(1)
    void publicaUnaListaVaciaAntesDeLaPrimeraLectura() {
        JsonNode message = awaitMessage(json -> json.get("zones").isEmpty(), 10_000);

        assertThat(message.get("timestamp").asText()).isNotBlank();
        assertThat(Instant.parse(message.get("timestamp").asText())).isNotNull();
        assertThat(message.get("zones")).isEmpty();
    }

    @Test
    @Order(2)
    void procesaEventosControladosYPublicaTodasLasTransiciones() throws Exception {
        String zone = "Zona IT";

        // Cada evento se envía solo después de observar en RabbitMQ el estado
        // publicado anterior. Así se verifican las transiciones reales y no solo
        // el estado final del procesador.
        sendAndAwait(zone, 10, TrafficStatus.DESPEJADA, 10.0);
        sendAndAwait(zone, 60, TrafficStatus.MODERADA, 35.0);
        sendAndAwait(zone, 110, TrafficStatus.CONGESTIONADA, 60.0);
        sendAndAwait(zone, 0, TrafficStatus.MODERADA, 45.0);
        sendAndAwait(zone, 0, TrafficStatus.MODERADA, 36.0);
        sendAndAwait(zone, 0, TrafficStatus.MODERADA, 30.0);
        sendAndAwait(zone, 0, TrafficStatus.DESPEJADA, 180.0 / 7.0);
        sendAndAwait(zone, 0, TrafficStatus.DESPEJADA, 22.5);
        sendAndAwait(zone, 0, TrafficStatus.DESPEJADA, 20.0);
        sendAndAwait(zone, 0, TrafficStatus.DESPEJADA, 18.0);
        sendAndAwait(zone, 0, TrafficStatus.DESPEJADA, 17.0);
        sendAndAwait(zone, 0, TrafficStatus.DESPEJADA, 11.0);

        ZoneSnapshot snapshot = awaitSnapshot(
                zone, value -> value.recentReadings().size() == 10, 10_000);
        assertThat(snapshot.recentReadings())
                .containsExactly(110, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        assertThat(snapshot.averageVehicles()).isEqualTo(11.0);
        assertThat(snapshot.trafficStatus()).isEqualTo(TrafficStatus.DESPEJADA);
    }

    @Test
    @Order(3)
    void publicaElEstadoCompletoDeTodasLasZonasConocidas() throws Exception {
        send("Zona IT A", 10);
        send("Zona IT B", 80);

        JsonNode message = awaitMessage(json ->
                findZone(json, "Zona IT A") != null && findZone(json, "Zona IT B") != null, 15_000);

        assertThat(findZone(message, "Zona IT A").get("traffic_status").asText())
                .isEqualTo("DESPEJADA");
        assertThat(findZone(message, "Zona IT B").get("traffic_status").asText())
                .isEqualTo("CONGESTIONADA");
        assertThat(findZone(message, "Zona IT A").get("average_vehicles").asDouble())
                .isEqualTo(10.0);
        assertThat(findZone(message, "Zona IT B").get("average_vehicles").asDouble())
                .isEqualTo(80.0);
    }

    @Test
    @Order(4)
    void descartaUnJsonInvalidoYContinuaProcesando() throws Exception {
        kafkaTemplate.send(TOPIC, "Zona IT INV", "esto-no-es-json")
                .get(10, TimeUnit.SECONDS);

        send("Zona IT INV", 70);

        awaitZone("Zona IT INV", TrafficStatus.CONGESTIONADA, 70.0, 15_000);
    }

    private void send(String zoneId, int vehicleCount) throws Exception {
        TrafficReadingEvent event = new TrafficReadingEvent(
                UUID.randomUUID(), zoneId, vehicleCount, Instant.now());
        kafkaTemplate.send(TOPIC, zoneId, objectMapper.writeValueAsString(event))
                .get(10, TimeUnit.SECONDS);
    }

    private void sendAndAwait(
            String zoneId, int vehicleCount, TrafficStatus status, double average) throws Exception {
        send(zoneId, vehicleCount);
        awaitZone(zoneId, status, average, 15_000);
    }

    private JsonNode awaitZone(
            String zoneId, TrafficStatus status, double average, long timeoutMillis) {
        return awaitMessage(json -> {
            JsonNode zone = findZone(json, zoneId);
            return zone != null
                    && status.name().equals(zone.get("traffic_status").asText())
                    && Math.abs(zone.get("average_vehicles").asDouble() - average) < 0.001;
        }, timeoutMillis);
    }

    private JsonNode awaitMessage(Predicate<JsonNode> predicate, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        JsonNode lastMessage = null;

        while (System.currentTimeMillis() < deadline) {
            Message message = rabbitTemplate.receive(QUEUE, 500);
            if (message == null) {
                continue;
            }
            assertThat(message.getMessageProperties().getHeaders())
                    .as("El contrato RabbitMQ no debe depender de clases Java")
                    .doesNotContainKey("__TypeId__");
            try {
                lastMessage = objectMapper.readTree(
                        new String(message.getBody(), StandardCharsets.UTF_8));
                if (predicate.test(lastMessage)) {
                    return lastMessage;
                }
            } catch (Exception exception) {
                throw new AssertionError("Mensaje RabbitMQ con JSON inválido", exception);
            }
        }

        throw new AssertionError("No se recibió el mensaje esperado en " + timeoutMillis
                + " ms. Último mensaje: " + lastMessage);
    }

    private ZoneSnapshot awaitSnapshot(
            String zoneId, Predicate<ZoneSnapshot> predicate, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        ZoneSnapshot lastSnapshot = null;

        while (System.currentTimeMillis() < deadline) {
            lastSnapshot = zoneStateStore.findSnapshot(zoneId).orElse(null);
            if (lastSnapshot != null && predicate.test(lastSnapshot)) {
                return lastSnapshot;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Espera interrumpida", exception);
            }
        }

        throw new AssertionError("No se alcanzó el snapshot esperado para " + zoneId
                + ". Último snapshot: " + lastSnapshot);
    }

    private JsonNode findZone(JsonNode message, String zoneId) {
        for (JsonNode zone : message.get("zones")) {
            if (zoneId.equals(zone.get("zone_id").asText())) {
                return zone;
            }
        }
        return null;
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

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {

        @Bean
        NewTopic rawTrafficDataTestTopic(
                @Value("${smarttraffic.kafka.topic}") String topic) {
            return TopicBuilder.name(topic)
                    .partitions(1)
                    .replicas(1)
                    .build();
        }

        @Bean
        KafkaTemplate<String, String> testKafkaTemplate(
                KafkaProperties kafkaProperties,
                ObjectProvider<SslBundles> sslBundles) {
            Map<String, Object> properties =
                    kafkaProperties.buildProducerProperties(sslBundles.getIfAvailable());
            return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                    properties,
                    new StringSerializer(),
                    new StringSerializer()));
        }
    }
}
