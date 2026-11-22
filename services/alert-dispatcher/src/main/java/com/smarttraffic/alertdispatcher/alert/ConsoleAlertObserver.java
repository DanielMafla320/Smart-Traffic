package com.smarttraffic.alertdispatcher.alert;

import com.smarttraffic.alertdispatcher.state.StatusChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConsoleAlertObserver implements TrafficAlertObserver {

    private static final Logger log = LoggerFactory.getLogger(ConsoleAlertObserver.class);

    @Override
    public void onStatusChange(StatusChange change) {
        log.info("Traffic status changed: zone={}, {} -> {}, timestamp={}",
                change.zone(), change.previousStatus(), change.currentStatus(), change.timestamp());
    }
}
