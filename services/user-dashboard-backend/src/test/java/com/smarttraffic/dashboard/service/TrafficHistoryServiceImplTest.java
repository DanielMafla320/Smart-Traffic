package com.smarttraffic.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smarttraffic.dashboard.config.DashboardProperties;
import com.smarttraffic.dashboard.domain.TrafficReadingHistory;
import com.smarttraffic.dashboard.exception.ReglaNegocioException;
import com.smarttraffic.dashboard.repository.TrafficHistoryRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

@ExtendWith(MockitoExtension.class)
class TrafficHistoryServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-22T12:00:00Z");

    @Mock
    private TrafficHistoryRepository repository;

    private TrafficHistoryService service;

    @BeforeEach
    void setUp() {
        DashboardProperties properties = new DashboardProperties(
                "traffic_updates", "dashboard_traffic_queue", 100, List.of("http://localhost"),
                new DashboardProperties.History(Duration.ofHours(1), Duration.ofDays(7), 500));
        service = new TrafficHistoryServiceImpl(repository, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void consultaElRangoIndicadoConElLimiteConfigurado() {
        Instant from = Instant.parse("2026-11-22T10:00:00Z");
        Instant to = Instant.parse("2026-11-22T11:00:00Z");
        TrafficReadingHistory reading = new TrafficReadingHistory(
                1L, UUID.randomUUID(), "Zona A", 40, from.plusSeconds(10), from.plusSeconds(11));
        when(repository.findByZoneIdAndReadingTimestampBetweenOrderByReadingTimestampDesc(
                "Zona A", from, to, Limit.of(500))).thenReturn(List.of(reading));

        List<TrafficReadingHistory> result = service.findHistory(" Zona A ", from, to);

        assertThat(result).containsExactly(reading);
    }

    @Test
    void sinFechasUsaLaUltimaHora() {
        service.findHistory("Zona A", null, null);

        verify(repository).findByZoneIdAndReadingTimestampBetweenOrderByReadingTimestampDesc(
                "Zona A", NOW.minus(Duration.ofHours(1)), NOW, Limit.of(500));
    }

    @Test
    void rechazaZonaVacia() {
        assertThatThrownBy(() -> service.findHistory("  ", null, null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("zoneId");
        verifySinConsulta();
    }

    @Test
    void rechazaFromPosteriorATo() {
        Instant from = Instant.parse("2026-11-22T11:00:00Z");
        Instant to = Instant.parse("2026-11-22T10:00:00Z");

        assertThatThrownBy(() -> service.findHistory("Zona A", from, to))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("from");
        verifySinConsulta();
    }

    @Test
    void rechazaRangoMayorAlMaximo() {
        Instant from = NOW.minus(Duration.ofDays(8));

        assertThatThrownBy(() -> service.findHistory("Zona A", from, NOW))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("168");
        verifySinConsulta();
    }

    private void verifySinConsulta() {
        verify(repository, never()).findByZoneIdAndReadingTimestampBetweenOrderByReadingTimestampDesc(
                anyString(), any(), any(), any());
    }
}
