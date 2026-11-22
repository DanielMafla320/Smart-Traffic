package com.smarttraffic.dashboard.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smarttraffic.dashboard.domain.TrafficStatus;
import com.smarttraffic.dashboard.domain.ZoneState;
import com.smarttraffic.dashboard.exception.ReglaNegocioException;
import com.smarttraffic.dashboard.messaging.TrafficUpdateMessage;
import com.smarttraffic.dashboard.messaging.TrafficUpdateMessage.ZoneUpdateMessage;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrafficUpdateMessageMapperTest {

    private static final Instant TS = Instant.parse("2026-11-22T00:00:05Z");

    private final TrafficUpdateMapper mapper = new TrafficUpdateMessageMapper();

    @Test
    void convierteCadaZonaUsandoElTimestampDelMensaje() {
        TrafficUpdateMessage message = new TrafficUpdateMessage(TS, List.of(
                new ZoneUpdateMessage("Zona A", 52.87, "MODERADA"),
                new ZoneUpdateMessage("Zona B", 10.0, "despejada")));

        List<ZoneState> zones = mapper.toZoneStates(message);

        assertThat(zones).containsExactly(
                new ZoneState("Zona A", 52.87, TrafficStatus.MODERADA, TS),
                new ZoneState("Zona B", 10.0, TrafficStatus.DESPEJADA, TS));
    }

    @Test
    void devuelveListaVaciaSiNoHayZonas() {
        assertThat(mapper.toZoneStates(new TrafficUpdateMessage(TS, List.of()))).isEmpty();
        assertThat(mapper.toZoneStates(new TrafficUpdateMessage(TS, null))).isEmpty();
    }

    @Test
    void rechazaMensajeSinTimestamp() {
        assertThatThrownBy(() -> mapper.toZoneStates(new TrafficUpdateMessage(null, List.of())))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void rechazaEstadoDesconocido() {
        TrafficUpdateMessage message = new TrafficUpdateMessage(TS,
                List.of(new ZoneUpdateMessage("Zona A", 10.0, "LIBRE")));

        assertThatThrownBy(() -> mapper.toZoneStates(message))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("LIBRE");
    }

    @Test
    void rechazaPromedioFaltante() {
        TrafficUpdateMessage message = new TrafficUpdateMessage(TS,
                List.of(new ZoneUpdateMessage("Zona A", null, "MODERADA")));

        assertThatThrownBy(() -> mapper.toZoneStates(message))
                .isInstanceOf(ReglaNegocioException.class);
    }
}
