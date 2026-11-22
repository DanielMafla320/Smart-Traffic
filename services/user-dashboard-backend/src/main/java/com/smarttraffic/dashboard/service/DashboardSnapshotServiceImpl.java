package com.smarttraffic.dashboard.service;

import com.smarttraffic.dashboard.dto.DashboardUpdateResponse;
import com.smarttraffic.dashboard.mapper.DashboardResponseMapper;
import org.springframework.stereotype.Service;

@Service
public class DashboardSnapshotServiceImpl implements DashboardSnapshotService {

    private final ZoneStateService zoneStateService;
    private final TrafficStatsService statsService;
    private final DashboardResponseMapper mapper;

    public DashboardSnapshotServiceImpl(
            ZoneStateService zoneStateService,
            TrafficStatsService statsService,
            DashboardResponseMapper mapper) {
        this.zoneStateService = zoneStateService;
        this.statsService = statsService;
        this.mapper = mapper;
    }

    @Override
    public DashboardUpdateResponse currentSnapshot() {
        return new DashboardUpdateResponse(
                DashboardUpdateResponse.TYPE_TRAFFIC_UPDATE,
                zoneStateService.findAll().stream().map(mapper::toResponse).toList(),
                mapper.toResponse(statsService.currentStats()));
    }
}
