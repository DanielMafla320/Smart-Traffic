package com.smarttraffic.archiver.repository;

import com.smarttraffic.archiver.domain.TrafficStatusChange;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Contrato de acceso a la tabla {@code traffic_status_history}.
 */
public interface TrafficStatusChangeRepository extends JpaRepository<TrafficStatusChange, Long> {

    /**
     * Último cambio registrado de una zona. Se usa tras un reinicio para no
     * repetir en la base un estado que ya estaba guardado.
     */
    Optional<TrafficStatusChange> findFirstByZoneIdOrderByStatusTimestampDescIdDesc(String zoneId);
}
