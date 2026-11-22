package com.smarttraffic.dashboard.messaging;

import com.smarttraffic.dashboard.domain.ZoneState;
import com.smarttraffic.dashboard.exception.SmartTrafficException;
import com.smarttraffic.dashboard.mapper.TrafficUpdateMapper;
import com.smarttraffic.dashboard.service.DashboardSnapshotService;
import com.smarttraffic.dashboard.service.ZoneStateService;
import com.smarttraffic.dashboard.websocket.TrafficBroadcaster;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consume la cola {@code dashboard_traffic_queue}. RabbitMQ empuja (push) cada
 * snapshot; se actualiza el estado en memoria y se reenvía a los navegadores.
 */
@Component
public class TrafficUpdateListener {

    private static final Logger log = LoggerFactory.getLogger(TrafficUpdateListener.class);

    private final TrafficUpdateMapper mapper;
    private final ZoneStateService zoneStateService;
    private final DashboardSnapshotService snapshotService;
    private final TrafficBroadcaster broadcaster;

    public TrafficUpdateListener(
            TrafficUpdateMapper mapper,
            ZoneStateService zoneStateService,
            DashboardSnapshotService snapshotService,
            TrafficBroadcaster broadcaster) {
        this.mapper = mapper;
        this.zoneStateService = zoneStateService;
        this.snapshotService = snapshotService;
        this.broadcaster = broadcaster;
    }

    @RabbitListener(queues = "${smarttraffic.dashboard.queue:dashboard_traffic_queue}")
    public void onTrafficUpdate(TrafficUpdateMessage message) {
        List<ZoneState> zones;
        try {
            zones = mapper.toZoneStates(message);
        } catch (SmartTrafficException exception) {
            // Mensaje inválido: se descarta (ack) para no bloquear la cola.
            log.warn("Actualización de tráfico descartada: {}", exception.getMessage());
            return;
        }

        if (zones.isEmpty()) {
            log.info("Snapshot {} sin zonas: esperando datos del procesador", message.timestamp());
        }

        boolean applied = zoneStateService.applySnapshot(message.timestamp(), zones);
        if (applied) {
            broadcaster.broadcast(snapshotService.currentSnapshot());
            log.debug("Snapshot {} con {} zonas reenviado a {} clientes WebSocket",
                    message.timestamp(), zones.size(), broadcaster.connectedClients());
        }
    }
}
