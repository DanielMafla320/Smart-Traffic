package com.smarttraffic.archiver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smarttraffic.archiver.domain.TrafficReading;
import com.smarttraffic.archiver.dto.TrafficReadingEvent;
import com.smarttraffic.archiver.exception.EventoDuplicadoException;
import com.smarttraffic.archiver.exception.ReglaNegocioException;
import com.smarttraffic.archiver.mapper.TrafficReadingEventMapper;
import com.smarttraffic.archiver.repository.TrafficReadingRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class TrafficArchiveServiceImplTest {

    private static final UUID EVENT_ID = UUID.fromString("f33df0e7-b180-4a75-9561-569ff24f62eb");

    @Mock
    private TrafficReadingRepository repository;

    private TrafficArchiveService service;

    @BeforeEach
    void setUp() {
        // Mapper real (no tiene dependencias) y repositorio simulado con Mockito.
        service = new TrafficArchiveServiceImpl(repository, new TrafficReadingEventMapper());
    }

    @Test
    void guardaUnaLecturaNueva() {
        when(repository.existsByEventId(EVENT_ID)).thenReturn(false);
        when(repository.save(any(TrafficReading.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrafficReading saved = service.archive(evento(61));

        assertThat(saved.getEventId()).isEqualTo(EVENT_ID);
        assertThat(saved.getZoneId()).isEqualTo("Zona A");
        assertThat(saved.getVehicleCount()).isEqualTo(61);
        verify(repository).save(any(TrafficReading.class));
    }

    @Test
    void lanzaEventoDuplicadoSinInsertarSiElEventIdYaExiste() {
        when(repository.existsByEventId(EVENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.archive(evento(61)))
                .isInstanceOf(EventoDuplicadoException.class)
                .hasMessageContaining(EVENT_ID.toString());
        verify(repository, never()).save(any());
    }

    @Test
    void convierteLaViolacionDeUniqueEnEventoDuplicado() {
        // Simula que otro proceso insertó el mismo evento justo antes.
        when(repository.existsByEventId(EVENT_ID)).thenReturn(false, true);
        when(repository.save(any(TrafficReading.class)))
                .thenThrow(new DataIntegrityViolationException("uk_traffic_readings_event_id"));

        assertThatThrownBy(() -> service.archive(evento(61)))
                .isInstanceOf(EventoDuplicadoException.class);
    }

    @Test
    void convierteOtraViolacionDeRestriccionEnReglaDeNegocio() {
        when(repository.existsByEventId(EVENT_ID)).thenReturn(false, false);
        when(repository.save(any(TrafficReading.class)))
                .thenThrow(new DataIntegrityViolationException("ck_traffic_readings_vehicle_count"));

        assertThatThrownBy(() -> service.archive(evento(61)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechazaDatosInvalidosSinTocarLaBase() {
        assertThatThrownBy(() -> service.archive(evento(-5)))
                .isInstanceOf(ReglaNegocioException.class);
        verify(repository, never()).existsByEventId(any());
        verify(repository, never()).save(any());
    }

    @Test
    void propagaElErrorCuandoLaBaseNoEstaDisponible() {
        when(repository.existsByEventId(EVENT_ID))
                .thenThrow(new DataAccessResourceFailureException("Connection refused"));

        assertThatThrownBy(() -> service.archive(evento(61)))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    private TrafficReadingEvent evento(int vehiculos) {
        return new TrafficReadingEvent(EVENT_ID, "Zona A", vehiculos, Instant.parse("2026-11-13T04:21:25Z"));
    }
}
