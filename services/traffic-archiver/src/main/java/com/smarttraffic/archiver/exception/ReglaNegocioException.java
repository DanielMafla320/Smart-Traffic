package com.smarttraffic.archiver.exception;

/**
 * Los datos violan una regla de negocio (equivale a un HTTP 400).
 * Ejemplo: cantidad de vehículos negativa o zona vacía.
 *
 * <p>No tiene sentido reintentar: el mismo dato volvería a fallar.
 */
public class ReglaNegocioException extends SmartTrafficException {

    public ReglaNegocioException(String message) {
        super(message);
    }

    public ReglaNegocioException(String message, Throwable cause) {
        super(message, cause);
    }
}
