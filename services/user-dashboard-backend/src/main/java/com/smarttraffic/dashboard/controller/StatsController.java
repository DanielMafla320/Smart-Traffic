package com.smarttraffic.dashboard.controller;

import com.smarttraffic.dashboard.dto.TrafficStatsResponse;
import com.smarttraffic.dashboard.mapper.DashboardResponseMapper;
import com.smarttraffic.dashboard.service.TrafficStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Resumen general del tráfico.
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final TrafficStatsService statsService;
    private final DashboardResponseMapper mapper;

    public StatsController(TrafficStatsService statsService, DashboardResponseMapper mapper) {
        this.statsService = statsService;
        this.mapper = mapper;
    }

    /** GET /api/stats */
    @GetMapping
    public TrafficStatsResponse currentStats() {
        return mapper.toResponse(statsService.currentStats());
    }
}
