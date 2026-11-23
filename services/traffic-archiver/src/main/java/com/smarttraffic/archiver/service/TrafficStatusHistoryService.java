package com.smarttraffic.archiver.service;

import com.smarttraffic.archiver.domain.TrafficStatusChange;
import com.smarttraffic.archiver.dto.TrafficUpdateMessage;
import java.util.List;

/**
 * Caso de uso: guardar en el historial los cambios de estado de congestión.
 */
public interface TrafficStatusHistoryService {

    /**
     * Compara cada zona del snapshot con su último estado conocido y guarda una
     * fila solo cuando el estado cambió (o la primera vez que aparece la zona).
     * Una zona con datos inválidos se descarta con log sin afectar a las demás.
     *
     * @return los cambios guardados (vacío si ninguna zona cambió)
     * @throws com.smarttraffic.archiver.exception.ReglaNegocioException si el snapshot es inválido
     * @throws org.springframework.dao.DataAccessException si la base de datos no está disponible
     */
    List<TrafficStatusChange> recordChanges(TrafficUpdateMessage message);
}
