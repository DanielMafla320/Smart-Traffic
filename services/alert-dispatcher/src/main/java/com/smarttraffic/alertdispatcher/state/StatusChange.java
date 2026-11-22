package com.smarttraffic.alertdispatcher.state;

import com.smarttraffic.alertdispatcher.domain.CongestionStatus;

import java.time.Instant;

public record StatusChange(
        String zone,
        CongestionStatus previousStatus,
        CongestionStatus currentStatus,
        Instant timestamp) {
}
