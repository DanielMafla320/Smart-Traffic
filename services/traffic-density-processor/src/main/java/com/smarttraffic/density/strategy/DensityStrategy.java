package com.smarttraffic.density.strategy;

import java.util.List;

/**
 * Estrategia de cálculo de densidad. Recibe lecturas ordenadas de más antigua a
 * más reciente y devuelve la cantidad promedio de vehículos.
 */
public interface DensityStrategy {

    double calculate(List<Integer> recentReadings);

    /**
     * Valida la ventana común a todas las estrategias.
     *
     * @throws IllegalArgumentException si la lista es nula, está vacía o contiene
     *     valores nulos o negativos
     */
    static void validateReadings(List<Integer> recentReadings) {
        if (recentReadings == null || recentReadings.isEmpty()) {
            throw new IllegalArgumentException("Se requiere al menos una lectura para calcular la densidad");
        }
        for (Integer reading : recentReadings) {
            if (reading == null) {
                throw new IllegalArgumentException("La ventana de lecturas no puede contener valores nulos");
            }
            if (reading < 0) {
                throw new IllegalArgumentException(
                        "La ventana de lecturas no puede contener valores negativos: " + reading);
            }
        }
    }
}
