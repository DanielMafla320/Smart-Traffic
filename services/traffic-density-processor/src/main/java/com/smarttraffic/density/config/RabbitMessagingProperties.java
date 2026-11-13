package com.smarttraffic.density.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración de la publicación del estado agregado en RabbitMQ.
 */
@Validated
@ConfigurationProperties(prefix = "smarttraffic.rabbit")
public record RabbitMessagingProperties(
        @NotBlank String exchange,
        @Positive long publishIntervalMs) {
}
