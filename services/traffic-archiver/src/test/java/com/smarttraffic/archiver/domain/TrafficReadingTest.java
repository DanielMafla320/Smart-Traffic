package com.smarttraffic.archiver.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smarttraffic.archiver.exception.ReglaNegocioException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrafficReadingTest {

    private static final UUID EVENT_ID = UUID.fromString("f33df0e7-b180-4a75-9561-569ff24f62eb");
    private static final Instant TIMESTAMP = Instant.parse("2026-11-13T04:21:25.765885200Z");

    @Test
    void registraUnaLecturaValidaYNormalizaLaZona() {
        TrafficReading reading = TrafficReading.registrar(EVENT_ID, "  Zona A  ", 61, TIMESTAMP);

        assertThat(reading.getEventId()).isEqualTo(EVENT_ID);
        assertThat(reading.getZoneId()).isEqualTo("Zona A");
        assertThat(reading.getVehicleCount()).isEqualTo(61);
        assertThat(reading.getReadingTimestamp()).isEqualTo(TIMESTAMP);
        assertThat(reading.getId()).isNull();
        assertThat(reading.getCreatedAt()).isNull();
    }

    @Test
    void aceptaCeroVehiculos() {
        TrafficReading reading = TrafficReading.registrar(EVENT_ID, "Zona B", 0, TIMESTAMP);

        assertThat(reading.getVehicleCount()).isZero();
    }

    @Test
    void rechazaEventIdNulo() {
        assertThatThrownBy(() -> TrafficReading.registrar(null, "Zona A", 10, TIMESTAMP))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("event_id");
    }

    @Test
    void rechazaZonaVacia() {
        assertThatThrownBy(() -> TrafficReading.registrar(EVENT_ID, "   ", 10, TIMESTAMP))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("zone_id");
    }

    @Test
    void rechazaZonaMuyLarga() {
        String zonaLarga = "Z".repeat(51);

        assertThatThrownBy(() -> TrafficReading.registrar(EVENT_ID, zonaLarga, 10, TIMESTAMP))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("50");
    }

    @Test
    void rechazaCantidadNegativa() {
        assertThatThrownBy(() -> TrafficReading.registrar(EVENT_ID, "Zona A", -1, TIMESTAMP))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("vehículos");
    }

    @Test
    void rechazaCantidadNula() {
        assertThatThrownBy(() -> TrafficReading.registrar(EVENT_ID, "Zona A", null, TIMESTAMP))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void rechazaTimestampNulo() {
        assertThatThrownBy(() -> TrafficReading.registrar(EVENT_ID, "Zona A", 10, null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("timestamp");
    }
}
