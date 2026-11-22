package com.smarttraffic.sensor.simulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smarttraffic.sensor.config.SimulatorProperties;
import com.smarttraffic.sensor.domain.SensorReadingRecord;
import com.smarttraffic.sensor.domain.TrafficZone;
import com.smarttraffic.sensor.producer.TrafficSensorProducer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrafficSimulationServiceTest {

    @Mock
    private SensorReadingGenerator generator;

    @Mock
    private TrafficSensorProducer producer;

    @Test
    void generaYPublicaUnaLecturaPorCadaZonaConfigurada() {
        SimulatorProperties properties = new SimulatorProperties(
                List.of("Zona A", "Zona B", "Zona C"), 0, 100, 2000);
        when(generator.generate(any(TrafficZone.class), anyInt(), anyInt()))
                .thenAnswer(invocation -> {
                    TrafficZone zone = invocation.getArgument(0);
                    return new SensorReadingRecord(UUID.randomUUID(), zone.id(), 30, Instant.now());
                });

        TrafficSimulationService service = new TrafficSimulationService(properties, generator, producer);
        service.generateAndPublishReadings();

        verify(generator, times(3)).generate(any(TrafficZone.class), org.mockito.ArgumentMatchers.eq(0),
                org.mockito.ArgumentMatchers.eq(100));
        ArgumentCaptor<SensorReadingRecord> readings = ArgumentCaptor.forClass(SensorReadingRecord.class);
        verify(producer, times(3)).publish(readings.capture());
        assertThat(readings.getAllValues())
                .extracting(SensorReadingRecord::zoneId)
                .containsExactly("Zona A", "Zona B", "Zona C");
    }
}
