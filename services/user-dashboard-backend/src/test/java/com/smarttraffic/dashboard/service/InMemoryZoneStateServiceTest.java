package com.smarttraffic.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarttraffic.dashboard.domain.TrafficStatus;
import com.smarttraffic.dashboard.domain.ZoneState;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryZoneStateServiceTest {

    private static final Instant T1 = Instant.parse("2026-11-22T00:00:05Z");
    private static final Instant T2 = Instant.parse("2026-11-22T00:00:10Z");

    private final ZoneStateService service = new InMemoryZoneStateService();

    @Test
    void empiezaSinDatos() {
        assertThat(service.findAll()).isEmpty();
        assertThat(service.lastSnapshotAt()).isEmpty();
    }

    @Test
    void aplicaUnSnapshotYOrdenaLasZonas() {
        boolean applied = service.applySnapshot(T1, List.of(zona("Zona B", 10, T1), zona("Zona A", 70, T1)));

        assertThat(applied).isTrue();
        assertThat(service.findAll()).extracting(ZoneState::zoneId).containsExactly("Zona A", "Zona B");
        assertThat(service.lastSnapshotAt()).contains(T1);
    }

    @Test
    void elSnapshotVacioRegistraLaFechaPeroNoZonas() {
        service.applySnapshot(T1, List.of());

        assertThat(service.findAll()).isEmpty();
        assertThat(service.lastSnapshotAt()).contains(T1);
    }

    @Test
    void reemplazaLasZonasConElSnapshotNuevo() {
        service.applySnapshot(T1, List.of(zona("Zona A", 10, T1), zona("Zona B", 20, T1)));
        service.applySnapshot(T2, List.of(zona("Zona A", 65, T2)));

        assertThat(service.findAll()).containsExactly(zona("Zona A", 65, T2));
    }

    @Test
    void ignoraUnSnapshotMasViejo() {
        service.applySnapshot(T2, List.of(zona("Zona A", 65, T2)));

        boolean applied = service.applySnapshot(T1, List.of(zona("Zona A", 10, T1)));

        assertThat(applied).isFalse();
        assertThat(service.findByZoneId("Zona A")).contains(zona("Zona A", 65, T2));
        assertThat(service.lastSnapshotAt()).contains(T2);
    }

    @Test
    void buscaPorZonaIgnorandoEspacios() {
        service.applySnapshot(T1, List.of(zona("Zona A", 10, T1)));

        assertThat(service.findByZoneId(" Zona A ")).isPresent();
        assertThat(service.findByZoneId("Zona Z")).isEmpty();
        assertThat(service.findByZoneId(null)).isEmpty();
    }

    private ZoneState zona(String id, double average, Instant ts) {
        TrafficStatus status = average >= 60 ? TrafficStatus.CONGESTIONADA
                : average >= 30 ? TrafficStatus.MODERADA : TrafficStatus.DESPEJADA;
        return new ZoneState(id, average, status, ts);
    }
}
