package com.smarttraffic.density.state;

import com.smarttraffic.density.domain.TrafficStatus;
import com.smarttraffic.density.domain.TrafficThresholds;

/**
 * Tráfico fluido: promedio estrictamente menor que el umbral moderado.
 */
public final class DespejadaState implements ZoneState {

    @Override
    public TrafficStatus status() {
        return TrafficStatus.DESPEJADA;
    }

    @Override
    public ZoneState evaluate(double averageVehicles, TrafficThresholds thresholds) {
        if (averageVehicles >= thresholds.congestionada()) {
            return new CongestionadaState();
        }
        if (averageVehicles >= thresholds.moderada()) {
            return new ModeradaState();
        }
        return this;
    }
}
