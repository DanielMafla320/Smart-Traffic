package com.smarttraffic.sensor.producer;

import com.smarttraffic.sensor.domain.SensorReadingRecord;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica lecturas validadas en Kafka. Los errores de envío se registran y no se
 * propagan para que una lectura fallida no interrumpa la simulación.
 */
@Component
public class TrafficSensorProducer {

    private static final Logger log = LoggerFactory.getLogger(TrafficSensorProducer.class);

    private final KafkaTemplate<String, SensorReadingRecord> kafkaTemplate;
    private final Validator validator;
    private final String topic;

    public TrafficSensorProducer(
            KafkaTemplate<String, SensorReadingRecord> kafkaTemplate,
            Validator validator,
            @Value("${smarttraffic.kafka.topic:raw_traffic_data}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.validator = validator;
        this.topic = topic;
    }

    public void publish(SensorReadingRecord reading) {
        if (reading == null) {
            log.error("Se intentó publicar una lectura nula");
            return;
        }

        Set<ConstraintViolation<SensorReadingRecord>> violations = validator.validate(reading);
        if (!violations.isEmpty()) {
            log.warn("Lectura descartada por datos inválidos (zone={}): {}",
                    reading.zoneId(), formatViolations(violations));
            return;
        }

        try {
            kafkaTemplate.send(topic, reading.zoneId(), reading)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.error("Error asíncrono al publicar la lectura {} de la zona {} en el topic {}",
                                    reading.eventId(), reading.zoneId(), topic, error);
                        } else {
                            log.debug("Lectura {} de la zona {} publicada en el topic {}",
                                    reading.eventId(), reading.zoneId(), topic);
                        }
                    });
        } catch (RuntimeException exception) {
            log.error("No fue posible enviar la lectura {} de la zona {} al topic {}",
                    reading.eventId(), reading.zoneId(), topic, exception);
        }
    }

    private String formatViolations(Set<ConstraintViolation<SensorReadingRecord>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
