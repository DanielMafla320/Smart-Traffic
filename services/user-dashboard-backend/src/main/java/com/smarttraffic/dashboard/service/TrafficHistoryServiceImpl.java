package com.smarttraffic.dashboard.service;

import com.smarttraffic.dashboard.config.DashboardProperties;
import com.smarttraffic.dashboard.domain.TrafficReadingHistory;
import com.smarttraffic.dashboard.exception.ReglaNegocioException;
import com.smarttraffic.dashboard.repository.TrafficHistoryRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

@Service
public class TrafficHistoryServiceImpl implements TrafficHistoryService {

    private final TrafficHistoryRepository repository;
    private final DashboardProperties.History limits;
    private final Clock clock;

    public TrafficHistoryServiceImpl(
            TrafficHistoryRepository repository, DashboardProperties properties, Clock clock) {
        this.repository = repository;
        this.limits = properties.history();
        this.clock = clock;
    }

    @Override
    public List<TrafficReadingHistory> findHistory(String zoneId, Instant from, Instant to) {
        if (zoneId == null || zoneId.isBlank()) {
            throw new ReglaNegocioException("El parámetro zoneId es obligatorio");
        }
        Instant end = to != null ? to : clock.instant();
        Instant start = from != null ? from : end.minus(limits.defaultRange());

        if (!start.isBefore(end)) {
            throw new ReglaNegocioException("La fecha 'from' debe ser anterior a 'to'");
        }
        if (Duration.between(start, end).compareTo(limits.maxRange()) > 0) {
            throw new ReglaNegocioException(
                    "El rango máximo de consulta es " + limits.maxRange().toHours() + " horas");
        }

        return repository.findByZoneIdAndReadingTimestampBetweenOrderByReadingTimestampDesc(
                zoneId.trim(), start, end, Limit.of(limits.maxResults()));
    }
}
