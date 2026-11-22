package com.smarttraffic.archiver.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades {@code smarttraffic.kafka.*} de application.yml, validadas al
 * arrancar: si un valor es inválido, la aplicación no inicia.
 *
 * @param topic                   topic de lecturas crudas
 * @param partitions              particiones del topic si hay que crearlo
 * @param replicas                factor de replicación si hay que crearlo
 * @param retryIntervalMs         espera entre reintentos de errores inesperados
 * @param retryMaxAttempts        reintentos antes de descartar un error inesperado
 * @param databaseRetryIntervalMs espera entre reintentos cuando la base está caída
 */
@Validated
@ConfigurationProperties(prefix = "smarttraffic.kafka")
public record ArchiverKafkaProperties(
        @NotBlank String topic,
        @Min(1) int partitions,
        @Min(1) short replicas,
        @Positive long retryIntervalMs,
        @Min(0) long retryMaxAttempts,
        @Positive long databaseRetryIntervalMs) {
}
