package com.smarttraffic.density.domain;

import java.time.Instant;
import java.util.List;

/**
 * Fotografía inmutable del estado de una zona en un instante.
 */
public record ZoneSnapshot(
        String zoneId,
        List<Integer> recentReadings,
        double averageVehicles,
        TrafficStatus trafficStatus,
        Instant lastUpdated) {

    public ZoneSnapshot {
        recentReadings = List.copyOf(recentReadings);
    }
}
