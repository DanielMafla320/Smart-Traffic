package com.smarttraffic.alertdispatcher.dto;

import com.smarttraffic.alertdispatcher.domain.QueryType;
import com.smarttraffic.alertdispatcher.domain.TrafficZone;

import java.time.Instant;

public record RouteQueryRequest(
        String queryId,
        String clientId,
        QueryType type,
        TrafficZone zone,
        TrafficZone origin,
        TrafficZone destination,
        Instant requestedAt) {
}
