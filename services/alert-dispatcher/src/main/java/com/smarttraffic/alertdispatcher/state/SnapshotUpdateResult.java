package com.smarttraffic.alertdispatcher.state;

import java.time.Instant;
import java.util.List;

public record SnapshotUpdateResult(
        boolean accepted,
        boolean initialized,
        Instant timestamp,
        List<StatusChange> changes) {

    public SnapshotUpdateResult {
        changes = List.copyOf(changes);
    }
}
