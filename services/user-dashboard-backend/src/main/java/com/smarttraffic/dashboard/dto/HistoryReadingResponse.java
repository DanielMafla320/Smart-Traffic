package com.smarttraffic.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * Una lectura del historial guardada en PostgreSQL.
 */
public record HistoryReadingResponse(
        @JsonProperty("event_id") UUID eventId,
        @JsonProperty("zone_id") String zoneId,
        @JsonProperty("vehicle_count") int vehicleCount,
        @JsonProperty("reading_timestamp") Instant readingTimestamp,
        @JsonProperty("created_at") Instant createdAt) {
}
