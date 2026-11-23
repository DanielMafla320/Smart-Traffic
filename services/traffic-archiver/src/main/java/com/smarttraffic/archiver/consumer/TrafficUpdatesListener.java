package com.smarttraffic.archiver.consumer;

import com.smarttraffic.archiver.domain.TrafficStatusChange;
import com.smarttraffic.archiver.dto.TrafficUpdateMessage;
import com.smarttraffic.archiver.exception.ReglaNegocioException;
import com.smarttraffic.archiver.service.TrafficStatusHistoryService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Recibe los snapshots del exchange RabbitMQ {@code traffic_updates} y delega en
 * el servicio de historial de estados.
 *
 * <ul>
 *   <li>Snapshot inválido: se descarta con log.</li>
 *   <li>Base de datos caída: la excepción sube y RabbitMQ reintenta el mensaje.</li>
 * </ul>
 */
@Component
public class TrafficUpdatesListener {

    private static final Logger log = LoggerFactory.getLogger(TrafficUpdatesListener.class);

    private final TrafficStatusHistoryService historyService;

    public TrafficUpdatesListener(TrafficStatusHistoryService historyService) {
        this.historyService = historyService;
    }

    @RabbitListener(queues = "${smarttraffic.rabbit.status-history-queue:traffic_status_history_queue}")
    public void onMessage(TrafficUpdateMessage message) {
        try {
            List<TrafficStatusChange> changes = historyService.recordChanges(message);
            log.debug("Snapshot procesado: {} cambio(s) de estado archivado(s)", changes.size());
        } catch (ReglaNegocioException exception) {
            log.warn("Snapshot de estados descartado por datos inválidos: {}", exception.getMessage());
        }
    }
}
