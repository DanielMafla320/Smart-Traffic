package com.smarttraffic.alertdispatcher.service;

import com.smarttraffic.alertdispatcher.alert.TrafficAlertSubject;
import com.smarttraffic.alertdispatcher.domain.CongestionStatus;
import com.smarttraffic.alertdispatcher.domain.QueryResultStatus;
import com.smarttraffic.alertdispatcher.domain.QueryType;
import com.smarttraffic.alertdispatcher.domain.TrafficZone;
import com.smarttraffic.alertdispatcher.dto.RouteQueryRequest;
import com.smarttraffic.alertdispatcher.dto.TrafficUpdateEvent;
import com.smarttraffic.alertdispatcher.dto.TrafficUpdateZone;
import com.smarttraffic.alertdispatcher.state.StatusChange;
import com.smarttraffic.alertdispatcher.state.TrafficStateStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class TrafficUpdateProcessorTest {

    @Mock
    private TrafficAlertSubject alertSubject;

    private TrafficStateStore stateStore;
    private TrafficUpdateProcessor processor;

    @BeforeEach
    void setUp() {
        stateStore = new TrafficStateStore();
        processor = new TrafficUpdateProcessor(stateStore, alertSubject);
    }

    @Test
    void publishesOnlyChangesAfterTheInitialSnapshotAndIgnoresOldSnapshots() {
        processor.process(event(1, CongestionStatus.DESPEJADA));
        processor.process(event(2, CongestionStatus.CONGESTIONADA));

        var staleResult = processor.process(event(1, CongestionStatus.MODERADA));

        verify(alertSubject).publish(new StatusChange(
                TrafficZone.A, CongestionStatus.DESPEJADA,
                CongestionStatus.CONGESTIONADA, Instant.ofEpochSecond(2)));
        verifyNoMoreInteractions(alertSubject);
        assertFalse(staleResult.accepted());
        assertEquals(CongestionStatus.CONGESTIONADA, stateStore.statusOf(TrafficZone.A));
    }

    @Test
    void consumesDensityProcessorPayloadAndMapsSpanishZoneId() throws Exception {
        String payload = """
                {
                  "timestamp": "2026-11-22T12:00:00Z",
                  "zones": [
                    {
                      "zone_id": "Zona A",
                      "average_vehicles": 42.5,
                      "traffic_status": "MODERADA"
                    }
                  ]
                }
                """;
        TrafficUpdateEvent event = new ObjectMapper().findAndRegisterModules()
                .readValue(payload, TrafficUpdateEvent.class);

        var result = processor.process(event);

        assertTrue(result.accepted());
        assertEquals(CongestionStatus.MODERADA, stateStore.statusOf(TrafficZone.A));
        var response = new TrafficQueryService(stateStore).resolve(
                new RouteQueryRequest("query-1", "client-1", QueryType.ZONE,
                        TrafficZone.A, null, null, Instant.now()));
        assertEquals(QueryResultStatus.OK, response.result());
    }

    @Test
    void ignoresEmptyOrIncompleteInitialUpdatesWithoutThrowing() {
        var emptyResult = processor.process(new TrafficUpdateEvent(
                Instant.ofEpochSecond(1), List.of()));
        var incompleteResult = processor.process(new TrafficUpdateEvent(
                Instant.ofEpochSecond(2),
                List.of(new TrafficUpdateZone("Zona A", null, null))));

        assertFalse(emptyResult.accepted());
        assertFalse(incompleteResult.accepted());
        assertFalse(stateStore.isInitialized());
    }

    private TrafficUpdateEvent event(long timestamp, CongestionStatus zoneAStatus) {
        List<TrafficUpdateZone> zones = List.of(
                new TrafficUpdateZone("Zona A", 42.5, zoneAStatus.name()),
                new TrafficUpdateZone("Zona B", 10.0, CongestionStatus.DESPEJADA.name()),
                new TrafficUpdateZone("Zona C", 10.0, CongestionStatus.DESPEJADA.name()),
                new TrafficUpdateZone("Zona D", 10.0, CongestionStatus.DESPEJADA.name()),
                new TrafficUpdateZone("Zona E", 10.0, CongestionStatus.DESPEJADA.name()));
        return new TrafficUpdateEvent(Instant.ofEpochSecond(timestamp), zones);
    }
}
