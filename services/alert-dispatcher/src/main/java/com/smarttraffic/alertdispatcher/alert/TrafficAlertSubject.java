package com.smarttraffic.alertdispatcher.alert;

import com.smarttraffic.alertdispatcher.state.StatusChange;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class TrafficAlertSubject {

    private final List<TrafficAlertObserver> observers = new CopyOnWriteArrayList<>();

    public TrafficAlertSubject(List<TrafficAlertObserver> observers) {
        this.observers.addAll(observers);
    }

    public void addObserver(TrafficAlertObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(TrafficAlertObserver observer) {
        observers.remove(observer);
    }

    public void publish(StatusChange change) {
        observers.forEach(observer -> observer.onStatusChange(change));
    }
}
