package com.smarttraffic.dashboard.service;

import com.smarttraffic.dashboard.domain.TrafficReadingHistory;
import java.time.Instant;
import java.util.List;

/**
 * Consulta del historial de lecturas guardado en PostgreSQL.
 */
public interface TrafficHistoryService {

    /**
     * Lecturas de una zona en un rango de fechas, de la más reciente a la más antigua.
     *
     * @param zoneId zona obligatoria, por ejemplo "Zona A"
     * @param from   inicio del rango; si es null se usa {@code to - rango por defecto}
     * @param to     fin del rango; si es null se usa el instante actual
     * @throws com.smarttraffic.dashboard.exception.ReglaNegocioException si los filtros son inválidos
     */
    List<TrafficReadingHistory> findHistory(String zoneId, Instant from, Instant to);
}
