package com.smarttraffic.density.config;

import com.smarttraffic.density.domain.TrafficThresholds;
import com.smarttraffic.density.strategy.DensityStrategy;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración de la ventana de lecturas, la estrategia y los umbrales de estado.
 *
 * <p>Los umbrales iniciales (30 y 60 vehículos promedio) son provisionales y
 * configurables; no provienen de una fórmula oficial del enunciado.
 */
@Validated
@ConfigurationProperties(prefix = "smarttraffic.density")
public record DensityProperties(
        @NotNull Strategy strategy,
        @Min(1) int windowSize,
        @DecimalMin("0.0") double moderadaThreshold,
        @DecimalMin("0.0") double congestionadaThreshold) {

    public enum Strategy {
        SIMPLE_AVERAGE,
        WEIGHTED_AVERAGE
    }

    @AssertTrue(message = "congestionada-threshold debe ser mayor que moderada-threshold")
    public boolean isThresholdOrderValid() {
        return congestionadaThreshold > moderadaThreshold;
    }

    public TrafficThresholds toThresholds() {
        return new TrafficThresholds(moderadaThreshold, congestionadaThreshold);
    }
}
