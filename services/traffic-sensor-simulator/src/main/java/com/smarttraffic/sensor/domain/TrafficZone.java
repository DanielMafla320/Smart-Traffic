package com.smarttraffic.sensor.domain;

/**
 * Zona de tráfico configurable para la que se generan lecturas.
 */
public record TrafficZone(String id) {

    public TrafficZone {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El identificador de la zona no puede estar vacío");
        }
        id = id.trim();
    }
}
