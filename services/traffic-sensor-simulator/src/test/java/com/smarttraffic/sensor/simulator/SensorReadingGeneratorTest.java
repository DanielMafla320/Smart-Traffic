package com.smarttraffic.sensor.simulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smarttraffic.sensor.domain.SensorReadingRecord;
import com.smarttraffic.sensor.domain.TrafficZone;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class SensorReadingGeneratorTest {

    private final SensorReadingGenerator generator = new SensorReadingGenerator();

    @Test
    void generaUnaLecturaValidaDentroDelRangoConfigurado() {
        TrafficZone zone = new TrafficZone("Zona A");
        Instant before = Instant.now();

        for (int i = 0; i < 100; i++) {
            SensorReadingRecord reading = generator.generate(zone, 0, 120);

            assertThat(reading.eventId()).isNotNull();
            assertThat(reading.zoneId()).isEqualTo("Zona A");
            assertThat(reading.vehicleCount()).isBetween(0, 120);
            assertThat(reading.timestamp()).isBetween(before, Instant.now());
        }
    }

    @Test
    void generaIdentificadoresUnicos() {
        Set<UUID> ids = IntStream.range(0, 1000)
                .mapToObj(index -> generator.generate(new TrafficZone("Zona A"), 0, 120))
                .map(SensorReadingRecord::eventId)
                .collect(Collectors.toSet());

        assertThat(ids).hasSize(1000);
    }

    @Test
    void admiteUnRangoExactoDeUnSoloValor() {
        SensorReadingRecord reading = generator.generate(new TrafficZone("Zona B"), 45, 45);

        assertThat(reading.vehicleCount()).isEqualTo(45);
    }

    @Test
    void rechazaRangosInvalidos() {
        TrafficZone zone = new TrafficZone("Zona A");

        assertThatThrownBy(() -> generator.generate(zone, -1, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> generator.generate(zone, 10, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaUnaZonaNula() {
        assertThatThrownBy(() -> generator.generate(null, 0, 10))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void normalizaElIdentificadorDeLaZona() {
        assertThat(new TrafficZone("  Zona C  ").id()).isEqualTo("Zona C");
        assertThatThrownBy(() -> new TrafficZone("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
