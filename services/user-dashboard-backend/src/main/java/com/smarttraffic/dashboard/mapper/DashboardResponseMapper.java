package com.smarttraffic.dashboard.mapper;

import com.smarttraffic.dashboard.domain.TrafficReadingHistory;
import com.smarttraffic.dashboard.domain.TrafficStats;
import com.smarttraffic.dashboard.domain.ZoneState;
import com.smarttraffic.dashboard.dto.HistoryReadingResponse;
import com.smarttraffic.dashboard.dto.TrafficStatsResponse;
import com.smarttraffic.dashboard.dto.ZoneStatusResponse;

/**
 * Convierte objetos del dominio en DTOs de respuesta. Las entidades JPA nunca
 * salen del backend: el frontend solo ve estos records.
 */
public interface DashboardResponseMapper {

    ZoneStatusResponse toResponse(ZoneState zone);

    TrafficStatsResponse toResponse(TrafficStats stats);

    HistoryReadingResponse toResponse(TrafficReadingHistory reading);
}
