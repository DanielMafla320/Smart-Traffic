package com.smarttraffic.alertdispatcher.state;

import com.smarttraffic.alertdispatcher.dto.ZoneStatus;

import java.time.Instant;
import java.util.List;

public record TrafficStateSnapshot(boolean initialized, Instant timestamp, List<ZoneStatus> zones) {

    public TrafficStateSnapshot {
        zones = List.copyOf(zones);
    }
}
