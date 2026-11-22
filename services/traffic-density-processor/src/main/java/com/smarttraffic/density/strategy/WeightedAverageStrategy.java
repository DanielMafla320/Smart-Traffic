package com.smarttraffic.density.strategy;

import java.util.List;

/**
 * Promedio ponderado que da más importancia a las lecturas recientes.
 *
 * <p>Para una ventana {@code [v0, v1, ..., vn-1]} ordenada de más antigua a más
 * reciente se usan pesos {@code 1, 2, ..., n}. La lectura más nueva tiene el peso
 * más alto. Fórmula: {@code sum(vi * (i + 1)) / sum(i + 1)}.
 */
public class WeightedAverageStrategy implements DensityStrategy {

    @Override
    public double calculate(List<Integer> recentReadings) {
        DensityStrategy.validateReadings(recentReadings);

        double weightedSum = 0;
        double weightSum = 0;
        for (int i = 0; i < recentReadings.size(); i++) {
            double weight = i + 1.0;
            weightedSum += recentReadings.get(i) * weight;
            weightSum += weight;
        }
        return weightedSum / weightSum;
    }
}
