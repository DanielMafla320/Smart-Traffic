package com.smarttraffic.archiver.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smarttraffic.archiver.dto.TrafficUpdateMessage;
import com.smarttraffic.archiver.exception.ReglaNegocioException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TrafficStatusChangeTest {

    private static final Instant T0 = Instant.parse("2026-11-12T16:00:00Z");

    @Test
    void registraUnCambioValidoNormalizandoLaZona() {
        TrafficStatusChange change = TrafficStatusChange.registrar(
                "  Zona A ", TrafficStatus.DESPEJADA, TrafficStatus.MODERADA, 35.0, T0);

        assertThat(change.getZoneId()).isEqualTo("Zona A");
        assertThat(change.getPreviousStatus()).isEqualTo(TrafficStatus.DESPEJADA);
        assertThat(change.getTrafficStatus()).isEqualTo(TrafficStatus.MODERADA);
        assertThat(change.getAverageVehicles()).isEqualTo(35.0);
        assertThat(change.getStatusTimestamp()).isEqualTo(T0);
    }

    @Test
    void rechazaDatosInvalidos() {
        assertThatThrownBy(() -> TrafficStatusChange.registrar(
                " ", null, TrafficStatus.MODERADA, 35.0, T0)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> TrafficStatusChange.registrar(
                "Zona A", null, null, 35.0, T0)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> TrafficStatusChange.registrar(
                "Zona A", null, TrafficStatus.MODERADA, -0.5, T0)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> TrafficStatusChange.registrar(
                "Zona A", null, TrafficStatus.MODERADA, null, T0)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> TrafficStatusChange.registrar(
                "Zona A", null, TrafficStatus.MODERADA, 35.0, null)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> TrafficStatusChange.registrar(
                "Z".repeat(51), null, TrafficStatus.MODERADA, 35.0, T0))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void rechazaUnCambioSinCambioDeEstado() {
        assertThatThrownBy(() -> TrafficStatusChange.registrar(
                "Zona A", TrafficStatus.MODERADA, TrafficStatus.MODERADA, 35.0, T0))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void convierteElTextoDelEstado() {
        assertThat(TrafficStatus.fromText(" congestionada ")).isEqualTo(TrafficStatus.CONGESTIONADA);
        assertThatThrownBy(() -> TrafficStatus.fromText("ROJO")).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> TrafficStatus.fromText(null)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void leeElJsonQuePublicaElProcesador() throws Exception {
        ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        String json = """
                {"timestamp":"2026-11-12T16:00:00Z","zones":[
                {"zone_id":"Zona A","average_vehicles":61.5,"traffic_status":"CONGESTIONADA"}]}""";

        TrafficUpdateMessage message = objectMapper.readValue(json, TrafficUpdateMessage.class);

        assertThat(message.timestamp()).isEqualTo(T0);
        assertThat(message.zones()).singleElement().satisfies(zone -> {
            assertThat(zone.zoneId()).isEqualTo("Zona A");
            assertThat(zone.averageVehicles()).isEqualTo(61.5);
            assertThat(zone.trafficStatus()).isEqualTo("CONGESTIONADA");
        });
    }
}
