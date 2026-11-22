package com.smarttraffic.dashboard.mapper;

import com.smarttraffic.dashboard.domain.TrafficStatus;
import com.smarttraffic.dashboard.domain.ZoneState;
import com.smarttraffic.dashboard.exception.ReglaNegocioException;
import com.smarttraffic.dashboard.messaging.TrafficUpdateMessage;
import com.smarttraffic.dashboard.messaging.TrafficUpdateMessage.ZoneUpdateMessage;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Implementación del adaptador mensaje de RabbitMQ → {@link ZoneState}.
 * El {@code timestamp} del mensaje se usa como última actualización de cada
 * zona, porque el procesador no envía una fecha por zona.
 */
@Component
public class TrafficUpdateMessageMapper implements TrafficUpdateMapper {

    @Override
    public List<ZoneState> toZoneStates(TrafficUpdateMessage message) {
        if (message == null || message.timestamp() == null) {
            throw new ReglaNegocioException("El mensaje de actualización no tiene timestamp");
        }
        if (message.zones() == null) {
            return List.of();
        }
        return message.zones().stream()
                .map(zone -> toZoneState(zone, message))
                .toList();
    }

    private ZoneState toZoneState(ZoneUpdateMessage zone, TrafficUpdateMessage message) {
        if (zone == null) {
            throw new ReglaNegocioException("El mensaje contiene una zona nula");
        }
        if (zone.averageVehicles() == null) {
            throw new ReglaNegocioException("La zona " + zone.zoneId() + " no tiene average_vehicles");
        }
        return new ZoneState(
                zone.zoneId(),
                zone.averageVehicles(),
                parseStatus(zone.trafficStatus()),
                message.timestamp());
    }

    private TrafficStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            throw new ReglaNegocioException("El traffic_status es obligatorio");
        }
        try {
            return TrafficStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ReglaNegocioException("Estado de tráfico desconocido: " + value);
        }
    }
}
