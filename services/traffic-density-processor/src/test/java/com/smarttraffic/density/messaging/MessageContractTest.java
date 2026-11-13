package com.smarttraffic.density.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smarttraffic.density.domain.TrafficReadingEvent;
import com.smarttraffic.density.domain.TrafficStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MessageContractTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    void deserializaElContratoKafkaAcordado() throws Exception {
        String json = """
                {
                  "event_id": "6f1b8e4e-6f6a-4b52-9d3a-5c1f5d7d9f10",
                  "zone_id": "Zona A",
                  "vehicle_count": 45,
                  "timestamp": "2026-11-12T16:00:00Z"
                }
                """;

        TrafficReadingEvent event = objectMapper.readValue(json, TrafficReadingEvent.class);

        assertThat(event.eventId())
                .isEqualTo(UUID.fromString("6f1b8e4e-6f6a-4b52-9d3a-5c1f5d7d9f10"));
        assertThat(event.zoneId()).isEqualTo("Zona A");
        assertThat(event.vehicleCount()).isEqualTo(45);
        assertThat(event.timestamp()).isEqualTo(Instant.parse("2026-11-12T16:00:00Z"));
    }

    @Test
    void serializaElContratoRabbitAcordado() throws Exception {
        SystemStatusMessage message = new SystemStatusMessage(
                Instant.parse("2026-11-12T16:00:05Z"),
                List.of(new ZoneStatusMessage("Zona A", 45.0, TrafficStatus.MODERADA)));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(message));

        assertThat(json.fieldNames()).toIterable().containsExactly("timestamp", "zones");
        assertThat(json.get("timestamp").asText()).isEqualTo("2026-11-12T16:00:05Z");
        JsonNode zone = json.get("zones").get(0);
        assertThat(zone.fieldNames()).toIterable()
                .containsExactly("zone_id", "average_vehicles", "traffic_status");
        assertThat(zone.get("zone_id").asText()).isEqualTo("Zona A");
        assertThat(zone.get("average_vehicles").asDouble()).isEqualTo(45.0);
        assertThat(zone.get("traffic_status").asText()).isEqualTo("MODERADA");
    }
}
