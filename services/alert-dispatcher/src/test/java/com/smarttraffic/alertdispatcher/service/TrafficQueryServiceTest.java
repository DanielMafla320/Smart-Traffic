package com.smarttraffic.alertdispatcher.service;

import com.smarttraffic.alertdispatcher.domain.CongestionStatus;
import com.smarttraffic.alertdispatcher.domain.QueryResultStatus;
import com.smarttraffic.alertdispatcher.dto.QueryRequestRecord;
import com.smarttraffic.alertdispatcher.dto.TrafficUpdateEvent;
import com.smarttraffic.alertdispatcher.dto.TrafficUpdateZone;
import com.smarttraffic.alertdispatcher.state.TrafficStateStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrafficQueryServiceTest {

    @Test
    void resolvesTrayectoUsingTheWorstStatusOfAllItsZones() {
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

        var response = new TrafficQueryService(store).resolve(
                new QueryRequestRecord("client_1", List.of("Zona A", "Zona E")), "query-1");

        assertEquals(QueryResultStatus.OK, response.result());
        assertEquals(CongestionStatus.CONGESTIONADA, response.routeStatus());
        assertEquals(snapshotTimestamp, response.snapshotTimestamp());
        assertEquals(2, response.zones().size());
    }

    @Test
    void reportsWhenTheInitialSnapshotHasNotArrived() {
        var response = new TrafficQueryService(new TrafficStateStore()).resolve(
                new QueryRequestRecord("client_1", List.of("Zona A")), "query-2");

        assertEquals(QueryResultStatus.SNAPSHOT_NOT_READY, response.result());
    }

    @Test
    void reportsUnknownZoneAfterReceivingSnapshot() {
        TrafficStateStore store = new TrafficStateStore();
        store.applySnapshot(new TrafficUpdateEvent(
                Instant.now(), List.of(zone("Zona A", CongestionStatus.MODERADA))));

        var response = new TrafficQueryService(store).resolve(
                new QueryRequestRecord("client_1", List.of("Zona Z")), "query-3");

        assertEquals(QueryResultStatus.ZONE_NOT_FOUND, response.result());
        org.junit.jupiter.api.Assertions.assertTrue(response.message().contains("Zona Z"));
    }

    private TrafficUpdateZone zone(String zoneId, CongestionStatus status) {
        return new TrafficUpdateZone(zoneId, 42.5, status.name());
    }
}
