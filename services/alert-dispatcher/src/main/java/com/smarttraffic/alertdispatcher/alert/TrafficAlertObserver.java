package com.smarttraffic.alertdispatcher.alert;

import com.smarttraffic.alertdispatcher.state.StatusChange;

@FunctionalInterface
public interface TrafficAlertObserver {

    void onStatusChange(StatusChange change);
}
