package com.smarttraffic.density.consumer;

import com.smarttraffic.density.domain.TrafficReadingEvent;
import com.smarttraffic.density.service.InvalidTrafficReadingException;
import com.smarttraffic.density.service.TrafficReadingProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.log.LogAccessor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.SerializationUtils;
import org.springframework.stereotype.Component;

/**
 * Consumidor de {@code raw_traffic_data}. Los errores de deserialización llegan
 * como valor nulo y los eventos inválidos se descartan con un log; los errores
 * inesperados se delegan al {@code CommonErrorHandler} configurado en Kafka.
 */
@Component
public class RawTrafficDataListener {

    private static final Logger log = LoggerFactory.getLogger(RawTrafficDataListener.class);

    private final TrafficReadingProcessor processor;

    public RawTrafficDataListener(TrafficReadingProcessor processor) {
        this.processor = processor;
    }

    @KafkaListener(
            topics = "${smarttraffic.kafka.topic:raw_traffic_data}",
            groupId = "${spring.kafka.consumer.group-id:traffic-density-processor}")
    public void onMessage(ConsumerRecord<String, TrafficReadingEvent> record) {
        TrafficReadingEvent event = record.value();
        if (event == null) {
            DeserializationException deserializationException = SerializationUtils.getExceptionFromHeader(
                    record,
                    SerializationUtils.VALUE_DESERIALIZER_EXCEPTION_HEADER,
                    new LogAccessor(RawTrafficDataListener.class));
            log.error("Registro descartado: no fue posible deserializar el valor "
                            + "(topic={}, partition={}, offset={})",
                    record.topic(), record.partition(), record.offset(), deserializationException);
            return;
        }

        if (record.key() != null && !record.key().equals(event.zoneId())) {
            log.warn("La clave Kafka {} no coincide con zone_id {} (offset={}); "
                            + "se procesa el zone_id del payload",
                    record.key(), event.zoneId(), record.offset());
        }

        try {
            processor.process(event);
        } catch (InvalidTrafficReadingException exception) {
            log.warn("Registro descartado por datos inválidos "
                            + "(topic={}, partition={}, offset={}): {}",
                    record.topic(), record.partition(), record.offset(), exception.getMessage());
        }
    }
}
