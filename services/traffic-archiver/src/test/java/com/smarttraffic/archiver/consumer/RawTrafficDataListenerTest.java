package com.smarttraffic.archiver.consumer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smarttraffic.archiver.domain.TrafficReading;
import com.smarttraffic.archiver.dto.TrafficReadingEvent;
import com.smarttraffic.archiver.exception.EventoDuplicadoException;
import com.smarttraffic.archiver.exception.ReglaNegocioException;
import com.smarttraffic.archiver.service.TrafficArchiveService;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class RawTrafficDataListenerTest {

    private static final UUID EVENT_ID = UUID.randomUUID();

    @Mock
    private TrafficArchiveService archiveService;

    private RawTrafficDataListener listener;

    @BeforeEach
    void setUp() {
        listener = new RawTrafficDataListener(archiveService);
    }

    @Test
    void archivaUnEventoValido() {
        TrafficReadingEvent event = evento();
        when(archiveService.archive(event))
                .thenReturn(TrafficReading.registrar(EVENT_ID, "Zona A", 30, event.timestamp()));

        listener.onMessage(registro(event));

        verify(archiveService).archive(event);
    }

    @Test
    void descartaUnValorQueNoSePudoDeserializar() {
        assertThatCode(() -> listener.onMessage(registro(null))).doesNotThrowAnyException();

        verify(archiveService, never()).archive(any());
    }

    @Test
    void ignoraUnDuplicadoSinLanzarExcepcion() {
        TrafficReadingEvent event = evento();
        when(archiveService.archive(event)).thenThrow(new EventoDuplicadoException(EVENT_ID));

        assertThatCode(() -> listener.onMessage(registro(event))).doesNotThrowAnyException();
    }

    @Test
    void descartaDatosInvalidosSinLanzarExcepcion() {
        TrafficReadingEvent event = evento();
        when(archiveService.archive(event)).thenThrow(new ReglaNegocioException("vehículos negativos"));

        assertThatCode(() -> listener.onMessage(registro(event))).doesNotThrowAnyException();
    }

    @Test
    void dejaSubirElErrorDeBaseDeDatosParaQueKafkaReintente() {
        TrafficReadingEvent event = evento();
        when(archiveService.archive(event))
                .thenThrow(new DataAccessResourceFailureException("Connection refused"));

        assertThatThrownBy(() -> listener.onMessage(registro(event)))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    private TrafficReadingEvent evento() {
        return new TrafficReadingEvent(EVENT_ID, "Zona A", 30, Instant.parse("2026-11-21T13:00:00Z"));
    }

    private ConsumerRecord<String, TrafficReadingEvent> registro(TrafficReadingEvent value) {
        return new ConsumerRecord<>("raw_traffic_data", 0, 7L, "Zona A", value);
    }
}
