package com.smarttraffic.dashboard.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smarttraffic.dashboard.dto.DashboardUpdateResponse;
import com.smarttraffic.dashboard.mapper.TrafficUpdateMessageMapper;
import com.smarttraffic.dashboard.messaging.TrafficUpdateMessage.ZoneUpdateMessage;
import com.smarttraffic.dashboard.service.DashboardSnapshotService;
import com.smarttraffic.dashboard.service.InMemoryZoneStateService;
import com.smarttraffic.dashboard.service.ZoneStateService;
import com.smarttraffic.dashboard.websocket.TrafficBroadcaster;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrafficUpdateListenerTest {

    private static final Instant T1 = Instant.parse("2026-11-22T00:00:05Z");
    private static final Instant T2 = Instant.parse("2026-11-22T00:00:10Z");

    @Mock
    private DashboardSnapshotService snapshotService;
    @Mock
    private TrafficBroadcaster broadcaster;

    private ZoneStateService zoneStateService;
    private TrafficUpdateListener listener;

    @BeforeEach
    void setUp() {
        zoneStateService = new InMemoryZoneStateService();
        listener = new TrafficUpdateListener(
                new TrafficUpdateMessageMapper(), zoneStateService, snapshotService, broadcaster);
    }

    @Test
    void actualizaElEstadoYLoReenviaPorWebSocket() {
        DashboardUpdateResponse update = new DashboardUpdateResponse("traffic_update", List.of(), null);
        when(snapshotService.currentSnapshot()).thenReturn(update);

        listener.onTrafficUpdate(new TrafficUpdateMessage(T1,
                List.of(new ZoneUpdateMessage("Zona A", 52.87, "MODERADA"))));

        assertThat(zoneStateService.findByZoneId("Zona A")).isPresent();
        verify(broadcaster).broadcast(update);
    }

    @Test
    void elSnapshotVacioTambienSeReenvia() {
        when(snapshotService.currentSnapshot())
                .thenReturn(new DashboardUpdateResponse("traffic_update", List.of(), null));

        listener.onTrafficUpdate(new TrafficUpdateMessage(T1, List.of()));

        assertThat(zoneStateService.lastSnapshotAt()).contains(T1);
        verify(broadcaster).broadcast(any());
    }

    @Test
    void descartaUnMensajeInvalidoSinLanzarExcepcion() {
        TrafficUpdateMessage invalido = new TrafficUpdateMessage(T1,
                List.of(new ZoneUpdateMessage("Zona A", 10.0, "DESCONOCIDO")));

        assertThatCode(() -> listener.onTrafficUpdate(invalido)).doesNotThrowAnyException();

        assertThat(zoneStateService.findAll()).isEmpty();
        verify(broadcaster, never()).broadcast(any());
    }

    @Test
    void noReenviaUnSnapshotViejo() {
        zoneStateService.applySnapshot(T2, List.of());

        listener.onTrafficUpdate(new TrafficUpdateMessage(T1, List.of()));

        verify(broadcaster, never()).broadcast(any());
    }
}
