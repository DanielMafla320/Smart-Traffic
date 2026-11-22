package com.smarttraffic.queryclient.dto;

import com.smarttraffic.queryclient.domain.CongestionStatus;
import com.smarttraffic.queryclient.domain.TrafficZone;

public record ZoneStatus(TrafficZone zone, CongestionStatus status) {
}
