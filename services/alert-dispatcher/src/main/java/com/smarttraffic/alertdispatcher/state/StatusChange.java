package com.smarttraffic.alertdispatcher.state;

import com.smarttraffic.alertdispatcher.domain.CongestionStatus;
import com.smarttraffic.alertdispatcher.domain.TrafficZone;

import java.time.Instant;

public record StatusChange(
        TrafficZone zone,
        CongestionStatus previousStatus,
        CongestionStatus currentStatus,
        Instant timestamp) {
}
