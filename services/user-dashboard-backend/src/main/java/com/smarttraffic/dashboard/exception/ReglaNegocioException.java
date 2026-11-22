package com.smarttraffic.dashboard.exception;

import org.springframework.http.HttpStatus;

/**
 * La petición o los datos violan una regla de negocio: HTTP 400 Bad Request.
 * Ejemplo: rango de fechas con {@code from} posterior a {@code to}.
 */
public class ReglaNegocioException extends SmartTrafficException {

    public ReglaNegocioException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
