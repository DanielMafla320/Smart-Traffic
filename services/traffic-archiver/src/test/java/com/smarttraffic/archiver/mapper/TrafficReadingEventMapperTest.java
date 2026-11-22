package com.smarttraffic.archiver.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smarttraffic.archiver.domain.TrafficReading;
import com.smarttraffic.archiver.dto.TrafficReadingEvent;
import com.smarttraffic.archiver.exception.ReglaNegocioException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrafficReadingEventMapperTest {

    private final TrafficReadingMapper mapper = new TrafficReadingEventMapper();

    @Test
    void convierteElEventoEnLaEntidad() {
        UUID eventId = UUID.randomUUID();
        Instant timestamp = Instant.parse("2026-11-21T13:00:00Z");

        TrafficReading reading = mapper.toEntity(new TrafficReadingEvent(eventId, "Zona C", 42, timestamp));

        assertThat(reading.getEventId()).isEqualTo(eventId);
        assertThat(reading.getZoneId()).isEqualTo("Zona C");
        assertThat(reading.getVehicleCount()).isEqualTo(42);
        assertThat(reading.getReadingTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void rechazaEventoNulo() {
        assertThatThrownBy(() -> mapper.toEntity(null))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void propagaLasReglasDelDominio() {
        TrafficReadingEvent invalido = new TrafficReadingEvent(UUID.randomUUID(), "Zona A", -3, Instant.now());

        assertThatThrownBy(() -> mapper.toEntity(invalido))
                .isInstanceOf(ReglaNegocioException.class);
    }
}
