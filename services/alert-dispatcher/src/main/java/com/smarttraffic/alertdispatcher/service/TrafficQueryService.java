package com.smarttraffic.alertdispatcher.service;

import com.smarttraffic.alertdispatcher.domain.CongestionStatus;
import com.smarttraffic.alertdispatcher.domain.QueryResultStatus;
import com.smarttraffic.alertdispatcher.dto.QueryRequestRecord;
import com.smarttraffic.alertdispatcher.dto.RouteQueryResponse;
import com.smarttraffic.alertdispatcher.dto.ZoneStatus;
import com.smarttraffic.alertdispatcher.exception.ZoneNotFoundException;
import com.smarttraffic.alertdispatcher.state.TrafficStateSnapshot;
import com.smarttraffic.alertdispatcher.state.TrafficStateStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class TrafficQueryService {

    private static final Pattern USER_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private final TrafficStateStore stateStore;

    public TrafficQueryService(TrafficStateStore stateStore) {
        this.stateStore = stateStore;
    }

    public RouteQueryResponse resolve(QueryRequestRecord request, String queryId) {
        if (request == null || !USER_ID_PATTERN.matcher(request.userId()).matches()) {
            return response(queryId, request, QueryResultStatus.INVALID_REQUEST, List.of(), null,
                    null, "Query must include a valid user_id and trayecto.");
        }

        TrafficStateSnapshot snapshot = stateStore.snapshotState();
        if (!snapshot.initialized()) {
            return response(queryId, request, QueryResultStatus.SNAPSHOT_NOT_READY,
                    List.of(), null, null, "Traffic snapshot has not been received yet.");
        }

        try {
            List<ZoneStatus> zones = request.trayecto().stream()
                    .map(zone -> statusOf(snapshot, zone))
                    .toList();
            CongestionStatus routeStatus = zones.stream()
                    .map(ZoneStatus::status)
                    .max(Comparator.comparingInt(CongestionStatus::ordinal))
                    .orElseThrow();
            return response(queryId, request, QueryResultStatus.OK, zones, routeStatus,
                    snapshot.timestamp(), "Query resolved from the local traffic snapshot.");
        } catch (ZoneNotFoundException exception) {
            return response(queryId, request, QueryResultStatus.ZONE_NOT_FOUND,
                    List.of(), null, snapshot.timestamp(),
                    "No existe la zona '" + exception.zoneName() + "' en la réplica.");
        }
    }

    private RouteQueryResponse response(
            String queryId,
            QueryRequestRecord request,
            QueryResultStatus result,
            List<ZoneStatus> zones,
            CongestionStatus routeStatus,
            Instant snapshotTimestamp,
            String message) {
        return new RouteQueryResponse(
                queryId,
                request == null ? null : request.userId(),
                request == null ? List.of() : request.trayecto(),
                result,
                zones,
                routeStatus,
                snapshotTimestamp,
                Instant.now(),
                message);
    }

    private ZoneStatus statusOf(TrafficStateSnapshot snapshot, String zone) {
        return snapshot.zones().stream()
                .filter(zoneStatus -> zoneStatus.zone().equals(zone))
                .findFirst()
                .orElseThrow(() -> new ZoneNotFoundException(zone));
    }
}
