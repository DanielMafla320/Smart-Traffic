package com.smarttraffic.alertdispatcher.alert;

import com.smarttraffic.alertdispatcher.domain.CongestionStatus;
import com.smarttraffic.alertdispatcher.state.StatusChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CriticalCongestionObserver implements TrafficAlertObserver {

    private static final Logger log = LoggerFactory.getLogger(CriticalCongestionObserver.class);

    @Override
    public void onStatusChange(StatusChange change) {
        if (change.currentStatus() == CongestionStatus.CONGESTIONADA) {
            log.warn("CRITICAL congestion alert: zone={}, timestamp={}",
                    change.zone(), change.timestamp());
        }
    }
}
