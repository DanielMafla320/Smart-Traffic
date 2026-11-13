package com.smarttraffic.density.messaging;

import com.smarttraffic.density.config.RabbitMessagingProperties;
import com.smarttraffic.density.domain.ZoneSnapshot;
import com.smarttraffic.density.service.ZoneStateStore;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Publica periódicamente el estado completo de todas las zonas conocidas.
 *
 * <p>Si aún no se recibió ninguna lectura se publica un mensaje con la lista
 * {@code zones} vacía, manteniendo el contrato y permitiendo que los consumidores
 * distingan "sin datos" de "sin publicación".
 */
@Component
public class TrafficStatusPublisher {

    private static final Logger log = LoggerFactory.getLogger(TrafficStatusPublisher.class);

    private final ZoneStateStore zoneStateStore;
    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public TrafficStatusPublisher(
            ZoneStateStore zoneStateStore,
            RabbitTemplate rabbitTemplate,
            RabbitMessagingProperties properties) {
        this.zoneStateStore = zoneStateStore;
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = properties.exchange();
    }

    @Scheduled(fixedDelayString = "${smarttraffic.rabbit.publish-interval-ms:5000}")
    public void publishSystemStatus() {
        List<ZoneStatusMessage> zones = zoneStateStore.snapshots().stream()
                .map(this::toMessage)
                .toList();
        SystemStatusMessage message = new SystemStatusMessage(Instant.now(), zones);

        try {
            rabbitTemplate.convertAndSend(exchange, "", message);
            log.debug("Estado de {} zonas publicado en el exchange {}", zones.size(), exchange);
        } catch (RuntimeException exception) {
            log.error("No fue posible publicar el estado en el exchange {}", exchange, exception);
        }
    }

    private ZoneStatusMessage toMessage(ZoneSnapshot snapshot) {
        return new ZoneStatusMessage(
                snapshot.zoneId(),
                snapshot.averageVehicles(),
                snapshot.trafficStatus());
    }
}
