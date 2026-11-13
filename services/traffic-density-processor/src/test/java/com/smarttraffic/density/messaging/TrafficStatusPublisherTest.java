package com.smarttraffic.density.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smarttraffic.density.config.RabbitMessagingProperties;
import com.smarttraffic.density.domain.TrafficStatus;
import com.smarttraffic.density.domain.ZoneSnapshot;
import com.smarttraffic.density.service.ZoneStateStore;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class TrafficStatusPublisherTest {

    private static final String EXCHANGE = "traffic_updates";

    @Mock
    private ZoneStateStore zoneStateStore;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private TrafficStatusPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new TrafficStatusPublisher(
                zoneStateStore,
                rabbitTemplate,
                new RabbitMessagingProperties(EXCHANGE, 5000));
    }

    @Test
    void publicaElEstadoCompletoDeTodasLasZonasConocidas() {
        when(zoneStateStore.snapshots()).thenReturn(List.of(
                snapshot("Zona A", 45.0, TrafficStatus.MODERADA),
                snapshot("Zona B", 80.0, TrafficStatus.CONGESTIONADA)));

        publisher.publishSystemStatus();

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(""), payload.capture());

        SystemStatusMessage message = (SystemStatusMessage) payload.getValue();
        assertThat(message.timestamp()).isNotNull();
        assertThat(message.zones()).hasSize(2);
        assertThat(message.zones())
                .extracting(ZoneStatusMessage::zoneId)
                .containsExactly("Zona A", "Zona B");
        assertThat(message.zones().get(0).averageVehicles()).isEqualTo(45.0);
        assertThat(message.zones().get(0).trafficStatus()).isEqualTo(TrafficStatus.MODERADA);
        assertThat(message.zones().get(1).averageVehicles()).isEqualTo(80.0);
        assertThat(message.zones().get(1).trafficStatus()).isEqualTo(TrafficStatus.CONGESTIONADA);
    }

    @Test
    void publicaUnaListaVaciaCuandoAunNoHayLecturas() {
        when(zoneStateStore.snapshots()).thenReturn(List.of());

        publisher.publishSystemStatus();

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(""), payload.capture());
        assertThat(((SystemStatusMessage) payload.getValue()).zones()).isEmpty();
    }

    @Test
    void registraUnErrorDeRabbitSinPropagarlo() {
        when(zoneStateStore.snapshots()).thenReturn(List.of());
        org.mockito.Mockito.doThrow(new AmqpException("broker no disponible"))
                .when(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(""), any(Object.class));

        assertThatCode(() -> publisher.publishSystemStatus()).doesNotThrowAnyException();

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(""), any(Object.class));
    }

    private ZoneSnapshot snapshot(String zoneId, double average, TrafficStatus status) {
        return new ZoneSnapshot(
                zoneId,
                List.of((int) average),
                average,
                status,
                Instant.parse("2026-11-12T16:00:00Z"));
    }
}
