package com.smarttraffic.archiver.repository;

import com.smarttraffic.archiver.domain.TrafficReading;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Contrato de acceso a la tabla {@code traffic_readings_history}.
 *
 * <p>Spring Data JPA genera la implementación en tiempo de ejecución: el
 * servicio depende de esta interfaz, nunca de una clase concreta (DIP).
 */
public interface TrafficReadingRepository extends JpaRepository<TrafficReading, Long> {

    /**
     * Spring Data deduce la consulta a partir del nombre del método:
     * {@code SELECT count(*) > 0 FROM traffic_readings_history WHERE event_id = ?}.
     * Usa el índice único de event_id, por lo que es rápida.
     */
    boolean existsByEventId(UUID eventId);
}
