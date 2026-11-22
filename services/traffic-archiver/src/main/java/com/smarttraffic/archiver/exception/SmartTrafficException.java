package com.smarttraffic.archiver.exception;

/**
 * Raíz de la jerarquía de excepciones propias de SmartTraffic.
 *
 * <p>Es abstracta: nunca se lanza directamente, sino una subclase que indica
 * el tipo de problema. Así se pueden capturar todos los errores del dominio
 * con un solo {@code catch (SmartTrafficException e)} o tratarlos por separado.
 */
public abstract class SmartTrafficException extends RuntimeException {

    protected SmartTrafficException(String message) {
        super(message);
    }

    protected SmartTrafficException(String message, Throwable cause) {
        super(message, cause);
    }
}
