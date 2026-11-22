package com.smarttraffic.archiver.service;

import com.smarttraffic.archiver.domain.TrafficReading;
import com.smarttraffic.archiver.dto.TrafficReadingEvent;

/**
 * Caso de uso: archivar una lectura de sensor en el historial.
 */
public interface TrafficArchiveService {

    /**
     * Guarda la lectura en PostgreSQL.
     *
     * @return la lectura guardada, con su id asignado por la base de datos
     * @throws com.smarttraffic.archiver.exception.ReglaNegocioException si los datos son inválidos
     * @throws com.smarttraffic.archiver.exception.EventoDuplicadoException si el event_id ya existe
     * @throws org.springframework.dao.DataAccessException si la base de datos no está disponible
     */
    TrafficReading archive(TrafficReadingEvent event);
}
