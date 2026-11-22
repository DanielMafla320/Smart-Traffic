package com.smarttraffic.archiver.service;

import com.smarttraffic.archiver.domain.TrafficReading;
import com.smarttraffic.archiver.dto.TrafficReadingEvent;
import com.smarttraffic.archiver.exception.EventoDuplicadoException;
import com.smarttraffic.archiver.exception.ReglaNegocioException;
import com.smarttraffic.archiver.mapper.TrafficReadingMapper;
import com.smarttraffic.archiver.repository.TrafficReadingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Implementación del archivado. Depende solo de interfaces (mapper y
 * repositorio), inyectadas por constructor.
 */
@Service
public class TrafficArchiveServiceImpl implements TrafficArchiveService {

    private static final Logger log = LoggerFactory.getLogger(TrafficArchiveServiceImpl.class);

    private final TrafficReadingRepository repository;
    private final TrafficReadingMapper mapper;

    public TrafficArchiveServiceImpl(TrafficReadingRepository repository, TrafficReadingMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public TrafficReading archive(TrafficReadingEvent event) {
        // 1. Adaptar y validar (lanza ReglaNegocioException si algo es inválido).
        TrafficReading reading = mapper.toEntity(event);

        // 2. Camino normal de un duplicado: comprobarlo antes de insertar.
        if (repository.existsByEventId(reading.getEventId())) {
            throw new EventoDuplicadoException(reading.getEventId());
        }

        // 3. Insertar. La restricción UNIQUE de la base es la última defensa
        //    por si el mismo evento se insertó entre el paso 2 y el 3.
        try {
            TrafficReading saved = repository.save(reading);
            log.debug("Lectura archivada: id={}, eventId={}, zona={}, vehículos={}",
                    saved.getId(), saved.getEventId(), saved.getZoneId(), saved.getVehicleCount());
            return saved;
        } catch (DataIntegrityViolationException exception) {
            if (repository.existsByEventId(reading.getEventId())) {
                throw new EventoDuplicadoException(reading.getEventId());
            }
            throw new ReglaNegocioException(
                    "La lectura " + reading.getEventId() + " viola una restricción de la base de datos",
                    exception);
        }
    }
}
