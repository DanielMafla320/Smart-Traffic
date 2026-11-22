package com.smarttraffic.dashboard.mapper;

import com.smarttraffic.dashboard.domain.ZoneState;
import com.smarttraffic.dashboard.messaging.TrafficUpdateMessage;
import java.util.List;

/**
 * Adapter: convierte el mensaje externo de RabbitMQ en objetos del dominio.
 */
public interface TrafficUpdateMapper {

    /**
     * @throws com.smarttraffic.dashboard.exception.ReglaNegocioException si el mensaje es inválido
     */
    List<ZoneState> toZoneStates(TrafficUpdateMessage message);
}
