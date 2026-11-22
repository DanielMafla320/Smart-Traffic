package com.smarttraffic.alertdispatcher.service;

import com.smarttraffic.alertdispatcher.domain.CongestionStatus;
import com.smarttraffic.alertdispatcher.domain.QueryResultStatus;
import com.smarttraffic.alertdispatcher.domain.QueryType;
import com.smarttraffic.alertdispatcher.domain.TrafficZone;
import com.smarttraffic.alertdispatcher.dto.RouteQueryRequest;
import com.smarttraffic.alertdispatcher.dto.RouteQueryResponse;
import com.smarttraffic.alertdispatcher.dto.ZoneStatus;
import com.smarttraffic.alertdispatcher.state.TrafficStateSnapshot;
import com.smarttraffic.alertdispatcher.state.TrafficStateStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class TrafficQueryService {

    private static final Pattern CLIENT_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private final TrafficStateStore stateStore;

    public TrafficQueryService(TrafficStateStore stateStore) {
        this.stateStore = stateStore;
    }

    public RouteQueryResponse resolve(RouteQueryRequest request) {
        if (!isValid(request)) {
            return response(request, QueryResultStatus.INVALID_REQUEST, List.of(), null, null,
                    "Query must include a valid clientId, queryId, and zone or route.");
        }
        TrafficStateSnapshot snapshot = stateStore.snapshotState();
        if (!snapshot.initialized()) {
            return response(request, QueryResultStatus.SNAPSHOT_NOT_READY, List.of(), null, null,
                    "Traffic snapshot has not been received yet.");
        }

        List<ZoneStatus> zones;
        CongestionStatus routeStatus = null;
        if (request.type() == QueryType.ZONE) {
            CongestionStatus status = statusOf(snapshot, request.zone());
            if (status == null) {
                return snapshotNotReady(request, snapshot);
            }
            zones = List.of(new ZoneStatus(request.zone(), status));
            routeStatus = status;
        } else {
            CongestionStatus originStatus = statusOf(snapshot, request.origin());
            CongestionStatus destinationStatus = statusOf(snapshot, request.destination());
            if (originStatus == null || destinationStatus == null) {
                return snapshotNotReady(request, snapshot);
            }
            zones = List.of(
                    new ZoneStatus(request.origin(), originStatus),
                    new ZoneStatus(request.destination(), destinationStatus));
            routeStatus = zones.stream()
                    .map(ZoneStatus::status)
                    .max((left, right) -> Integer.compare(left.ordinal(), right.ordinal()))
                    .orElseThrow();
        }
        return response(request, QueryResultStatus.OK, zones, routeStatus,
                snapshot.timestamp(), "Query resolved from the local traffic snapshot.");
    }

    private boolean isValid(RouteQueryRequest request) {
        if (request == null || request.queryId() == null || request.queryId().isBlank()
                || request.clientId() == null
                || !CLIENT_ID_PATTERN.matcher(request.clientId()).matches()
                || request.type() == null) {
            return false;
        }
        return switch (request.type()) {
            case ZONE -> request.zone() != null;
            case ROUTE -> request.origin() != null && request.destination() != null;
        };
    }

    private RouteQueryResponse response(
            RouteQueryRequest request,
            QueryResultStatus result,
            List<ZoneStatus> zones,
            CongestionStatus routeStatus,
            Instant snapshotTimestamp,
            String message) {
        return new RouteQueryResponse(
                request == null ? null : request.queryId(),
                request == null ? null : request.clientId(),
                request == null ? null : request.type(),
                result,
                zones,
                routeStatus,
                snapshotTimestamp,
                Instant.now(),
                message);
    }

    private RouteQueryResponse snapshotNotReady(
            RouteQueryRequest request, TrafficStateSnapshot snapshot) {
        return response(request, QueryResultStatus.SNAPSHOT_NOT_READY, List.of(), null,
                snapshot.timestamp(), "Traffic snapshot does not contain all requested zones yet.");
    }

    private CongestionStatus statusOf(TrafficStateSnapshot snapshot, TrafficZone zone) {
        return snapshot.zones().stream()
                .filter(zoneStatus -> zoneStatus.zone() == zone)
                .map(ZoneStatus::status)
                .findFirst()
                .orElse(null);
    }
}
