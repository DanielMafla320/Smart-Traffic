package com.smarttraffic.alertdispatcher.dto;

import com.smarttraffic.alertdispatcher.domain.CongestionStatus;
import com.smarttraffic.alertdispatcher.domain.QueryResultStatus;
import com.smarttraffic.alertdispatcher.domain.QueryType;

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
