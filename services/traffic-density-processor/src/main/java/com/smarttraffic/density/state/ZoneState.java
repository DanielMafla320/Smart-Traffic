package com.smarttraffic.density.state;

import com.smarttraffic.density.domain.TrafficStatus;
import com.smarttraffic.density.domain.TrafficThresholds;

/**
 * Abstracción del patrón State. Cada implementación decide si la zona continúa
 * en el mismo estado o debe transicionar según la densidad y los umbrales.
 */
public interface ZoneState {

    TrafficStatus status();

    /**
     * Evalúa la densidad actual.
     *
     * @return la misma instancia si no hay cambio de estado, o el nuevo estado
     */
    ZoneState evaluate(double averageVehicles, TrafficThresholds thresholds);
}
