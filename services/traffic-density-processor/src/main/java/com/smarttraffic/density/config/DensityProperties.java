package com.smarttraffic.density.config;

import com.smarttraffic.density.domain.TrafficThresholds;
import com.smarttraffic.density.strategy.DensityStrategy;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración de la ventana de lecturas, la estrategia y los umbrales de estado.
 *
 * <p>La ventana combina dos límites: como máximo {@code windowSize} lecturas y,
 * además, solo las lecturas con antigüedad menor o igual a {@code windowDuration}
 * respecto a la lectura más reciente de la zona.
 *
 * <p>Los umbrales iniciales (30 y 60 vehículos promedio) son provisionales y
 * configurables; no provienen de una fórmula oficial del enunciado.
 */
@Validated
@ConfigurationProperties(prefix = "smarttraffic.density")
public record DensityProperties(
        @NotNull Strategy strategy,
        @Min(1) int windowSize,
        @NotNull Duration windowDuration,
        @DecimalMin("0.0") double moderadaThreshold,
        @DecimalMin("0.0") double congestionadaThreshold) {

    /** Ventana temporal por defecto: lecturas de los últimos 60 segundos. */
    public static final Duration DEFAULT_WINDOW_DURATION = Duration.ofSeconds(60);

    /**
     * Indica si la ventana temporal está activa. Con duración {@code 0} solo se
     * aplica el límite por cantidad de lecturas.
     */
    public boolean timeWindowEnabled() {
        return windowDuration != null && !windowDuration.isZero();
    }

    @AssertTrue(message = "window-duration no puede ser negativa")
    public boolean isWindowDurationValid() {
        return windowDuration == null || !windowDuration.isNegative();
    }

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
