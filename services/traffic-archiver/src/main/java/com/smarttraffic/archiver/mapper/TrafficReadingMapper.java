package com.smarttraffic.archiver.mapper;

import com.smarttraffic.archiver.domain.TrafficReading;
import com.smarttraffic.archiver.dto.TrafficReadingEvent;

/**
 * Contrato para convertir el mensaje de Kafka en la entidad de dominio.
 *
 * <p>Patrón Adapter: adapta la interfaz del mensaje externo (DTO con nombres
 * del JSON) a la interfaz que espera el dominio. Si el contrato de Kafka
 * cambia, solo cambia la implementación de esta interfaz.
 */
public interface TrafficReadingMapper {

    TrafficReading toEntity(TrafficReadingEvent event);
}
