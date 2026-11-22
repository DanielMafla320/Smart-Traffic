package com.smarttraffic.dashboard.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Verifica que el DTO entiende exactamente el JSON que publica
 * traffic-density-processor en el exchange traffic_updates.
 */
class TrafficUpdateMessageContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void deserializaElSnapshotRealDelProcesador() throws Exception {
        String json = """
                {"timestamp":"2026-11-13T04:21:52.379261700Z",
                 "zones":[{"zone_id":"Zona A","average_vehicles":52.87,"traffic_status":"MODERADA"},
                          {"zone_id":"Zona B","average_vehicles":80.0,"traffic_status":"CONGESTIONADA"}]}
                """;

        TrafficUpdateMessage message = objectMapper.readValue(json, TrafficUpdateMessage.class);

        assertThat(message.timestamp()).isEqualTo(Instant.parse("2026-11-13T04:21:52.379261700Z"));
        assertThat(message.zones()).hasSize(2);
        assertThat(message.zones().get(0).zoneId()).isEqualTo("Zona A");
        assertThat(message.zones().get(0).averageVehicles()).isEqualTo(52.87);
        assertThat(message.zones().get(1).trafficStatus()).isEqualTo("CONGESTIONADA");
    }

    @Test
    void deserializaElPrimerSnapshotSinZonas() throws Exception {
        TrafficUpdateMessage message = objectMapper.readValue(
                "{\"timestamp\":\"2026-11-13T04:21:47Z\",\"zones\":[]}", TrafficUpdateMessage.class);

        assertThat(message.zones()).isEmpty();
    }
}
