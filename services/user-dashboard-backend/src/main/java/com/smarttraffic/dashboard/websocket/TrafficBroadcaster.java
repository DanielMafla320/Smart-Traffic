package com.smarttraffic.dashboard.websocket;

import com.smarttraffic.dashboard.dto.DashboardUpdateResponse;

/**
 * Envía una actualización a todos los clientes conectados en tiempo real.
 * El listener de RabbitMQ depende de esta interfaz, no del WebSocket concreto.
 */
public interface TrafficBroadcaster {

    void broadcast(DashboardUpdateResponse update);

    int connectedClients();
}
