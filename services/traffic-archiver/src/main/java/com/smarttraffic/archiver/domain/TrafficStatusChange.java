package com.smarttraffic.archiver.domain;

import com.smarttraffic.archiver.exception.ReglaNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Entidad JPA: una fila de la tabla {@code traffic_status_history}, es decir,
 * un cambio de estado de congestión de una zona.
 *
 * <p>Igual que {@link TrafficReading}, solo se crea con {@link #registrar}, que
 * valida las reglas de negocio, y no tiene setters: el historial es inmutable.
 */
@Entity
@Table(name = "traffic_status_history")
public class TrafficStatusChange {

    private static final int MAX_ZONE_LENGTH = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zone_id", nullable = false, length = MAX_ZONE_LENGTH, updatable = false)
    private String zoneId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20, updatable = false)
    private TrafficStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "traffic_status", nullable = false, length = 20, updatable = false)
    private TrafficStatus trafficStatus;

    @Column(name = "average_vehicles", nullable = false, updatable = false)
    private Double averageVehicles;

    @Column(name = "status_timestamp", nullable = false, updatable = false)
    private Instant statusTimestamp;

    /** La asigna PostgreSQL con DEFAULT now(); Java nunca la escribe. */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    /** Constructor exigido por JPA. No usar desde el código de la aplicación. */
    protected TrafficStatusChange() {
    }

    private TrafficStatusChange(String zoneId, TrafficStatus previousStatus,
            TrafficStatus trafficStatus, double averageVehicles, Instant statusTimestamp) {
        this.zoneId = zoneId;
        this.previousStatus = previousStatus;
        this.trafficStatus = trafficStatus;
        this.averageVehicles = averageVehicles;
        this.statusTimestamp = statusTimestamp;
    }

    /**
     * Crea un cambio de estado validando las reglas de negocio.
     *
     * @param previousStatus estado anterior; {@code null} si es el primero de la zona
     * @throws ReglaNegocioException si algún dato es inválido
     */
    public static TrafficStatusChange registrar(String zoneId, TrafficStatus previousStatus,
            TrafficStatus trafficStatus, Double averageVehicles, Instant statusTimestamp) {
        if (zoneId == null || zoneId.isBlank()) {
            throw new ReglaNegocioException("El zone_id es obligatorio");
        }
        String normalizedZone = zoneId.trim();
        if (normalizedZone.length() > MAX_ZONE_LENGTH) {
            throw new ReglaNegocioException(
                    "El zone_id supera " + MAX_ZONE_LENGTH + " caracteres: " + normalizedZone);
        }
        if (trafficStatus == null) {
            throw new ReglaNegocioException("El estado de tráfico es obligatorio");
        }
        if (trafficStatus == previousStatus) {
            throw new ReglaNegocioException(
                    "No hay cambio de estado en la zona " + normalizedZone + ": " + trafficStatus);
        }
        if (averageVehicles == null || averageVehicles.isNaN() || averageVehicles < 0) {
            throw new ReglaNegocioException(
                    "El promedio de vehículos debe ser mayor o igual que 0: " + averageVehicles);
        }
        if (statusTimestamp == null) {
            throw new ReglaNegocioException("El timestamp del estado es obligatorio");
        }
        return new TrafficStatusChange(
                normalizedZone, previousStatus, trafficStatus, averageVehicles, statusTimestamp);
    }

    public Long getId() {
        return id;
    }

    public String getZoneId() {
        return zoneId;
    }

    public TrafficStatus getPreviousStatus() {
        return previousStatus;
    }

    public TrafficStatus getTrafficStatus() {
        return trafficStatus;
    }

    public Double getAverageVehicles() {
        return averageVehicles;
    }

    public Instant getStatusTimestamp() {
        return statusTimestamp;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "TrafficStatusChange{id=" + id + ", zoneId='" + zoneId + "', previousStatus="
                + previousStatus + ", trafficStatus=" + trafficStatus
                + ", averageVehicles=" + averageVehicles + ", statusTimestamp=" + statusTimestamp + '}';
    }
}
