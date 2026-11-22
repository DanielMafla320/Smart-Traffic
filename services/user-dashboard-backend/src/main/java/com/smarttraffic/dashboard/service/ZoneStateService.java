package com.smarttraffic.dashboard.service;

import com.smarttraffic.dashboard.domain.ZoneState;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Estado actual de las zonas en memoria (la "réplica" local del dashboard).
 */
public interface ZoneStateService {

    /**
     * Aplica un snapshot completo recibido de RabbitMQ.
     *
     * @return {@code false} si el snapshot es más viejo que el último aplicado y se ignoró
     */
    boolean applySnapshot(Instant snapshotTimestamp, List<ZoneState> zones);

    /** Zonas ordenadas por identificador. */
    List<ZoneState> findAll();

    Optional<ZoneState> findByZoneId(String zoneId);

    /** Instante del último snapshot aplicado, vacío si aún no llegó ninguno. */
    Optional<Instant> lastSnapshotAt();
}
