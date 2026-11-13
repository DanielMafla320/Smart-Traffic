package com.smarttraffic.density.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smarttraffic.density.config.DensityProperties;
import com.smarttraffic.density.config.DensityProperties.Strategy;
import com.smarttraffic.density.domain.TrafficStatus;
import com.smarttraffic.density.domain.ZoneSnapshot;
import com.smarttraffic.density.strategy.SimpleAverageStrategy;
import com.smarttraffic.density.strategy.WeightedAverageStrategy;
import java.time.Instant;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ZoneStateStoreTest {

    private static final Instant BASE_TIME = Instant.parse("2026-11-12T16:00:00Z");

    @Test
    void unaZonaSinLecturasNoTieneEstado() {
        ZoneStateStore store = newStore(10, 30.0, 60.0);

        assertThat(store.snapshots()).isEmpty();
        assertThat(store.findSnapshot("Zona A")).isEmpty();
        assertThat(store.findSnapshot(null)).isEmpty();
        assertThat(store.findSnapshot("   ")).isEmpty();
    }

    @Test
    void creaElEstadoDeLaZonaConLaPrimeraLectura() {
        ZoneStateStore store = newStore(10, 30.0, 60.0);

        store.record("Zona A", 45, BASE_TIME);

        ZoneSnapshot snapshot = store.findSnapshot("Zona A").orElseThrow();
        assertThat(snapshot.zoneId()).isEqualTo("Zona A");
        assertThat(snapshot.recentReadings()).containsExactly(45);
        assertThat(snapshot.averageVehicles()).isEqualTo(45.0);
        assertThat(snapshot.trafficStatus()).isEqualTo(TrafficStatus.MODERADA);
        assertThat(snapshot.lastUpdated()).isEqualTo(BASE_TIME);
    }

    @Test
    void unaPrimeraLecturaBajaInicializaLaZonaComoDespejada() {
        ZoneStateStore store = newStore(10, 30.0, 60.0);

        store.record("Zona A", 10, BASE_TIME);

        assertThat(store.findSnapshot("Zona A").orElseThrow().trafficStatus())
                .isEqualTo(TrafficStatus.DESPEJADA);
    }

    @Test
    void limitaLaVentanaDeLecturasYRecalculaElPromedio() {
        ZoneStateStore store = newStore(3, 30.0, 60.0);

        store.record("Zona A", 10, BASE_TIME);
        store.record("Zona A", 20, BASE_TIME.plusSeconds(1));
        store.record("Zona A", 30, BASE_TIME.plusSeconds(2));
        store.record("Zona A", 40, BASE_TIME.plusSeconds(3));

        ZoneSnapshot snapshot = store.findSnapshot("Zona A").orElseThrow();
        assertThat(snapshot.recentReadings()).containsExactly(20, 30, 40);
        assertThat(snapshot.averageVehicles()).isEqualTo(30.0);
        assertThat(snapshot.lastUpdated()).isEqualTo(BASE_TIME.plusSeconds(3));
    }

    @Test
    void limitaLaVentanaAlValorPorDefectoDeDiez() {
        ZoneStateStore store = newStore(10, 30.0, 60.0);

        for (int i = 0; i < 15; i++) {
            store.record("Zona A", i, BASE_TIME.plusSeconds(i));
        }

        ZoneSnapshot snapshot = store.findSnapshot("Zona A").orElseThrow();
        assertThat(snapshot.recentReadings())
                .containsExactly(5, 6, 7, 8, 9, 10, 11, 12, 13, 14);
        assertThat(snapshot.averageVehicles()).isEqualTo(9.5);
    }

    @Test
    void mantieneElEstadoIndependienteYOrdenadoPorZona() {
        ZoneStateStore store = newStore(10, 30.0, 60.0);

        store.record("Zona B", 10, BASE_TIME);
        store.record("Zona A", 80, BASE_TIME);

        assertThat(store.snapshots())
                .extracting(ZoneSnapshot::zoneId)
                .containsExactly("Zona A", "Zona B");
        assertThat(store.findSnapshot("Zona A").orElseThrow().trafficStatus())
                .isEqualTo(TrafficStatus.CONGESTIONADA);
        assertThat(store.findSnapshot("Zona B").orElseThrow().trafficStatus())
                .isEqualTo(TrafficStatus.DESPEJADA);
    }

    @Test
    void aplicaLasTransicionesDeEstadoSegunLosUmbrales() {
        ZoneStateStore store = newStore(1, 30.0, 60.0);

        store.record("Zona A", 10, BASE_TIME);
        assertThat(currentStatus(store)).isEqualTo(TrafficStatus.DESPEJADA);

        store.record("Zona A", 40, BASE_TIME.plusSeconds(1));
        assertThat(currentStatus(store)).isEqualTo(TrafficStatus.MODERADA);

        store.record("Zona A", 70, BASE_TIME.plusSeconds(2));
        assertThat(currentStatus(store)).isEqualTo(TrafficStatus.CONGESTIONADA);

        store.record("Zona A", 40, BASE_TIME.plusSeconds(3));
        assertThat(currentStatus(store)).isEqualTo(TrafficStatus.MODERADA);

        store.record("Zona A", 10, BASE_TIME.plusSeconds(4));
        assertThat(currentStatus(store)).isEqualTo(TrafficStatus.DESPEJADA);
    }

    @Test
    void aplicaLaEstrategiaPonderadaConfigurada() {
        DensityProperties properties = new DensityProperties(
                Strategy.WEIGHTED_AVERAGE, 10, 30.0, 60.0);
        ZoneStateStore store = new ZoneStateStore(new WeightedAverageStrategy(), properties);

        store.record("Zona A", 10, BASE_TIME);
        store.record("Zona A", 20, BASE_TIME.plusSeconds(1));
        store.record("Zona A", 30, BASE_TIME.plusSeconds(2));

        ZoneSnapshot snapshot = store.findSnapshot("Zona A").orElseThrow();
        assertThat(snapshot.averageVehicles()).isEqualTo(140.0 / 6.0);
        assertThat(snapshot.trafficStatus()).isEqualTo(TrafficStatus.DESPEJADA);
    }

    @Test
    void normalizaElIdentificadorDeLaZona() {
        ZoneStateStore store = newStore(10, 30.0, 60.0);

        store.record("  Zona A  ", 10, BASE_TIME);

        assertThat(store.findSnapshot("Zona A")).isPresent();
        assertThat(store.snapshots()).extracting(ZoneSnapshot::zoneId)
                .containsExactly("Zona A");
    }

    @Test
    void rechazaDatosInvalidosSinCrearEstado() {
        ZoneStateStore store = newStore(10, 30.0, 60.0);

        assertThatThrownBy(() -> store.record(null, 10, BASE_TIME))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.record("   ", 10, BASE_TIME))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.record("Zona A", -1, BASE_TIME))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.record("Zona A", 10, null))
                .isInstanceOf(NullPointerException.class);

        assertThat(store.snapshots()).isEmpty();
    }

    @Test
    void losSnapshotsSonInmutables() {
        ZoneStateStore store = newStore(10, 30.0, 60.0);
        store.record("Zona A", 10, BASE_TIME);

        ZoneSnapshot snapshot = store.findSnapshot("Zona A").orElseThrow();

        assertThatThrownBy(() -> snapshot.recentReadings().add(20))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void soportaActualizacionesConcurrentesSinPerderLaVentanaAcotada() {
        ZoneStateStore store = newStore(10, 30.0, 60.0);

        IntStream.range(0, 1000).parallel()
                .forEach(index -> store.record("Zona A", index % 100, BASE_TIME.plusMillis(index)));

        ZoneSnapshot snapshot = store.findSnapshot("Zona A").orElseThrow();
        assertThat(snapshot.recentReadings()).hasSize(10);
        assertThat(snapshot.recentReadings()).allSatisfy(value -> assertThat(value).isBetween(0, 99));
    }

    @Test
    void mantieneZonasIndependientesConActualizacionesConcurrentes() {
        ZoneStateStore store = newStore(10, 30.0, 60.0);

        IntStream.range(0, 10).parallel().forEach(zoneIndex ->
                IntStream.range(0, 100).forEach(readingIndex ->
                        store.record("Zona " + zoneIndex, readingIndex % 100,
                                BASE_TIME.plusMillis(readingIndex))));

        assertThat(store.snapshots()).hasSize(10);
        assertThat(store.snapshots()).allSatisfy(snapshot -> {
            assertThat(snapshot.recentReadings()).hasSize(10);
            assertThat(snapshot.averageVehicles()).isBetween(0.0, 99.0);
        });
    }

    private TrafficStatus currentStatus(ZoneStateStore store) {
        return store.findSnapshot("Zona A").orElseThrow().trafficStatus();
    }

    private ZoneStateStore newStore(int windowSize, double moderada, double congestionada) {
        DensityProperties properties = new DensityProperties(
                Strategy.SIMPLE_AVERAGE, windowSize, moderada, congestionada);
        return new ZoneStateStore(new SimpleAverageStrategy(), properties);
    }
}
