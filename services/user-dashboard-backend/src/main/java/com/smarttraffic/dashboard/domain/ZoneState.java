package com.smarttraffic.dashboard.domain;

import com.smarttraffic.dashboard.exception.ReglaNegocioException;
import java.time.Instant;

/**
 * Estado actual de una zona (valor inmutable y validado).
 *
 * @param zoneId          identificador de la zona, por ejemplo "Zona A"
 * @param averageVehicles promedio de vehículos calculado por el procesador
 * @param status          nivel de congestión
 * @param lastUpdated     instante del snapshot que trajo este estado
 */
public record ZoneState(String zoneId, double averageVehicles, TrafficStatus status, Instant lastUpdated) {

    public ZoneState {
        if (zoneId == null || zoneId.isBlank()) {
            throw new ReglaNegocioException("El zone_id es obligatorio");
        }
        if (Double.isNaN(averageVehicles) || Double.isInfinite(averageVehicles) || averageVehicles < 0) {
            throw new ReglaNegocioException(
                    "El promedio de vehículos de " + zoneId + " es inválido: " + averageVehicles);
        }
        if (status == null) {
            throw new ReglaNegocioException("El estado de tráfico de " + zoneId + " es obligatorio");
        }
        if (lastUpdated == null) {
            throw new ReglaNegocioException("La fecha de actualización de " + zoneId + " es obligatoria");
        }
        zoneId = zoneId.trim();
    }

    public boolean isCongested() {
        return status == TrafficStatus.CONGESTIONADA;
    }
}
