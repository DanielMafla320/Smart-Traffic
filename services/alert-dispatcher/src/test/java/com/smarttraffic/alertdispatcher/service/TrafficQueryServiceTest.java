package com.smarttraffic.alertdispatcher.service;

import com.smarttraffic.alertdispatcher.domain.CongestionStatus;
import com.smarttraffic.alertdispatcher.domain.QueryResultStatus;
import com.smarttraffic.alertdispatcher.domain.QueryType;
import com.smarttraffic.alertdispatcher.domain.TrafficZone;
import com.smarttraffic.alertdispatcher.dto.RouteQueryRequest;
import com.smarttraffic.alertdispatcher.dto.TrafficUpdateEvent;
import com.smarttraffic.alertdispatcher.dto.TrafficUpdateZone;
import com.smarttraffic.alertdispatcher.state.TrafficStateStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrafficQueryServiceTest {

    @Test
    void resolvesRouteUsingTheWorstStatusOfItsEndpoints() {
        TrafficStateStore store = new TrafficStateStore();
        Instant snapshotTimestamp = Instant.parse("2026-11-22T12:00:00Z");
        store.applySnapshot(new TrafficUpdateEvent(
                snapshotTimestamp,
                List.of(
                        zone("Zona A", CongestionStatus.DESPEJADA),
                        zone("Zona B", CongestionStatus.MODERADA),
                        zone("Zona C", CongestionStatus.DESPEJADA),
                        zone("Zona D", CongestionStatus.DESPEJADA),
                        zone("Zona E", CongestionStatus.CONGESTIONADA))));

        var response = new TrafficQueryService(store).resolve(new RouteQueryRequest(
                "query-1", "client-1", QueryType.ROUTE, null,
                TrafficZone.A, TrafficZone.E, Instant.now()));

        assertEquals(QueryResultStatus.OK, response.result());
        assertEquals(CongestionStatus.CONGESTIONADA, response.routeStatus());
        assertEquals(snapshotTimestamp, response.snapshotTimestamp());
        assertEquals(2, response.zones().size());
    }

    @Test
    void reportsWhenTheInitialSnapshotHasNotArrived() {
        var response = new TrafficQueryService(new TrafficStateStore()).resolve(
                new RouteQueryRequest("query-2", "client-1", QueryType.ZONE,
                        TrafficZone.A, null, null, Instant.now()));

        assertEquals(QueryResultStatus.SNAPSHOT_NOT_READY, response.result());
    }

    @Test
    void reportsWhenARequestedZoneHasNotArrivedInPartialSnapshot() {
        TrafficStateStore store = new TrafficStateStore();
        store.applySnapshot(new TrafficUpdateEvent(
                Instant.now(), List.of(zone("Zona A", CongestionStatus.MODERADA))));

        var response = new TrafficQueryService(store).resolve(new RouteQueryRequest(
                "query-3", "client-1", QueryType.ZONE, TrafficZone.B,
                null, null, Instant.now()));

        assertEquals(QueryResultStatus.SNAPSHOT_NOT_READY, response.result());
    }

    private TrafficUpdateZone zone(String zoneId, CongestionStatus status) {
        return new TrafficUpdateZone(zoneId, 42.5, status.name());
    }
}
