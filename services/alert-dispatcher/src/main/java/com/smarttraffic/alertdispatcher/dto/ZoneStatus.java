package com.smarttraffic.alertdispatcher.dto;

import com.smarttraffic.alertdispatcher.domain.CongestionStatus;
import com.smarttraffic.alertdispatcher.domain.TrafficZone;

public record ZoneStatus(TrafficZone zone, CongestionStatus status) {
}
