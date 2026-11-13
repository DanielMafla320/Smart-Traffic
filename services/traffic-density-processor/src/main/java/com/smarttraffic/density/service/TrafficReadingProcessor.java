package com.smarttraffic.density.service;

import com.smarttraffic.density.domain.TrafficReadingEvent;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Valida un evento y actualiza el estado de su zona. Un evento inválido se rechaza
 * con {@link InvalidTrafficReadingException} antes de tocar el estado en memoria.
 */
@Service
public class TrafficReadingProcessor {

    private static final Logger log = LoggerFactory.getLogger(TrafficReadingProcessor.class);

    private final Validator validator;
    private final ZoneStateStore zoneStateStore;

    public TrafficReadingProcessor(Validator validator, ZoneStateStore zoneStateStore) {
        this.validator = validator;
        this.zoneStateStore = zoneStateStore;
    }

    public void process(TrafficReadingEvent event) {
        if (event == null) {
            throw new InvalidTrafficReadingException("El evento de tráfico es obligatorio");
        }

        Set<ConstraintViolation<TrafficReadingEvent>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            throw new InvalidTrafficReadingException(
                    "Evento de tráfico inválido: " + formatViolations(violations));
        }

        zoneStateStore.record(event.zoneId(), event.vehicleCount(), event.timestamp());
        log.debug("Lectura {} procesada para la zona {}",
                event.eventId(), event.zoneId());
    }

    private String formatViolations(Set<ConstraintViolation<TrafficReadingEvent>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
