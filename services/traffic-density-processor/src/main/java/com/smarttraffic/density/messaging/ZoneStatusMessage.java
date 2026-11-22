package com.smarttraffic.density.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.smarttraffic.density.domain.TrafficStatus;

/**
 * Estado de una zona dentro del mensaje RabbitMQ.
 */
@JsonPropertyOrder({"zone_id", "average_vehicles", "traffic_status"})
public record ZoneStatusMessage(
        @JsonProperty("zone_id") String zoneId,
        @JsonProperty("average_vehicles") double averageVehicles,
        @JsonProperty("traffic_status") TrafficStatus trafficStatus) {
}
