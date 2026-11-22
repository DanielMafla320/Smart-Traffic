package com.smarttraffic.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Estado de una zona tal como lo recibe el frontend.
 */
public record ZoneStatusResponse(
        @JsonProperty("zone_id") String zoneId,
        @JsonProperty("average_vehicles") double averageVehicles,
        @JsonProperty("traffic_status") String trafficStatus,
        @JsonProperty("last_updated") Instant lastUpdated) {
}
