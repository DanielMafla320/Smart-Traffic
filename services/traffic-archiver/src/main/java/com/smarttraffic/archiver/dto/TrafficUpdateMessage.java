package com.smarttraffic.archiver.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

/**
 * Snapshot que publica traffic-density-processor en el exchange fanout
 * {@code traffic_updates} cada pocos segundos.
 *
 * <pre>
 * {"timestamp":"2026-11-13T04:21:25Z","zones":[
 *   {"zone_id":"Zona A","average_vehicles":61.5,"traffic_status":"CONGESTIONADA"}]}
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TrafficUpdateMessage(
        @JsonProperty("timestamp") Instant timestamp,
        @JsonProperty("zones") List<Zone> zones) {

    public TrafficUpdateMessage {
        zones = zones == null ? List.of() : List.copyOf(zones);
    }

    /** Estado de una zona dentro del snapshot. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Zone(
            @JsonProperty("zone_id") String zoneId,
            @JsonProperty("average_vehicles") Double averageVehicles,
            @JsonProperty("traffic_status") String trafficStatus) {
    }
}
