package com.smarttraffic.archiver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smarttraffic.archiver.domain.TrafficStatus;
import com.smarttraffic.archiver.domain.TrafficStatusChange;
import com.smarttraffic.archiver.dto.TrafficUpdateMessage;
import com.smarttraffic.archiver.dto.TrafficUpdateMessage.Zone;
import com.smarttraffic.archiver.exception.ReglaNegocioException;
import com.smarttraffic.archiver.repository.TrafficStatusChangeRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class TrafficStatusHistoryServiceImplTest {

    private static final Instant T0 = Instant.parse("2026-11-12T16:00:00Z");

    @Mock
    private TrafficStatusChangeRepository repository;

    private TrafficStatusHistoryService service;

    @BeforeEach
    void setUp() {
        service = new TrafficStatusHistoryServiceImpl(repository);
        lenient().when(repository.findFirstByZoneIdOrderByStatusTimestampDescIdDesc(anyString()))
                .thenReturn(Optional.empty());
        lenient().when(repository.save(any(TrafficStatusChange.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void guardaElEstadoInicialDeCadaZona() {
        List<TrafficStatusChange> changes = service.recordChanges(snapshot(T0,
                new Zone("Zona A", 10.0, "DESPEJADA"),
                new Zone("Zona B", 70.0, "CONGESTIONADA")));

        assertThat(changes).hasSize(2);
        assertThat(changes.get(0).getZoneId()).isEqualTo("Zona A");
        assertThat(changes.get(0).getPreviousStatus()).isNull();
        assertThat(changes.get(0).getTrafficStatus()).isEqualTo(TrafficStatus.DESPEJADA);
        assertThat(changes.get(1).getTrafficStatus()).isEqualTo(TrafficStatus.CONGESTIONADA);
        assertThat(changes.get(1).getStatusTimestamp()).isEqualTo(T0);
    }

    @Test
    void noGuardaSnapshotsSinCambioDeEstado() {
        service.recordChanges(snapshot(T0, new Zone("Zona A", 10.0, "DESPEJADA")));
        List<TrafficStatusChange> changes = service.recordChanges(
                snapshot(T0.plusSeconds(5), new Zone("Zona A", 12.0, "DESPEJADA")));

        assertThat(changes).isEmpty();
        verify(repository, times(1)).save(any(TrafficStatusChange.class));
    }

    @Test
    void guardaElCambioConElEstadoAnterior() {
        service.recordChanges(snapshot(T0, new Zone("Zona A", 10.0, "DESPEJADA")));
        List<TrafficStatusChange> changes = service.recordChanges(
                snapshot(T0.plusSeconds(5), new Zone("Zona A", 45.5, "MODERADA")));

        assertThat(changes).singleElement().satisfies(change -> {
            assertThat(change.getPreviousStatus()).isEqualTo(TrafficStatus.DESPEJADA);
            assertThat(change.getTrafficStatus()).isEqualTo(TrafficStatus.MODERADA);
            assertThat(change.getAverageVehicles()).isEqualTo(45.5);
            assertThat(change.getStatusTimestamp()).isEqualTo(T0.plusSeconds(5));
        });
    }

    @Test
    void trasUnReinicioUsaElUltimoEstadoGuardado() {
        TrafficStatusChange stored = TrafficStatusChange.registrar(
                "Zona A", null, TrafficStatus.CONGESTIONADA, 80.0, T0);
        when(repository.findFirstByZoneIdOrderByStatusTimestampDescIdDesc("Zona A"))
                .thenReturn(Optional.of(stored));

        List<TrafficStatusChange> changes = service.recordChanges(
                snapshot(T0.plusSeconds(60), new Zone("Zona A", 75.0, "CONGESTIONADA")));

        assertThat(changes).isEmpty();
        verify(repository, never()).save(any(TrafficStatusChange.class));
    }

    @Test
    void unaZonaInvalidaNoImpideGuardarLasDemas() {
        List<TrafficStatusChange> changes = service.recordChanges(snapshot(T0,
                new Zone("Zona A", 10.0, "DESCONOCIDO"),
                new Zone("  ", 10.0, "DESPEJADA"),
                new Zone("Zona C", -1.0, "DESPEJADA"),
                new Zone("Zona D", 50.0, "moderada")));

        assertThat(changes).singleElement()
                .satisfies(change -> {
                    assertThat(change.getZoneId()).isEqualTo("Zona D");
                    assertThat(change.getTrafficStatus()).isEqualTo(TrafficStatus.MODERADA);
                });
    }

    @Test
    void rechazaUnSnapshotSinTimestamp() {
        assertThatThrownBy(() -> service.recordChanges(
                new TrafficUpdateMessage(null, List.of(new Zone("Zona A", 10.0, "DESPEJADA")))))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> service.recordChanges(null))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void siLaBaseFallaElCambioSeVuelveADetectarEnElReintento() {
        when(repository.save(any(TrafficStatusChange.class)))
                .thenThrow(new DataAccessResourceFailureException("base caída"))
                .thenAnswer(invocation -> invocation.getArgument(0));
        TrafficUpdateMessage message = snapshot(T0, new Zone("Zona A", 70.0, "CONGESTIONADA"));

        assertThatThrownBy(() -> service.recordChanges(message))
                .isInstanceOf(DataAccessResourceFailureException.class);

        assertThat(service.recordChanges(message)).hasSize(1);
    }

    private TrafficUpdateMessage snapshot(Instant timestamp, Zone... zones) {
        return new TrafficUpdateMessage(timestamp, List.of(zones));
    }
}
