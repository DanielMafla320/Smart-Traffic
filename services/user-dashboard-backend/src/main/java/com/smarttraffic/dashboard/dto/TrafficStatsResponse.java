package com.smarttraffic.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Resumen general del tráfico para el frontend.
 */
public record TrafficStatsResponse(
        @JsonProperty("total_vehicles") double totalVehicles,
        @JsonProperty("zones_count") int zonesCount,
        @JsonProperty("congested_zones_count") int congestedZonesCount,
        @JsonProperty("congested_zones") List<String> congestedZones,
        @JsonProperty("zones_by_status") Map<String, Long> zonesByStatus,
        @JsonProperty("last_updated") Instant lastUpdated,
        @JsonProperty("waiting_for_data") boolean waitingForData) {
}
