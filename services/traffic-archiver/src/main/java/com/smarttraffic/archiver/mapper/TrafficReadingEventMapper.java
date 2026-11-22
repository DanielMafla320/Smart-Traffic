package com.smarttraffic.archiver.mapper;

import com.smarttraffic.archiver.domain.TrafficReading;
import com.smarttraffic.archiver.dto.TrafficReadingEvent;
import com.smarttraffic.archiver.exception.ReglaNegocioException;
import org.springframework.stereotype.Component;

/**
 * Adapta un {@link TrafficReadingEvent} de Kafka a la entidad {@link TrafficReading}.
 * Las validaciones las hace la propia entidad en {@link TrafficReading#registrar}.
 */
@Component
public class TrafficReadingEventMapper implements TrafficReadingMapper {

    @Override
    public TrafficReading toEntity(TrafficReadingEvent event) {
        if (event == null) {
            throw new ReglaNegocioException("El evento de tráfico es obligatorio");
        }
        return TrafficReading.registrar(
                event.eventId(),
                event.zoneId(),
                event.vehicleCount(),
                event.timestamp());
    }
}
