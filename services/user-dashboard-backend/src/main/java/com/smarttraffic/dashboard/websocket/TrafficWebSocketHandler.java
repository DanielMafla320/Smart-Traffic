package com.smarttraffic.dashboard.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.dashboard.dto.DashboardUpdateResponse;
import com.smarttraffic.dashboard.service.DashboardSnapshotService;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket puro en {@code /ws/traffic} (sin STOMP ni SockJS): el navegador
 * se conecta con {@code new WebSocket("ws://.../ws/traffic")}.
 *
 * <ul>
 *   <li>Al conectarse, el cliente recibe de inmediato el estado actual.</li>
 *   <li>En cada snapshot de RabbitMQ, se envía el mismo JSON a todos.</li>
 *   <li>Las sesiones cerradas o con error se eliminan.</li>
 * </ul>
 */
@Component
public class TrafficWebSocketHandler extends TextWebSocketHandler implements TrafficBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(TrafficWebSocketHandler.class);
    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 512 * 1024;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final DashboardSnapshotService snapshotService;
    private final ObjectMapper objectMapper;

    public TrafficWebSocketHandler(DashboardSnapshotService snapshotService, ObjectMapper objectMapper) {
        this.snapshotService = snapshotService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // WebSocketSession no permite envíos simultáneos desde varios hilos; el
        // decorador los serializa y corta a un cliente demasiado lento.
        WebSocketSession safeSession =
                new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT_BYTES);
        sessions.put(session.getId(), safeSession);
        log.info("Cliente WebSocket conectado: {} desde {} (total: {})",
                session.getId(), session.getRemoteAddress(), sessions.size());
        send(safeSession, toJson(snapshotService.currentSnapshot()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // El canal es solo de servidor a cliente; los mensajes del cliente se ignoran.
        log.debug("Mensaje del cliente {} ignorado: {}", session.getId(), message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("Error de transporte en la sesión {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("Cliente WebSocket desconectado: {} ({}) (total: {})",
                session.getId(), status, sessions.size());
    }

    @Override
    public void broadcast(DashboardUpdateResponse update) {
        if (sessions.isEmpty()) {
            return;
        }
        // Se serializa una sola vez y se envía el mismo texto a todos.
        String json = toJson(update);
        sessions.values().forEach(session -> send(session, json));
    }

    @Override
    public int connectedClients() {
        return sessions.size();
    }

    private void send(WebSocketSession session, String json) {
        if (json == null) {
            return;
        }
        if (!session.isOpen()) {
            sessions.remove(session.getId());
            return;
        }
        try {
            session.sendMessage(new TextMessage(json));
        } catch (IOException | RuntimeException exception) {
            // RuntimeException incluye SessionLimitExceededException (cliente demasiado lento).
            log.warn("No se pudo enviar a la sesión {}; se descarta: {}", session.getId(), exception.getMessage());
            sessions.remove(session.getId());
        }
    }

    private String toJson(DashboardUpdateResponse update) {
        try {
            return objectMapper.writeValueAsString(update);
        } catch (JsonProcessingException exception) {
            log.error("No se pudo serializar la actualización del dashboard", exception);
            return null;
        }
    }
}
