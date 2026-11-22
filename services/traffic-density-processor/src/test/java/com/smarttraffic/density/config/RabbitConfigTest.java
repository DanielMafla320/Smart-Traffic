package com.smarttraffic.density.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smarttraffic.density.domain.TrafficStatus;
import com.smarttraffic.density.messaging.SystemStatusMessage;
import com.smarttraffic.density.messaging.ZoneStatusMessage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

class RabbitConfigTest {

    private final RabbitConfig config = new RabbitConfig();
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    void declaraUnExchangeFanoutDurableSinAutoDelete() {
        FanoutExchange exchange = config.trafficUpdatesExchange(
                new RabbitMessagingProperties("traffic_updates", 5000));

        assertThat(exchange.getName()).isEqualTo("traffic_updates");
        assertThat(exchange.getType()).isEqualTo("fanout");
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
    }

    @Test
    void serializaElContratoSinCabeceraDeTipoJava() {
        MessageConverter converter = config.rabbitMessageConverter(objectMapper);
        SystemStatusMessage payload = new SystemStatusMessage(
                Instant.parse("2026-11-12T16:00:05Z"),
                List.of(new ZoneStatusMessage("Zona A", 45.0, TrafficStatus.MODERADA)));

        Message message = converter.toMessage(payload, new MessageProperties());

        assertThat(message.getMessageProperties().getHeaders()).doesNotContainKey("__TypeId__");
        assertThat(new String(message.getBody(), StandardCharsets.UTF_8))
                .contains("\"timestamp\":\"2026-11-12T16:00:05Z\"")
                .contains("\"traffic_status\":\"MODERADA\"")
                .contains("\"zone_id\":\"Zona A\"");
    }
}
