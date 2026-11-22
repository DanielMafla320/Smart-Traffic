package com.smarttraffic.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Mensaje que se envía por WebSocket en cada actualización: el estado de las
 * zonas y el resumen, para que el frontend no tenga que llamar a la API.
 */
public record DashboardUpdateResponse(
        @JsonProperty("type") String type,
        @JsonProperty("zones") List<ZoneStatusResponse> zones,
        @JsonProperty("stats") TrafficStatsResponse stats) {

    public static final String TYPE_TRAFFIC_UPDATE = "traffic_update";

    public DashboardUpdateResponse {
        zones = List.copyOf(zones);
    }
}
