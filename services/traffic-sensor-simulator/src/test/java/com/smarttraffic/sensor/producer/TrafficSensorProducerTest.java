package com.smarttraffic.sensor.producer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.smarttraffic.sensor.domain.SensorReadingRecord;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class TrafficSensorProducerTest {

    @Mock
    private KafkaTemplate<String, SensorReadingRecord> kafkaTemplate;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private TrafficSensorProducer producer;

    @BeforeEach
    void setUp() {
        producer = new TrafficSensorProducer(kafkaTemplate, validator, "raw_traffic_data");
    }

    @Test
    void publicaLaLecturaUsandoLaZonaComoClave() {
        SensorReadingRecord reading = validReading();
        when(kafkaTemplate.send(anyString(), anyString(), any(SensorReadingRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        producer.publish(reading);

        verify(kafkaTemplate).send("raw_traffic_data", "Zona A", reading);
    }

    @Test
    void descartaLecturasInvalidasSinPublicarlas() {
        producer.publish(new SensorReadingRecord(
                UUID.randomUUID(), "Zona A", -5, Instant.now()));

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void descartaLecturasNulas() {
        producer.publish(null);

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void registraUnErrorSincronoDePublicacionSinPropagarlo() {
        SensorReadingRecord reading = validReading();
        when(kafkaTemplate.send(anyString(), anyString(), any(SensorReadingRecord.class)))
                .thenThrow(new KafkaException("broker no disponible"));

        assertThatCode(() -> producer.publish(reading)).doesNotThrowAnyException();

        verify(kafkaTemplate).send("raw_traffic_data", "Zona A", reading);
    }

    @Test
    void registraUnErrorAsincronoDePublicacionSinPropagarlo() {
        SensorReadingRecord reading = validReading();
        CompletableFuture<SendResult<String, SensorReadingRecord>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("timeout"));
        when(kafkaTemplate.send(eq("raw_traffic_data"), eq("Zona A"), eq(reading)))
                .thenReturn(failedFuture);

        assertThatCode(() -> producer.publish(reading)).doesNotThrowAnyException();
    }

    @Test
    void noPublicaUnEventoSinIdentificador() {
        SensorReadingRecord invalid = new SensorReadingRecord(null, "Zona A", 10, Instant.now());

        producer.publish(invalid);

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(SensorReadingRecord.class));
    }

    private SensorReadingRecord validReading() {
        return new SensorReadingRecord(
                UUID.randomUUID(), "Zona A", 45, Instant.parse("2026-11-12T16:00:00Z"));
    }
}
