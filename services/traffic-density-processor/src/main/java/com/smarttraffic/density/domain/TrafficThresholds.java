package com.smarttraffic.density.domain;

/**
 * Umbrales (en cantidad promedio de vehículos) que separan los estados de tráfico.
 *
 * <p>Los valores concretos son configurables y provisionales; deben validarse
 * con el equipo y con datos reales. Los límites se consideran inclusivos:
 * {@code average >= moderada} es MODERADA y {@code average >= congestionada} es
 * CONGESTIONADA.
 */
public record TrafficThresholds(double moderada, double congestionada) {

    public TrafficThresholds {
        if (moderada < 0) {
            throw new IllegalArgumentException("El umbral moderado no puede ser negativo");
        }
        if (congestionada <= moderada) {
            throw new IllegalArgumentException("El umbral de congestión debe ser mayor que el moderado");
        }
    }
}
