package com.smarttraffic.archiver.consumer;

import com.smarttraffic.archiver.domain.TrafficReading;
import com.smarttraffic.archiver.dto.TrafficReadingEvent;
import com.smarttraffic.archiver.exception.EventoDuplicadoException;
import com.smarttraffic.archiver.exception.ReglaNegocioException;
import com.smarttraffic.archiver.service.TrafficArchiveService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.log.LogAccessor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.SerializationUtils;
import org.springframework.stereotype.Component;

/**
 * Punto de entrada de los mensajes de Kafka. Solo traduce el registro de Kafka
 * en una llamada al servicio y decide qué errores se descartan.
 *
 * <ul>
 *   <li>Duplicado: se ignora (es normal con entrega "al menos una vez").</li>
 *   <li>Datos inválidos: se descartan con log.</li>
 *   <li>Base de datos caída: la excepción sube al manejador de errores de
 *       Kafka, que reintenta el mismo registro.</li>
 * </ul>
 */
@Component
public class RawTrafficDataListener {

    private static final Logger log = LoggerFactory.getLogger(RawTrafficDataListener.class);

    private final TrafficArchiveService archiveService;

    public RawTrafficDataListener(TrafficArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @KafkaListener(
            topics = "${smarttraffic.kafka.topic:raw_traffic_data}",
            groupId = "${spring.kafka.consumer.group-id:traffic-archiver}")
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

        try {
            TrafficReading saved = archiveService.archive(event);
            log.info("Lectura archivada id={} zona={} vehículos={} (partition={}, offset={})",
                    saved.getId(), saved.getZoneId(), saved.getVehicleCount(),
                    record.partition(), record.offset());
        } catch (EventoDuplicadoException exception) {
            log.info("Evento duplicado ignorado: {} (partition={}, offset={})",
                    exception.getEventId(), record.partition(), record.offset());
        } catch (ReglaNegocioException exception) {
            log.warn("Registro descartado por datos inválidos (topic={}, partition={}, offset={}): {}",
                    record.topic(), record.partition(), record.offset(), exception.getMessage());
        }
    }
}
