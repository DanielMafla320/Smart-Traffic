package com.smarttraffic.density.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento consumido desde {@code raw_traffic_data}. Replica el contrato del simulador
 * de sensores sin compartir código entre proyectos Maven independientes.
 */
@JsonPropertyOrder({"event_id", "zone_id", "vehicle_count", "timestamp"})
public record TrafficReadingEvent(
        @JsonProperty("event_id") @NotNull UUID eventId,
        @JsonProperty("zone_id") @NotBlank String zoneId,
        @JsonProperty("vehicle_count") @NotNull @PositiveOrZero Integer vehicleCount,
        @JsonProperty("timestamp") @NotNull Instant timestamp) {

    public TrafficReadingEvent {
        if (zoneId != null) {
            zoneId = zoneId.trim();
        }
    }
}
