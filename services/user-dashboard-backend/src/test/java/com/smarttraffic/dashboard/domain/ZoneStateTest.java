package com.smarttraffic.dashboard.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smarttraffic.dashboard.exception.ReglaNegocioException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ZoneStateTest {

    private static final Instant TS = Instant.parse("2026-11-22T00:00:05Z");

    @Test
    void normalizaLaZonaYDetectaCongestion() {
        ZoneState zone = new ZoneState("  Zona A ", 75.0, TrafficStatus.CONGESTIONADA, TS);

        assertThat(zone.zoneId()).isEqualTo("Zona A");
        assertThat(zone.isCongested()).isTrue();
    }

    @Test
    void rechazaDatosInvalidos() {
        assertThatThrownBy(() -> new ZoneState(" ", 1, TrafficStatus.MODERADA, TS))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> new ZoneState("Zona A", -1, TrafficStatus.MODERADA, TS))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> new ZoneState("Zona A", Double.NaN, TrafficStatus.MODERADA, TS))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> new ZoneState("Zona A", 1, null, TS))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> new ZoneState("Zona A", 1, TrafficStatus.MODERADA, null))
                .isInstanceOf(ReglaNegocioException.class);
    }
}
