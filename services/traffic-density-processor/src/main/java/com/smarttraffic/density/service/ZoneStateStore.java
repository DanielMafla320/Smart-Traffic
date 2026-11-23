package com.smarttraffic.density.service;

import com.smarttraffic.density.config.DensityProperties;
import com.smarttraffic.density.domain.TrafficStatus;
import com.smarttraffic.density.domain.TrafficThresholds;
import com.smarttraffic.density.domain.ZoneSnapshot;
import com.smarttraffic.density.state.DespejadaState;
import com.smarttraffic.density.state.ZoneState;
import com.smarttraffic.density.strategy.DensityStrategy;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Mantiene el estado agregado de cada zona en memoria.
 *
 * <p>El mapa es seguro para concurrencia y cada actualización crea un nuevo
 * {@link ZoneData} inmutable, por lo que las lecturas de {@link #snapshots()} no
 * observan estados parcialmente modificados. La ventana de lecturas está acotada
 * por cantidad ({@code smarttraffic.density.window-size}) y por antigüedad
 * ({@code smarttraffic.density.window-duration}).
 */
@Service
public class ZoneStateStore {

    private static final Logger log = LoggerFactory.getLogger(ZoneStateStore.class);

    private final ConcurrentMap<String, ZoneData> zoneStates = new ConcurrentHashMap<>();
    private final DensityStrategy densityStrategy;
    private final int windowSize;
    private final Duration windowDuration;
    private final TrafficThresholds thresholds;

    public ZoneStateStore(DensityStrategy densityStrategy, DensityProperties properties) {
        this.densityStrategy = densityStrategy;
        this.windowSize = properties.windowSize();
        this.windowDuration = properties.timeWindowEnabled() ? properties.windowDuration() : null;
        this.thresholds = properties.toThresholds();
    }

    public void record(String zoneId, int vehicleCount, Instant timestamp) {
        if (zoneId == null || zoneId.isBlank()) {
            throw new IllegalArgumentException("El identificador de la zona es obligatorio");
        }
        if (vehicleCount < 0) {
            throw new IllegalArgumentException("La cantidad de vehículos no puede ser negativa");
        }
        Objects.requireNonNull(timestamp, "El timestamp de la lectura es obligatorio");

        String normalizedZoneId = zoneId.trim();
        zoneStates.compute(normalizedZoneId,
                (id, current) -> updateZone(id, current, vehicleCount, timestamp));
    }

    public List<ZoneSnapshot> snapshots() {
        return zoneStates.values().stream()
                .map(this::toSnapshot)
                .sorted(Comparator.comparing(ZoneSnapshot::zoneId))
                .toList();
    }

    public Optional<ZoneSnapshot> findSnapshot(String zoneId) {
        if (zoneId == null || zoneId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(zoneStates.get(zoneId.trim()))
                .map(this::toSnapshot);
    }

    private ZoneData updateZone(
            String zoneId, ZoneData current, int vehicleCount, Instant timestamp) {
        Instant newest = current == null || timestamp.isAfter(current.lastUpdated())
                ? timestamp
                : current.lastUpdated();

        List<TimedReading> readings = new ArrayList<>(
                current == null ? List.of() : current.readings());
        readings.add(new TimedReading(vehicleCount, timestamp));

        // Ventana temporal: se descartan las lecturas más antiguas que windowDuration
        // respecto a la lectura más reciente de la zona.
        if (windowDuration != null) {
            Instant cutoff = newest.minus(windowDuration);
            readings.removeIf(reading -> reading.timestamp().isBefore(cutoff));
        }
        // Ventana por cantidad: como máximo windowSize lecturas.
        if (readings.size() > windowSize) {
            readings = new ArrayList<>(
                    readings.subList(readings.size() - windowSize, readings.size()));
        }
        if (readings.isEmpty()) {
            // Lectura atrasada fuera de la ventana: no altera el estado actual.
            log.debug("Lectura atrasada de la zona {} ({}) descartada por la ventana temporal",
                    zoneId, timestamp);
            return current;
        }

        List<Integer> window = readings.stream().map(TimedReading::vehicleCount).toList();
        double average = densityStrategy.calculate(window);
        ZoneState previousState = current == null ? new DespejadaState() : current.state();
        ZoneState nextState = previousState.evaluate(average, thresholds);

        if (current == null) {
            log.info("Zona {} inicializada con estado {} (vehículos promedio: {})",
                    zoneId, nextState.status(), formatAverage(average));
        } else if (previousState.status() != nextState.status()) {
            log.info("Zona {} cambió de {} a {} (vehículos promedio: {})",
                    zoneId, previousState.status(), nextState.status(), formatAverage(average));
        }

        return new ZoneData(zoneId, readings, average, nextState, newest);
    }

    private String formatAverage(double average) {
        return String.format(Locale.ROOT, "%.2f", average);
    }

    private ZoneSnapshot toSnapshot(ZoneData data) {
        TrafficStatus status = data.state().status();
        return new ZoneSnapshot(
                data.zoneId(),
                data.readings().stream().map(TimedReading::vehicleCount).toList(),
                data.averageVehicles(),
                status,
                data.lastUpdated());
    }

    private record TimedReading(int vehicleCount, Instant timestamp) {
    }

    private record ZoneData(
            String zoneId,
            List<TimedReading> readings,
            double averageVehicles,
            ZoneState state,
            Instant lastUpdated) {

        private ZoneData {
            readings = List.copyOf(readings);
        }
    }
}
