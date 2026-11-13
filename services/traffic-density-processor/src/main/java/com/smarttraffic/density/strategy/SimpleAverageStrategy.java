package com.smarttraffic.density.strategy;

import java.util.List;

/**
 * Promedio aritmético simple de la ventana de lecturas.
 */
public class SimpleAverageStrategy implements DensityStrategy {

    @Override
    public double calculate(List<Integer> recentReadings) {
        DensityStrategy.validateReadings(recentReadings);
        return recentReadings.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElseThrow();
    }
}
