package com.smarttraffic.dashboard.mapper;

import com.smarttraffic.dashboard.domain.TrafficReadingHistory;
import com.smarttraffic.dashboard.domain.TrafficStats;
import com.smarttraffic.dashboard.domain.TrafficStatus;
import com.smarttraffic.dashboard.domain.ZoneState;
import com.smarttraffic.dashboard.dto.HistoryReadingResponse;
import com.smarttraffic.dashboard.dto.TrafficStatsResponse;
import com.smarttraffic.dashboard.dto.ZoneStatusResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DashboardResponseMapperImpl implements DashboardResponseMapper {

    @Override
    public ZoneStatusResponse toResponse(ZoneState zone) {
        return new ZoneStatusResponse(
                zone.zoneId(),
                round(zone.averageVehicles()),
                zone.status().name(),
                zone.lastUpdated());
    }

    @Override
    public TrafficStatsResponse toResponse(TrafficStats stats) {
        // LinkedHashMap en el orden del enum: DESPEJADA, MODERADA, CONGESTIONADA.
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (TrafficStatus status : TrafficStatus.values()) {
            byStatus.put(status.name(), stats.zonesByStatus().getOrDefault(status, 0L));
        }
        return new TrafficStatsResponse(
                round(stats.totalVehicles()),
                stats.zonesCount(),
                stats.congestedZones().size(),
                stats.congestedZones(),
                byStatus,
                stats.lastUpdated(),
                !stats.hasData() || stats.zonesCount() == 0);
    }

    @Override
    public HistoryReadingResponse toResponse(TrafficReadingHistory reading) {
        return new HistoryReadingResponse(
                reading.getEventId(),
                reading.getZoneId(),
                reading.getVehicleCount(),
                reading.getReadingTimestamp(),
                reading.getCreatedAt());
    }

    /** Redondea a 2 decimales para mostrar (52.8712 → 52.87). */
    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
