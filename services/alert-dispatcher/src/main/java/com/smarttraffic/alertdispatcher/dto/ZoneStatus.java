package com.smarttraffic.alertdispatcher.dto;

import com.smarttraffic.alertdispatcher.domain.CongestionStatus;

public record ZoneStatus(String zone, CongestionStatus status) {
}
