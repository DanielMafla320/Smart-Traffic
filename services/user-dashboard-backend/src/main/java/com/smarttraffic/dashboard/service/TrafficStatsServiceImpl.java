package com.smarttraffic.dashboard.service;

import com.smarttraffic.dashboard.domain.TrafficStats;
import com.smarttraffic.dashboard.domain.TrafficStatus;
import com.smarttraffic.dashboard.domain.ZoneState;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TrafficStatsServiceImpl implements TrafficStatsService {

    private final ZoneStateService zoneStateService;

    public TrafficStatsServiceImpl(ZoneStateService zoneStateService) {
        this.zoneStateService = zoneStateService;
    }

    @Override
    public TrafficStats currentStats() {
        List<ZoneState> zones = zoneStateService.findAll();

        double totalVehicles = zones.stream()
                .mapToDouble(ZoneState::averageVehicles)
                .sum();

        List<String> congestedZones = zones.stream()
                .filter(ZoneState::isCongested)
                .map(ZoneState::zoneId)
                .toList();

        // Se inicializan todos los estados en 0 para que el frontend siempre
        // reciba las tres claves, aunque no haya zonas en alguno.
        Map<TrafficStatus, Long> zonesByStatus = new EnumMap<>(TrafficStatus.class);
        for (TrafficStatus status : TrafficStatus.values()) {
            zonesByStatus.put(status, 0L);
        }
        zones.forEach(zone -> zonesByStatus.merge(zone.status(), 1L, Long::sum));

        return new TrafficStats(
                totalVehicles,
                zones.size(),
                congestedZones,
                zonesByStatus,
                zoneStateService.lastSnapshotAt().orElse(null));
    }
}
