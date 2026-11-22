package com.smarttraffic.dashboard.exception;

import org.springframework.http.HttpStatus;

/**
 * El recurso pedido no existe: HTTP 404 Not Found.
 * Ejemplo: consultar una zona de la que todavía no se recibió estado.
 */
public class RecursoNoEncontradoException extends SmartTrafficException {

    public RecursoNoEncontradoException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
