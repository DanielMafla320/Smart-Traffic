package com.smarttraffic.density.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.Instant;
import java.util.List;

/**
 * Contrato del estado completo publicado en el exchange fanout {@code traffic_updates}.
 */
@JsonPropertyOrder({"timestamp", "zones"})
public record SystemStatusMessage(
        @JsonProperty("timestamp") Instant timestamp,
        @JsonProperty("zones") List<ZoneStatusMessage> zones) {

    public SystemStatusMessage {
        zones = List.copyOf(zones);
    }
}
