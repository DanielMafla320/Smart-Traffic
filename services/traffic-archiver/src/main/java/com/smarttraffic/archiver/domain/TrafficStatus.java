package com.smarttraffic.archiver.domain;

import com.smarttraffic.archiver.exception.ReglaNegocioException;
import java.util.Locale;

/**
 * Estados de congestión que publica traffic-density-processor.
 */
public enum TrafficStatus {
    DESPEJADA,
    MODERADA,
    CONGESTIONADA;

    /**
     * Convierte el texto del mensaje RabbitMQ en un estado.
     *
     * @throws ReglaNegocioException si el texto no corresponde a un estado conocido
     */
    public static TrafficStatus fromText(String value) {
        if (value == null || value.isBlank()) {
            throw new ReglaNegocioException("El estado de tráfico es obligatorio");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ReglaNegocioException("Estado de tráfico desconocido: " + value, exception);
        }
    }
}
