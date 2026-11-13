package com.smarttraffic.density.state;

import com.smarttraffic.density.domain.TrafficStatus;
import com.smarttraffic.density.domain.TrafficThresholds;

/**
 * Tráfico congestionado: promedio igual o superior al umbral de congestión.
 */
public final class CongestionadaState implements ZoneState {

    @Override
    public TrafficStatus status() {
        return TrafficStatus.CONGESTIONADA;
    }

    @Override
    public ZoneState evaluate(double averageVehicles, TrafficThresholds thresholds) {
        if (averageVehicles < thresholds.moderada()) {
            return new DespejadaState();
        }
        if (averageVehicles < thresholds.congestionada()) {
            return new ModeradaState();
        }
        return this;
    }
}
