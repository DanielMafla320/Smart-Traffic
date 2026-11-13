package com.smarttraffic.density.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smarttraffic.density.config.DensityProperties;
import com.smarttraffic.density.config.DensityProperties.Strategy;
import com.smarttraffic.density.domain.TrafficReadingEvent;
import com.smarttraffic.density.domain.TrafficStatus;
import com.smarttraffic.density.domain.ZoneSnapshot;
import com.smarttraffic.density.strategy.SimpleAverageStrategy;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TrafficReadingProcessorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private ZoneStateStore store;
    private TrafficReadingProcessor processor;

    @BeforeEach
    void setUp() {
        DensityProperties properties = new DensityProperties(
                Strategy.SIMPLE_AVERAGE, 10, 30.0, 60.0);
        store = new ZoneStateStore(new SimpleAverageStrategy(), properties);
        processor = new TrafficReadingProcessor(validator, store);
    }

    @Test
    void validaYActualizaElEstadoDeLaZona() {
        TrafficReadingEvent event = event("Zona A", 45, Instant.parse("2026-11-12T16:00:00Z"));

        processor.process(event);

        ZoneSnapshot snapshot = store.findSnapshot("Zona A").orElseThrow();
        assertThat(snapshot.averageVehicles()).isEqualTo(45.0);
        assertThat(snapshot.trafficStatus()).isEqualTo(TrafficStatus.MODERADA);
        assertThat(snapshot.recentReadings()).containsExactly(45);
        assertThat(snapshot.lastUpdated()).isEqualTo(event.timestamp());
    }

    @Test
    void rechazaUnaCantidadNegativaSinModificarElEstado() {
        TrafficReadingEvent invalid = event("Zona A", -1, Instant.now());

        assertThatThrownBy(() -> processor.process(invalid))
                .isInstanceOf(InvalidTrafficReadingException.class)
                .hasMessageContaining("vehicleCount");
        assertThat(store.snapshots()).isEmpty();
    }

    @Test
    void rechazaUnaZonaVaciaSinModificarElEstado() {
        TrafficReadingEvent invalid = event("   ", 10, Instant.now());

        assertThatThrownBy(() -> processor.process(invalid))
                .isInstanceOf(InvalidTrafficReadingException.class);
        assertThat(store.snapshots()).isEmpty();
    }

    @Test
    void rechazaUnEventoNulo() {
        assertThatThrownBy(() -> processor.process(null))
                .isInstanceOf(InvalidTrafficReadingException.class)
                .hasMessageContaining("obligatorio");
    }

    @Test
    void rechazaUnEventoSinIdentificadorOTimestamp() {
        TrafficReadingEvent withoutId = new TrafficReadingEvent(
                null, "Zona A", 10, Instant.parse("2026-11-12T16:00:00Z"));
        TrafficReadingEvent withoutTimestamp = new TrafficReadingEvent(
                UUID.randomUUID(), "Zona A", 10, null);

        assertThatThrownBy(() -> processor.process(withoutId))
                .isInstanceOf(InvalidTrafficReadingException.class);
        assertThatThrownBy(() -> processor.process(withoutTimestamp))
                .isInstanceOf(InvalidTrafficReadingException.class);
        assertThat(store.snapshots()).isEmpty();
    }

    @Test
    void mantieneZonasIndependientes() {
        processor.process(event("Zona A", 10, Instant.parse("2026-11-12T16:00:00Z")));
        processor.process(event("Zona B", 80, Instant.parse("2026-11-12T16:00:01Z")));

        assertThat(store.findSnapshot("Zona A").orElseThrow().trafficStatus())
                .isEqualTo(TrafficStatus.DESPEJADA);
        assertThat(store.findSnapshot("Zona B").orElseThrow().trafficStatus())
                .isEqualTo(TrafficStatus.CONGESTIONADA);
    }

    private TrafficReadingEvent event(String zoneId, int vehicleCount, Instant timestamp) {
        return new TrafficReadingEvent(UUID.randomUUID(), zoneId, vehicleCount, timestamp);
    }
}
