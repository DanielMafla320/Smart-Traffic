package com.smarttraffic.queryclient.dto;

import com.smarttraffic.queryclient.domain.QueryType;
import com.smarttraffic.queryclient.domain.CongestionStatus;
import com.smarttraffic.queryclient.domain.QueryResultStatus;

import java.time.Instant;
import java.util.List;

public record RouteQueryResponse(
        String queryId,
        String clientId,
        QueryType queryType,
        QueryResultStatus result,
        List<ZoneStatus> zones,
        CongestionStatus routeStatus,
        Instant snapshotTimestamp,
        Instant respondedAt,
        String message) {
}
