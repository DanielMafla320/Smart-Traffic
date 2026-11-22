package com.smarttraffic.dashboard.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.dashboard.dto.DashboardUpdateResponse;
import com.smarttraffic.dashboard.dto.ZoneStatusResponse;
import com.smarttraffic.dashboard.service.DashboardSnapshotService;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class TrafficWebSocketHandlerTest {

    private final DashboardSnapshotService snapshotService = mock(DashboardSnapshotService.class);
    private TrafficWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TrafficWebSocketHandler(snapshotService, new ObjectMapper().findAndRegisterModules());
        when(snapshotService.currentSnapshot()).thenReturn(update("Zona A"));
    }

    @Test
    void alConectarseRecibeElEstadoActual() throws Exception {
        WebSocketSession session = sesionAbierta("s1");

        handler.afterConnectionEstablished(session);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload())
                .contains("\"type\":\"traffic_update\"")
                .contains("\"zone_id\":\"Zona A\"");
        assertThat(handler.connectedClients()).isEqualTo(1);
    }

    @Test
    void difundeATodasLasSesionesAbiertas() throws Exception {
        WebSocketSession s1 = sesionAbierta("s1");
        WebSocketSession s2 = sesionAbierta("s2");
        handler.afterConnectionEstablished(s1);
        handler.afterConnectionEstablished(s2);

        handler.broadcast(update("Zona B"));

        verify(s1, times(2)).sendMessage(any(TextMessage.class));
        verify(s2, times(2)).sendMessage(any(TextMessage.class));
    }

    @Test
    void eliminaLaSesionAlCerrarse() throws Exception {
        WebSocketSession session = sesionAbierta("s1");
        handler.afterConnectionEstablished(session);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
        handler.broadcast(update("Zona B"));

        assertThat(handler.connectedClients()).isZero();
        verify(session, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void eliminaUnaSesionQueFallaAlEnviarYSigueConLasDemas() throws Exception {
        WebSocketSession rota = sesionAbierta("rota");
        WebSocketSession sana = sesionAbierta("sana");
        handler.afterConnectionEstablished(rota);
        handler.afterConnectionEstablished(sana);
        doThrow(new IOException("Broken pipe")).when(rota).sendMessage(any(TextMessage.class));

        handler.broadcast(update("Zona B"));

        assertThat(handler.connectedClients()).isEqualTo(1);
        verify(sana, times(2)).sendMessage(any(TextMessage.class));
    }

    @Test
    void sinClientesNoHaceNada() {
        handler.broadcast(update("Zona B"));

        assertThat(handler.connectedClients()).isZero();
        verify(snapshotService, never()).currentSnapshot();
    }

    private WebSocketSession sesionAbierta(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private DashboardUpdateResponse update(String zone) {
        return new DashboardUpdateResponse("traffic_update",
                List.of(new ZoneStatusResponse(zone, 40.0, "MODERADA", Instant.parse("2026-11-22T00:00:05Z"))),
                null);
    }
}
