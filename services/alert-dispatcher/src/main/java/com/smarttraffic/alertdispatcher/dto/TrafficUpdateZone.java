package com.smarttraffic.alertdispatcher.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TrafficUpdateZone(
        @JsonProperty("zone_id") String zoneId,
        @JsonProperty("average_vehicles") Double averageVehicles,
        @JsonProperty("traffic_status") String trafficStatus) {
}
