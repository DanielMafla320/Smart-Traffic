package com.smarttraffic.sensor.simulator;

import com.smarttraffic.sensor.config.SimulatorProperties;
import com.smarttraffic.sensor.domain.SensorReadingRecord;
import com.smarttraffic.sensor.domain.TrafficZone;
import com.smarttraffic.sensor.producer.TrafficSensorProducer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Orquesta la simulación: genera una lectura por cada zona configurada y delega
 * la publicación en {@link TrafficSensorProducer}.
 */
@Service
@ConditionalOnProperty(
        prefix = "smarttraffic.simulator",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class TrafficSimulationService {

    private static final Logger log = LoggerFactory.getLogger(TrafficSimulationService.class);

    private final SensorReadingGenerator generator;
    private final TrafficSensorProducer producer;
    private final List<TrafficZone> zones;
    private final int minVehicleCount;
    private final int maxVehicleCount;

    public TrafficSimulationService(
            SimulatorProperties properties,
            SensorReadingGenerator generator,
            TrafficSensorProducer producer) {
        this.generator = generator;
        this.producer = producer;
        this.zones = properties.zones().stream()
                .map(String::trim)
                .map(TrafficZone::new)
                .toList();
        this.minVehicleCount = properties.vehicleCountMin();
        this.maxVehicleCount = properties.vehicleCountMax();
    }

    @Scheduled(fixedDelayString = "${smarttraffic.simulator.interval-ms:2000}")
    public void generateAndPublishReadings() {
        for (TrafficZone zone : zones) {
            SensorReadingRecord reading = generator.generate(zone, minVehicleCount, maxVehicleCount);
            producer.publish(reading);
        }
        log.debug("Ciclo de simulación completado para {} zonas", zones.size());
    }
}
