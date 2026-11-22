package com.smarttraffic.dashboard.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

/**
 * Mensaje del exchange fanout {@code traffic_updates} (publicado por
 * traffic-density-processor cada 5 s). El primero llega con {@code zones: []}.
 *
 * <pre>
 * {"timestamp":"2026-11-13T04:21:52Z",
 *  "zones":[{"zone_id":"Zona A","average_vehicles":52.87,"traffic_status":"MODERADA"}]}
 * </pre>
 */
public record TrafficUpdateMessage(
        @JsonProperty("timestamp") Instant timestamp,
        @JsonProperty("zones") List<ZoneUpdateMessage> zones) {

    /**
     * Estado de una zona dentro del mensaje.
     */
    public record ZoneUpdateMessage(
            @JsonProperty("zone_id") String zoneId,
            @JsonProperty("average_vehicles") Double averageVehicles,
            @JsonProperty("traffic_status") String trafficStatus) {
    }
}
