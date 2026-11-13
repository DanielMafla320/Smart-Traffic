package com.smarttraffic.sensor.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.UUID;

/**
 * Contrato de una lectura de sensor publicada en el topic {@code raw_traffic_data}.
 *
 * <p>Los nombres JSON son explícitos para no depender de la estrategia de nombres
 * configurada en Jackson y mantener estable el contrato entre microservicios.
 */
@JsonPropertyOrder({"event_id", "zone_id", "vehicle_count", "timestamp"})
public record SensorReadingRecord(
        @JsonProperty("event_id") @NotNull UUID eventId,
        @JsonProperty("zone_id") @NotBlank String zoneId,
        @JsonProperty("vehicle_count") @NotNull @PositiveOrZero Integer vehicleCount,
        @JsonProperty("timestamp") @NotNull Instant timestamp) {

    public SensorReadingRecord {
        if (zoneId != null) {
            zoneId = zoneId.trim();
        }
    }
}
