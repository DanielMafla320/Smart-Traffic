package com.smarttraffic.density.consumer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;

import com.smarttraffic.density.domain.TrafficReadingEvent;
import com.smarttraffic.density.service.InvalidTrafficReadingException;
import com.smarttraffic.density.service.TrafficReadingProcessor;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RawTrafficDataListenerTest {

    @Mock
    private TrafficReadingProcessor processor;

    @InjectMocks
    private RawTrafficDataListener listener;

    @Test
    void procesaUnRegistroValido() {
        TrafficReadingEvent event = event();
        ConsumerRecord<String, TrafficReadingEvent> record = record(event);

        listener.onMessage(record);

        verify(processor).process(event);
    }

    @Test
    void ignoraUnRegistroConValorNuloDeserializado() {
        ConsumerRecord<String, TrafficReadingEvent> record = record(null);

        listener.onMessage(record);

        verifyNoInteractions(processor);
    }

    @Test
    void descartaUnEventoInvalidoSinDetenerElConsumidor() {
        TrafficReadingEvent event = event();
        doThrow(new InvalidTrafficReadingException("inválido")).when(processor).process(event);

        assertThatCode(() -> listener.onMessage(record(event))).doesNotThrowAnyException();

        verify(processor).process(event);
    }

    @Test
    void procesaElPayloadAunqueLaClaveNoCoincida() {
        TrafficReadingEvent event = event();
        ConsumerRecord<String, TrafficReadingEvent> record =
                new ConsumerRecord<>("raw_traffic_data", 0, 16L, "Otra zona", event);

        listener.onMessage(record);

        verify(processor).process(event);
    }

    @Test
    void procesaElPayloadAunqueLaClaveSeaNula() {
        TrafficReadingEvent event = event();
        ConsumerRecord<String, TrafficReadingEvent> record =
                new ConsumerRecord<>("raw_traffic_data", 0, 17L, null, event);

        listener.onMessage(record);

        verify(processor).process(event);
    }

    private ConsumerRecord<String, TrafficReadingEvent> record(TrafficReadingEvent event) {
        return new ConsumerRecord<>("raw_traffic_data", 0, 15L, "Zona A", event);
    }

    private TrafficReadingEvent event() {
        return new TrafficReadingEvent(
                UUID.randomUUID(), "Zona A", 45, Instant.parse("2026-11-12T16:00:00Z"));
    }
}
