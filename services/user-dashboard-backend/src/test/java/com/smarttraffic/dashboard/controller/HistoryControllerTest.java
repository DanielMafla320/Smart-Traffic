package com.smarttraffic.dashboard.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smarttraffic.dashboard.domain.TrafficReadingHistory;
import com.smarttraffic.dashboard.exception.ReglaNegocioException;
import com.smarttraffic.dashboard.mapper.DashboardResponseMapperImpl;
import com.smarttraffic.dashboard.service.TrafficHistoryService;
import com.smarttraffic.dashboard.service.TrafficStatsService;
import com.smarttraffic.dashboard.service.ZoneStateService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = HistoryController.class)
@Import(DashboardResponseMapperImpl.class)
class HistoryControllerTest {

    private static final Instant FROM = Instant.parse("2026-11-22T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-11-22T01:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrafficHistoryService historyService;
    @MockitoBean
    private ZoneStateService zoneStateService;
    @MockitoBean
    private TrafficStatsService statsService;

    @Test
    void devuelveElHistorialComoDto() throws Exception {
        UUID eventId = UUID.fromString("f33df0e7-b180-4a75-9561-569ff24f62eb");
        when(historyService.findHistory("Zona A", FROM, TO)).thenReturn(List.of(
                new TrafficReadingHistory(7L, eventId, "Zona A", 61,
                        Instant.parse("2026-11-22T00:30:00Z"), Instant.parse("2026-11-22T00:30:01Z"))));

        mockMvc.perform(get("/api/history")
                        .param("zoneId", "Zona A")
                        .param("from", "2026-11-22T00:00:00Z")
                        .param("to", "2026-11-22T01:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].event_id").value(eventId.toString()))
                .andExpect(jsonPath("$[0].vehicle_count").value(61))
                .andExpect(jsonPath("$[0].reading_timestamp").value("2026-11-22T00:30:00Z"))
                // El id interno de la entidad no se expone.
                .andExpect(jsonPath("$[0].id").doesNotExist());
    }

    @Test
    void lasFechasSonOpcionales() throws Exception {
        when(historyService.findHistory(eq("Zona A"), isNull(), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/history").param("zoneId", "Zona A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void responde400SiElRangoEsInvalido() throws Exception {
        when(historyService.findHistory(eq("Zona A"), any(), any()))
                .thenThrow(new ReglaNegocioException("La fecha 'from' debe ser anterior a 'to'"));

        mockMvc.perform(get("/api/history")
                        .param("zoneId", "Zona A")
                        .param("from", "2026-11-22T01:00:00Z")
                        .param("to", "2026-11-22T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha 'from' debe ser anterior a 'to'"));
    }

    @Test
    void responde400SiLaFechaEstaMalEscrita() throws Exception {
        mockMvc.perform(get("/api/history").param("zoneId", "Zona A").param("from", "ayer"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void responde503SiLaBaseNoEstaDisponible() throws Exception {
        when(historyService.findHistory(eq("Zona A"), any(), any()))
                .thenThrow(new DataAccessResourceFailureException("Connection refused"));

        mockMvc.perform(get("/api/history").param("zoneId", "Zona A"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }
}
