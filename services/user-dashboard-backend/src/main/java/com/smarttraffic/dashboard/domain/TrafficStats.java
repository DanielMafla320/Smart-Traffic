package com.smarttraffic.dashboard.domain;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resumen general del tráfico calculado a partir del estado actual.
 *
 * @param totalVehicles  suma de los promedios de vehículos de todas las zonas
 * @param zonesCount     cantidad de zonas con estado conocido
 * @param congestedZones zonas en estado CONGESTIONADA
 * @param zonesByStatus  cantidad de zonas por estado (incluye los estados con 0)
 * @param lastUpdated    instante del último snapshot recibido, o null si no hay datos
 */
public record TrafficStats(
        double totalVehicles,
        int zonesCount,
        List<String> congestedZones,
        Map<TrafficStatus, Long> zonesByStatus,
        Instant lastUpdated) {

    public TrafficStats {
        congestedZones = List.copyOf(congestedZones);
        zonesByStatus = Map.copyOf(new EnumMap<>(zonesByStatus));
    }

    public boolean hasData() {
        return lastUpdated != null;
    }
}
