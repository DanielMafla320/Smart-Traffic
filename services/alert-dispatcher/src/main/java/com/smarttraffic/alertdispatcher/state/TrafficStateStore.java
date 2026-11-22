package com.smarttraffic.alertdispatcher.state;

import com.smarttraffic.alertdispatcher.domain.CongestionStatus;
import com.smarttraffic.alertdispatcher.domain.TrafficZone;
import com.smarttraffic.alertdispatcher.dto.TrafficUpdateEvent;
import com.smarttraffic.alertdispatcher.dto.TrafficUpdateZone;
import com.smarttraffic.alertdispatcher.dto.ZoneStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TrafficStateStore {

    private final ConcurrentHashMap<TrafficZone, CongestionStatus> states =
            new ConcurrentHashMap<>();
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

        Map<TrafficZone, CongestionStatus> incoming = new EnumMap<>(TrafficZone.class);
        for (TrafficUpdateZone zone : event.zones()) {
            TrafficZone trafficZone = parseZone(zone);
            CongestionStatus status = parseStatus(zone);
            if (trafficZone != null && status != null) {
                incoming.putIfAbsent(trafficZone, status);
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

    public synchronized CongestionStatus statusOf(TrafficZone zone) {
        return states.get(zone);
    }

    public synchronized List<ZoneStatus> snapshot() {
        if (!initialized) {
            return List.of();
        }
        return EnumSet.allOf(TrafficZone.class).stream()
                .filter(states::containsKey)
                .map(zone -> new ZoneStatus(zone, states.get(zone)))
                .toList();
    }

    public synchronized TrafficStateSnapshot snapshotState() {
        return new TrafficStateSnapshot(initialized, latestTimestamp, snapshot());
    }

    private SnapshotUpdateResult unchangedResult() {
        return new SnapshotUpdateResult(false, initialized, latestTimestamp, List.of());
    }

    private TrafficZone parseZone(TrafficUpdateZone zone) {
        if (zone == null || zone.zoneId() == null) {
            return null;
        }
        String zoneId = zone.zoneId().trim();
        if (zoneId.regionMatches(true, 0, "Zona ", 0, 5)) {
            zoneId = zoneId.substring(5).trim();
        }
        try {
            return TrafficZone.valueOf(zoneId.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private CongestionStatus parseStatus(TrafficUpdateZone zone) {
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
