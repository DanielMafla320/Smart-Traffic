package com.smarttraffic.dashboard.controller;

import com.smarttraffic.dashboard.dto.ZoneStatusResponse;
import com.smarttraffic.dashboard.exception.RecursoNoEncontradoException;
import com.smarttraffic.dashboard.mapper.DashboardResponseMapper;
import com.smarttraffic.dashboard.service.ZoneStateService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Estado actual de las zonas.
 *
 * <p>El id de la zona va como parámetro ({@code ?zoneId=Zona A}) y no en la
 * ruta, porque contiene un espacio.
 */
@RestController
@RequestMapping("/api/zones")
public class ZoneController {

    private final ZoneStateService zoneStateService;
    private final DashboardResponseMapper mapper;

    public ZoneController(ZoneStateService zoneStateService, DashboardResponseMapper mapper) {
        this.zoneStateService = zoneStateService;
        this.mapper = mapper;
    }

    /** GET /api/zones: todas las zonas (lista vacía si aún no llegan datos). */
    @GetMapping
    public List<ZoneStatusResponse> findAll() {
        return zoneStateService.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    /** GET /api/zones/detail?zoneId=Zona A: una zona, o 404 si no existe. */
    @GetMapping("/detail")
    public ZoneStatusResponse findByZoneId(@RequestParam String zoneId) {
        return zoneStateService.findByZoneId(zoneId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay estado para la zona '" + zoneId + "'"));
    }
}
