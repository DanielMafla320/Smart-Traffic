package com.smarttraffic.archiver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje JSON que llega del topic Kafka {@code raw_traffic_data}
 * (publicado por traffic-sensor-simulator).
 *
 * <pre>
 * {"event_id":"f33d...","zone_id":"Zona A","vehicle_count":61,"timestamp":"2026-11-13T04:21:25Z"}
 * </pre>
 *
 * <p>Es un DTO: solo transporta datos entre servicios. Los nombres JSON se
 * fijan con {@code @JsonProperty} para que el contrato no dependa de la
 * configuración de Jackson.
 */
public record TrafficReadingEvent(
        @JsonProperty("event_id") @NotNull UUID eventId,
        @JsonProperty("zone_id") @NotBlank String zoneId,
        @JsonProperty("vehicle_count") @NotNull @PositiveOrZero Integer vehicleCount,
        @JsonProperty("timestamp") @NotNull Instant timestamp) {
}
