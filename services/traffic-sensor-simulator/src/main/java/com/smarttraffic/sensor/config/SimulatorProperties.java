package com.smarttraffic.sensor.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración externa del simulador.
 *
 * <p>Los valores por defecto se encuentran en {@code application.yml}. El intervalo
 * también se utiliza en la anotación {@code @Scheduled} mediante su propiedad.
 */
@Validated
@ConfigurationProperties(prefix = "smarttraffic.simulator")
public record SimulatorProperties(
        @NotEmpty List<@NotBlank String> zones,
        @Min(0) int vehicleCountMin,
        @Min(0) int vehicleCountMax,
        @Positive long intervalMs) {

    @AssertTrue(message = "vehicle-count-max debe ser mayor o igual que vehicle-count-min")
    public boolean isVehicleRangeValid() {
        return vehicleCountMax >= vehicleCountMin;
    }
}
