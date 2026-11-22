package com.smarttraffic.queryclient.dto;

import com.smarttraffic.queryclient.domain.QueryType;
import com.smarttraffic.queryclient.domain.TrafficZone;

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
