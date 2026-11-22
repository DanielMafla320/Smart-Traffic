package com.smarttraffic.alertdispatcher.service;

import com.smarttraffic.alertdispatcher.alert.TrafficAlertSubject;
import com.smarttraffic.alertdispatcher.dto.TrafficUpdateEvent;
import com.smarttraffic.alertdispatcher.state.SnapshotUpdateResult;
import com.smarttraffic.alertdispatcher.state.TrafficStateStore;
import org.springframework.stereotype.Service;

@Service
public class TrafficUpdateProcessor {

    private final TrafficStateStore stateStore;
    private final TrafficAlertSubject alertSubject;

    public TrafficUpdateProcessor(TrafficStateStore stateStore, TrafficAlertSubject alertSubject) {
        this.stateStore = stateStore;
        this.alertSubject = alertSubject;
    }

    public synchronized SnapshotUpdateResult process(TrafficUpdateEvent event) {
        SnapshotUpdateResult result = stateStore.applySnapshot(event);
        if (result.accepted()) {
            result.changes().forEach(alertSubject::publish);
        }
        return result;
    }
}
