package com.smarttraffic.queryclient.dto;

import com.smarttraffic.queryclient.domain.CongestionStatus;
import com.smarttraffic.queryclient.domain.QueryResultStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public record RouteQueryResponse(
        String queryId,
        @JsonProperty("user_id") String userId,
        List<String> trayecto,
        QueryResultStatus result,
        List<ZoneStatus> zones,
        CongestionStatus routeStatus,
        Instant snapshotTimestamp,
        Instant respondedAt,
        String message) {

    public RouteQueryResponse {
        trayecto = List.copyOf(trayecto);
        zones = List.copyOf(zones);
    }
}
