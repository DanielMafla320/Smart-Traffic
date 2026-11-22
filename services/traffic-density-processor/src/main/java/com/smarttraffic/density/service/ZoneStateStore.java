package com.smarttraffic.density.service;

import com.smarttraffic.density.config.DensityProperties;
import com.smarttraffic.density.domain.TrafficStatus;
import com.smarttraffic.density.domain.TrafficThresholds;
import com.smarttraffic.density.domain.ZoneSnapshot;
import com.smarttraffic.density.state.DespejadaState;
import com.smarttraffic.density.state.ZoneState;
import com.smarttraffic.density.strategy.DensityStrategy;
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
 * por {@code smarttraffic.density.window-size}.
 */
@Service
public class ZoneStateStore {

    private static final Logger log = LoggerFactory.getLogger(ZoneStateStore.class);

    private final ConcurrentMap<String, ZoneData> zoneStates = new ConcurrentHashMap<>();
    private final DensityStrategy densityStrategy;
    private final int windowSize;
    private final TrafficThresholds thresholds;

    public ZoneStateStore(DensityStrategy densityStrategy, DensityProperties properties) {
        this.densityStrategy = densityStrategy;
        this.windowSize = properties.windowSize();
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
        List<Integer> window = new ArrayList<>(
                current == null ? List.of() : current.recentReadings());
        window.add(vehicleCount);
        if (window.size() > windowSize) {
            window = new ArrayList<>(window.subList(window.size() - windowSize, window.size()));
        }

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

        return new ZoneData(zoneId, window, average, nextState, timestamp);
    }

    private String formatAverage(double average) {
        return String.format(Locale.ROOT, "%.2f", average);
    }

    private ZoneSnapshot toSnapshot(ZoneData data) {
        TrafficStatus status = data.state().status();
        return new ZoneSnapshot(
                data.zoneId(),
                data.recentReadings(),
                data.averageVehicles(),
                status,
                data.lastUpdated());
    }

    private record ZoneData(
            String zoneId,
            List<Integer> recentReadings,
            double averageVehicles,
            ZoneState state,
            Instant lastUpdated) {

        private ZoneData {
            recentReadings = List.copyOf(recentReadings);
        }
    }
}
