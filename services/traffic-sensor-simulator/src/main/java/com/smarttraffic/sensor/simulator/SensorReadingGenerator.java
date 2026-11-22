package com.smarttraffic.sensor.simulator;

import com.smarttraffic.sensor.domain.SensorReadingRecord;
import com.smarttraffic.sensor.domain.TrafficZone;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Genera lecturas aleatorias y válidas. No conoce Kafka ni la forma de publicación.
 */
@Component
public class SensorReadingGenerator {

    public SensorReadingRecord generate(TrafficZone zone, int minVehicleCount, int maxVehicleCount) {
        Objects.requireNonNull(zone, "La zona es obligatoria");
        if (minVehicleCount < 0 || maxVehicleCount < minVehicleCount) {
            throw new IllegalArgumentException(
                    "El rango de vehículos es inválido: min=" + minVehicleCount + ", max=" + maxVehicleCount);
        }

        int vehicleCount = (int) ThreadLocalRandom.current()
                .nextLong(minVehicleCount, (long) maxVehicleCount + 1);

        return new SensorReadingRecord(
                UUID.randomUUID(),
                zone.id(),
                vehicleCount,
                Instant.now());
    }
}
