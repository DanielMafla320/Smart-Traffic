package com.smarttraffic.archiver.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Verifica que el DTO entiende exactamente el JSON que publica
 * traffic-sensor-simulator en raw_traffic_data.
 */
class TrafficReadingEventContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void deserializaElJsonRealDelSimulador() throws Exception {
        String json = """
                {
                  "event_id": "f33df0e7-b180-4a75-9561-569ff24f62eb",
                  "zone_id": "Zona A",
                  "vehicle_count": 61,
                  "timestamp": "2026-11-13T04:21:25.765885200Z"
                }
                """;

        TrafficReadingEvent event = objectMapper.readValue(json, TrafficReadingEvent.class);

        assertThat(event.eventId()).isEqualTo(UUID.fromString("f33df0e7-b180-4a75-9561-569ff24f62eb"));
        assertThat(event.zoneId()).isEqualTo("Zona A");
        assertThat(event.vehicleCount()).isEqualTo(61);
        assertThat(event.timestamp()).isEqualTo(Instant.parse("2026-11-13T04:21:25.765885200Z"));
    }
}
