package com.smarttraffic.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Cuerpo JSON uniforme para todas las respuestas de error de la API.
 */
public record ErrorResponse(
        @JsonProperty("timestamp") Instant timestamp,
        @JsonProperty("status") int status,
        @JsonProperty("error") String error,
        @JsonProperty("message") String message,
        @JsonProperty("path") String path) {
}
