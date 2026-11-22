package com.smarttraffic.dashboard.controller;

import com.smarttraffic.dashboard.dto.HistoryReadingResponse;
import com.smarttraffic.dashboard.mapper.DashboardResponseMapper;
import com.smarttraffic.dashboard.service.TrafficHistoryService;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Historial de lecturas guardado en PostgreSQL por traffic-archiver.
 */
@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final TrafficHistoryService historyService;
    private final DashboardResponseMapper mapper;

    public HistoryController(TrafficHistoryService historyService, DashboardResponseMapper mapper) {
        this.historyService = historyService;
        this.mapper = mapper;
    }

    /**
     * GET /api/history?zoneId=Zona A&amp;from=2026-11-22T00:00:00Z&amp;to=2026-11-22T01:00:00Z
     *
     * <p>{@code from} y {@code to} son opcionales y van en ISO-8601 UTC.
     */
    @GetMapping
    public List<HistoryReadingResponse> findHistory(
            @RequestParam String zoneId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return historyService.findHistory(zoneId, from, to).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
