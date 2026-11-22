package com.smarttraffic.archiver.exception;

import java.util.UUID;

/**
 * El evento ya estaba guardado (equivale a un HTTP 409 Conflict).
 *
 * <p>Ocurre porque Kafka entrega "al menos una vez": tras un reinicio puede
 * volver a entregar un mensaje ya procesado. No es un fallo del sistema.
 */
public class EventoDuplicadoException extends SmartTrafficException {

    private final UUID eventId;

    public EventoDuplicadoException(UUID eventId) {
        super("El evento " + eventId + " ya está registrado en el historial");
        this.eventId = eventId;
    }

    public UUID getEventId() {
        return eventId;
    }
}
