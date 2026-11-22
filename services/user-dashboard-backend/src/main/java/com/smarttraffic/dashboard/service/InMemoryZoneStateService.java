package com.smarttraffic.dashboard.service;

import com.smarttraffic.dashboard.domain.ZoneState;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Guarda el estado de las zonas en un {@link ConcurrentHashMap}: lo escribe el
 * hilo del consumidor de RabbitMQ y lo leen a la vez los hilos de las peticiones
 * HTTP y del WebSocket.
 *
 * <p>Consistencia eventual (AP en CAP): refleja el último snapshot recibido;
 * si el procesador deja de publicar, se sigue mostrando el último estado
 * conocido con su fecha.
 */
@Service
public class InMemoryZoneStateService implements ZoneStateService {

    private static final Logger log = LoggerFactory.getLogger(InMemoryZoneStateService.class);

    private final ConcurrentMap<String, ZoneState> zones = new ConcurrentHashMap<>();
    private volatile Instant lastSnapshotAt;

    @Override
    public synchronized boolean applySnapshot(Instant snapshotTimestamp, List<ZoneState> newZones) {
        Objects.requireNonNull(snapshotTimestamp, "El timestamp del snapshot es obligatorio");
        Objects.requireNonNull(newZones, "La lista de zonas es obligatoria");

        if (lastSnapshotAt != null && snapshotTimestamp.isBefore(lastSnapshotAt)) {
            log.warn("Snapshot ignorado por ser anterior al último aplicado ({} < {})",
                    snapshotTimestamp, lastSnapshotAt);
            return false;
        }

        // Primero se insertan/actualizan las zonas y después se quitan las que
        // ya no vienen; así un lector nunca ve el mapa vacío a mitad de camino.
        newZones.forEach(zone -> zones.put(zone.zoneId(), zone));
        Set<String> incomingIds = newZones.stream().map(ZoneState::zoneId).collect(Collectors.toSet());
        zones.keySet().removeIf(zoneId -> !incomingIds.contains(zoneId));
        lastSnapshotAt = snapshotTimestamp;

        log.debug("Snapshot {} aplicado con {} zonas", snapshotTimestamp, newZones.size());
        return true;
    }

    @Override
    public List<ZoneState> findAll() {
        return zones.values().stream()
                .sorted(Comparator.comparing(ZoneState::zoneId))
                .toList();
    }

    @Override
    public Optional<ZoneState> findByZoneId(String zoneId) {
        if (zoneId == null || zoneId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(zones.get(zoneId.trim()));
    }

    @Override
    public Optional<Instant> lastSnapshotAt() {
        return Optional.ofNullable(lastSnapshotAt);
    }
}
