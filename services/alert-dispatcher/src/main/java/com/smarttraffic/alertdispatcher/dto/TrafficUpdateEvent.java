package com.smarttraffic.alertdispatcher.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public record TrafficUpdateEvent(
        @JsonProperty("timestamp") Instant timestamp,
        @JsonProperty("zones") List<TrafficUpdateZone> zones) {
}
