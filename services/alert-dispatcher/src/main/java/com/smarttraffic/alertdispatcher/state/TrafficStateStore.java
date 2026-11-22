package com.smarttraffic.alertdispatcher.state;

import com.smarttraffic.alertdispatcher.domain.CongestionStatus;
import com.smarttraffic.alertdispatcher.dto.TrafficUpdateEvent;
import com.smarttraffic.alertdispatcher.dto.TrafficUpdateZone;
import com.smarttraffic.alertdispatcher.dto.ZoneStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class TrafficStateStore {

    private final Map<String, CongestionStatus> states = new LinkedHashMap<>();
    private Instant latestTimestamp;
    private boolean initialized;

    public synchronized SnapshotUpdateResult applySnapshot(TrafficUpdateEvent event) {
        if (event == null || event.timestamp() == null || event.zones() == null
                || event.zones().isEmpty()) {
            return unchangedResult();
        }
        if (latestTimestamp != null && !event.timestamp().isAfter(latestTimestamp)) {
            return unchangedResult();
        }

        Map<String, CongestionStatus> incoming = new LinkedHashMap<>();
        for (TrafficUpdateZone zone : event.zones()) {
            String zoneName = zoneName(zone);
            CongestionStatus status = status(zone);
            if (zoneName != null && status != null) {
                incoming.putIfAbsent(zoneName, status);
            }
        }
        if (incoming.isEmpty()) {
            return unchangedResult();
        }

        List<StatusChange> changes = new ArrayList<>();
        if (initialized) {
            incoming.forEach((zone, status) -> {
                CongestionStatus previous = states.get(zone);
                if (previous != null && previous != status) {
                    changes.add(new StatusChange(zone, previous, status, event.timestamp()));
                }
            });
        }

        states.putAll(incoming);
        latestTimestamp = event.timestamp();
        initialized = true;
        return new SnapshotUpdateResult(true, true, latestTimestamp, changes);
    }

    public synchronized boolean isInitialized() {
        return initialized;
    }

    public synchronized Instant latestTimestamp() {
        return latestTimestamp;
    }

    public synchronized CongestionStatus statusOf(String zone) {
        return states.get(zone);
    }

    public synchronized List<ZoneStatus> snapshot() {
        if (!initialized) {
            return List.of();
        }
        return states.entrySet().stream()
                .map(entry -> new ZoneStatus(entry.getKey(), entry.getValue()))
                .toList();
    }

    public synchronized TrafficStateSnapshot snapshotState() {
        return new TrafficStateSnapshot(initialized, latestTimestamp, snapshot());
    }

    private SnapshotUpdateResult unchangedResult() {
        return new SnapshotUpdateResult(false, initialized, latestTimestamp, List.of());
    }

    private String zoneName(TrafficUpdateZone zone) {
        if (zone == null || zone.zoneId() == null || zone.zoneId().isBlank()) {
            return null;
        }
        return zone.zoneId().trim();
    }

    private CongestionStatus status(TrafficUpdateZone zone) {
        if (zone == null || zone.trafficStatus() == null) {
            return null;
        }
        try {
            return CongestionStatus.valueOf(zone.trafficStatus().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
