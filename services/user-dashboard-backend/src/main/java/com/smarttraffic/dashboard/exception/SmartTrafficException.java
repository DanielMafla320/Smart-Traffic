package com.smarttraffic.dashboard.exception;

import org.springframework.http.HttpStatus;

/**
 * Raíz de la jerarquía de excepciones propias de SmartTraffic.
 *
 * <p>Cada subclase declara qué código HTTP le corresponde, así el
 * {@code GlobalExceptionHandler} no necesita un {@code if} por cada tipo (OCP):
 * para un error nuevo basta con crear otra subclase.
 */
public abstract class SmartTrafficException extends RuntimeException {

    protected SmartTrafficException(String message) {
        super(message);
    }

    protected SmartTrafficException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Código HTTP con el que se responde al cliente. */
    public abstract HttpStatus getStatus();
}
