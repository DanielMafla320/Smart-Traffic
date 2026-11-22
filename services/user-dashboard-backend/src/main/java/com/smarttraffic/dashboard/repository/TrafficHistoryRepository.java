package com.smarttraffic.dashboard.repository;

import com.smarttraffic.dashboard.domain.TrafficReadingHistory;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.repository.Repository;

/**
 * Acceso de solo lectura al historial.
 *
 * <p>Extiende {@link Repository} (no {@code JpaRepository}) a propósito: así
 * solo existe el método de consulta declarado aquí y no hay {@code save} ni
 * {@code delete} disponibles (ISP: se expone solo lo que se necesita).
 */
public interface TrafficHistoryRepository extends Repository<TrafficReadingHistory, Long> {

    /**
     * Spring Data genera:
     * {@code SELECT ... WHERE zone_id = ? AND reading_timestamp BETWEEN ? AND ?
     * ORDER BY reading_timestamp DESC LIMIT ?}, que usa el índice
     * {@code idx_traffic_readings_zone_timestamp}.
     */
    List<TrafficReadingHistory> findByZoneIdAndReadingTimestampBetweenOrderByReadingTimestampDesc(
            String zoneId, Instant from, Instant to, Limit limit);
}
