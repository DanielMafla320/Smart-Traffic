package com.smarttraffic.queryclient.dto;

import com.smarttraffic.queryclient.domain.CongestionStatus;

public record ZoneStatus(String zone, CongestionStatus status) {
}
