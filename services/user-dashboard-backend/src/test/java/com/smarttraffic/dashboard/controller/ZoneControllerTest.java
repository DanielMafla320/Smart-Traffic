package com.smarttraffic.dashboard.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smarttraffic.dashboard.domain.TrafficStats;
import com.smarttraffic.dashboard.domain.TrafficStatus;
import com.smarttraffic.dashboard.domain.ZoneState;
import com.smarttraffic.dashboard.mapper.DashboardResponseMapperImpl;
import com.smarttraffic.dashboard.service.TrafficHistoryService;
import com.smarttraffic.dashboard.service.TrafficStatsService;
import com.smarttraffic.dashboard.service.ZoneStateService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pruebas de la capa web: rutas, JSON, códigos HTTP, errores y CORS.
 * Solo se levanta Spring MVC; los servicios son mocks.
 */
@WebMvcTest(controllers = {ZoneController.class, StatsController.class})
@Import(DashboardResponseMapperImpl.class)
class ZoneControllerTest {

    private static final Instant TS = Instant.parse("2026-11-22T00:00:05Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ZoneStateService zoneStateService;
    @MockitoBean
    private TrafficStatsService statsService;
    @MockitoBean
    private TrafficHistoryService historyService;

    @Test
    void listaLasZonasConNombresDelContrato() throws Exception {
        when(zoneStateService.findAll()).thenReturn(List.of(
                new ZoneState("Zona A", 52.8712, TrafficStatus.MODERADA, TS)));

        mockMvc.perform(get("/api/zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].zone_id").value("Zona A"))
                .andExpect(jsonPath("$[0].average_vehicles").value(52.87))
                .andExpect(jsonPath("$[0].traffic_status").value("MODERADA"))
                .andExpect(jsonPath("$[0].last_updated").value("2026-11-22T00:00:05Z"));
    }

    @Test
    void devuelveUnaZonaPorParametro() throws Exception {
        when(zoneStateService.findByZoneId("Zona A"))
                .thenReturn(Optional.of(new ZoneState("Zona A", 70, TrafficStatus.CONGESTIONADA, TS)));

        mockMvc.perform(get("/api/zones/detail").param("zoneId", "Zona A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traffic_status").value("CONGESTIONADA"));
    }

    @Test
    void responde404SiLaZonaNoExiste() throws Exception {
        when(zoneStateService.findByZoneId("Zona Z")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/zones/detail").param("zoneId", "Zona Z"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No hay estado para la zona 'Zona Z'"))
                .andExpect(jsonPath("$.path").value("/api/zones/detail"));
    }

    @Test
    void responde400SiFaltaElParametro() throws Exception {
        mockMvc.perform(get("/api/zones/detail"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Falta el parámetro obligatorio 'zoneId'"));
    }

    @Test
    void devuelveLasEstadisticas() throws Exception {
        when(statsService.currentStats()).thenReturn(new TrafficStats(
                150.256, 2, List.of("Zona A"),
                Map.of(TrafficStatus.CONGESTIONADA, 1L, TrafficStatus.MODERADA, 1L, TrafficStatus.DESPEJADA, 0L),
                TS));

        mockMvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_vehicles").value(150.26))
                .andExpect(jsonPath("$.congested_zones_count").value(1))
                .andExpect(jsonPath("$.congested_zones[0]").value("Zona A"))
                .andExpect(jsonPath("$.zones_by_status.DESPEJADA").value(0))
                .andExpect(jsonPath("$.waiting_for_data").value(false));
    }

    @Test
    void preflightCorsPermitidoParaUnOrigenConfigurado() throws Exception {
        mockMvc.perform(options("/api/zones")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5500")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5500"));
    }

    @Test
    void preflightCorsRechazadoParaUnOrigenDesconocido() throws Exception {
        mockMvc.perform(options("/api/zones")
                        .header(HttpHeaders.ORIGIN, "http://sitio-malicioso.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
