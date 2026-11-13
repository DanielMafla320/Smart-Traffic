package com.smarttraffic.density.state;

import com.smarttraffic.density.domain.TrafficStatus;
import com.smarttraffic.density.domain.TrafficThresholds;

/**
 * Tráfico moderado: desde el umbral moderado (incluido) hasta el de congestión
 * (excluido).
 */
public final class ModeradaState implements ZoneState {

    @Override
    public TrafficStatus status() {
        return TrafficStatus.MODERADA;
    }

    @Override
    public ZoneState evaluate(double averageVehicles, TrafficThresholds thresholds) {
        if (averageVehicles >= thresholds.congestionada()) {
            return new CongestionadaState();
        }
        if (averageVehicles < thresholds.moderada()) {
            return new DespejadaState();
        }
        return this;
    }
}
