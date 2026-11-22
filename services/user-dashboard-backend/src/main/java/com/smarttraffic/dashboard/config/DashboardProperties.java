package com.smarttraffic.dashboard.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades {@code smarttraffic.dashboard.*}, validadas al arrancar.
 *
 * @param exchange       exchange fanout de RabbitMQ publicado por el procesador
 * @param queue          cola propia del dashboard vinculada al exchange
 * @param queueMaxLength mensajes máximos retenidos en la cola
 * @param allowedOrigins orígenes permitidos para CORS y WebSocket
 * @param history        límites de la consulta de historial
 */
@Validated
@ConfigurationProperties(prefix = "smarttraffic.dashboard")
public record DashboardProperties(
        @NotBlank String exchange,
        @NotBlank String queue,
        @Min(1) int queueMaxLength,
        @NotEmpty List<@NotBlank String> allowedOrigins,
        @Valid @NotNull History history) {

    /**
     * @param defaultRange rango usado si no se envía {@code from}
     * @param maxRange     rango máximo permitido entre {@code from} y {@code to}
     * @param maxResults   filas máximas devueltas por consulta
     */
    public record History(
            @NotNull Duration defaultRange,
            @NotNull Duration maxRange,
            @Min(1) @Max(5000) int maxResults) {
    }

    public String[] allowedOriginsArray() {
        return allowedOrigins.toArray(String[]::new);
    }
}
