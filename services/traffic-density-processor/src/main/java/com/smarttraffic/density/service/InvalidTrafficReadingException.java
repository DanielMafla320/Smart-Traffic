package com.smarttraffic.density.service;

/**
 * Indica que un evento consumido no cumple el contrato o contiene datos inválidos.
 */
public class InvalidTrafficReadingException extends RuntimeException {

    public InvalidTrafficReadingException(String message) {
        super(message);
    }
}
