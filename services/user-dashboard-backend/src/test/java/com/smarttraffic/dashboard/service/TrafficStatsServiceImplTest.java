package com.smarttraffic.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarttraffic.dashboard.domain.TrafficStats;
import com.smarttraffic.dashboard.domain.TrafficStatus;
import com.smarttraffic.dashboard.domain.ZoneState;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrafficStatsServiceImplTest {

    private static final Instant TS = Instant.parse("2026-11-22T00:00:05Z");

    private final ZoneStateService zoneStateService = new InMemoryZoneStateService();
    private final TrafficStatsService statsService = new TrafficStatsServiceImpl(zoneStateService);

    @Test
    void calculaTotalesCongestionYConteoPorEstado() {
        zoneStateService.applySnapshot(TS, List.of(
                new ZoneState("Zona A", 70.5, TrafficStatus.CONGESTIONADA, TS),
                new ZoneState("Zona B", 40.0, TrafficStatus.MODERADA, TS),
                new ZoneState("Zona C", 80.0, TrafficStatus.CONGESTIONADA, TS),
                new ZoneState("Zona D", 10.0, TrafficStatus.DESPEJADA, TS)));

        TrafficStats stats = statsService.currentStats();

        assertThat(stats.totalVehicles()).isEqualTo(200.5);
        assertThat(stats.zonesCount()).isEqualTo(4);
        assertThat(stats.congestedZones()).containsExactly("Zona A", "Zona C");
        assertThat(stats.zonesByStatus())
                .containsEntry(TrafficStatus.CONGESTIONADA, 2L)
                .containsEntry(TrafficStatus.MODERADA, 1L)
                .containsEntry(TrafficStatus.DESPEJADA, 1L);
        assertThat(stats.lastUpdated()).isEqualTo(TS);
        assertThat(stats.hasData()).isTrue();
    }

    @Test
    void sinDatosDevuelveCerosEnTodosLosEstados() {
        TrafficStats stats = statsService.currentStats();

        assertThat(stats.totalVehicles()).isZero();
        assertThat(stats.zonesCount()).isZero();
        assertThat(stats.congestedZones()).isEmpty();
        assertThat(stats.zonesByStatus()).hasSize(3).allSatisfy((status, count) -> assertThat(count).isZero());
        assertThat(stats.hasData()).isFalse();
    }
}
