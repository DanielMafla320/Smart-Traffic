package com.smarttraffic.archiver.service;

import com.smarttraffic.archiver.domain.TrafficStatus;
import com.smarttraffic.archiver.domain.TrafficStatusChange;
import com.smarttraffic.archiver.dto.TrafficUpdateMessage;
import com.smarttraffic.archiver.exception.ReglaNegocioException;
import com.smarttraffic.archiver.repository.TrafficStatusChangeRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Detecta cambios de estado por zona y los guarda en {@code traffic_status_history}.
 *
 * <p>El último estado de cada zona se mantiene en memoria para no consultar la
 * base en cada snapshot. Si la zona no está en memoria (por ejemplo, tras un
 * reinicio), se consulta el último estado guardado. El listener de RabbitMQ usa
 * un solo hilo, pero el método es {@code synchronized} por seguridad.
 */
@Service
public class TrafficStatusHistoryServiceImpl implements TrafficStatusHistoryService {

    private static final Logger log = LoggerFactory.getLogger(TrafficStatusHistoryServiceImpl.class);

    private final TrafficStatusChangeRepository repository;
    private final Map<String, TrafficStatus> lastKnownStatus = new HashMap<>();

    public TrafficStatusHistoryServiceImpl(TrafficStatusChangeRepository repository) {
        this.repository = repository;
    }

    @Override
    public synchronized List<TrafficStatusChange> recordChanges(TrafficUpdateMessage message) {
        if (message == null) {
            throw new ReglaNegocioException("El mensaje de estado es obligatorio");
        }
        if (message.timestamp() == null) {
            throw new ReglaNegocioException("El timestamp del snapshot es obligatorio");
        }

        List<TrafficStatusChange> saved = new ArrayList<>();
        for (TrafficUpdateMessage.Zone zone : message.zones()) {
            try {
                recordZone(zone, message.timestamp()).ifPresent(saved::add);
            } catch (ReglaNegocioException exception) {
                log.warn("Zona descartada del snapshot {}: {}", message.timestamp(), exception.getMessage());
            }
        }
        return saved;
    }

    private Optional<TrafficStatusChange> recordZone(TrafficUpdateMessage.Zone zone, Instant timestamp) {
        if (zone == null || zone.zoneId() == null || zone.zoneId().isBlank()) {
            throw new ReglaNegocioException("El zone_id es obligatorio");
        }
        String zoneId = zone.zoneId().trim();
        TrafficStatus status = TrafficStatus.fromText(zone.trafficStatus());
        TrafficStatus previous = lastKnownStatus.containsKey(zoneId)
                ? lastKnownStatus.get(zoneId)
                : loadLastStatus(zoneId);

        if (status == previous) {
            lastKnownStatus.put(zoneId, status);
            return Optional.empty();
        }

        TrafficStatusChange change = TrafficStatusChange.registrar(
                zoneId, previous, status, zone.averageVehicles(), timestamp);
        TrafficStatusChange stored = repository.save(change);
        // Se actualiza la memoria solo después de guardar: si la base falla, el
        // reintento del mensaje vuelve a detectar el mismo cambio.
        lastKnownStatus.put(zoneId, status);
        log.info("Cambio de estado archivado: zona={} {} -> {} (promedio={})",
                zoneId, previous == null ? "(inicial)" : previous, status, zone.averageVehicles());
        return Optional.of(stored);
    }

    /** Devuelve null si la zona nunca tuvo un estado guardado. */
    private TrafficStatus loadLastStatus(String zoneId) {
        return repository.findFirstByZoneIdOrderByStatusTimestampDescIdDesc(zoneId)
                .map(TrafficStatusChange::getTrafficStatus)
                .orElse(null);
    }
}
