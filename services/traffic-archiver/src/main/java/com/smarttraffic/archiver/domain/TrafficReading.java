package com.smarttraffic.archiver.domain;

import com.smarttraffic.archiver.exception.ReglaNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA: una fila de la tabla {@code traffic_readings_history}.
 *
 * <p>Modelo enriquecido: la única forma de crear una lectura nueva es
 * {@link #registrar}, que valida las reglas de negocio. No hay setters, así que
 * una lectura guardada no se puede modificar (el historial es inmutable).
 *
 * <p>Los atributos no son {@code final} porque JPA necesita asignarlos por
 * reflexión al leer de la base de datos.
 */
@Entity
@Table(name = "traffic_readings_history")
public class TrafficReading {

    private static final int MAX_ZONE_LENGTH = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, updatable = false)
    private UUID eventId;

    @Column(name = "zone_id", nullable = false, length = MAX_ZONE_LENGTH, updatable = false)
    private String zoneId;

    @Column(name = "vehicle_count", nullable = false, updatable = false)
    private Integer vehicleCount;

    @Column(name = "reading_timestamp", nullable = false, updatable = false)
    private Instant readingTimestamp;

    /** La asigna PostgreSQL con DEFAULT now(); Java nunca la escribe. */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    /** Constructor exigido por JPA. No usar desde el código de la aplicación. */
    protected TrafficReading() {
    }

    private TrafficReading(UUID eventId, String zoneId, int vehicleCount, Instant readingTimestamp) {
        this.eventId = eventId;
        this.zoneId = zoneId;
        this.vehicleCount = vehicleCount;
        this.readingTimestamp = readingTimestamp;
    }

    /**
     * Crea una lectura nueva validando las reglas de negocio.
     *
     * @throws ReglaNegocioException si algún dato es inválido
     */
    public static TrafficReading registrar(
            UUID eventId, String zoneId, Integer vehicleCount, Instant readingTimestamp) {
        if (eventId == null) {
            throw new ReglaNegocioException("El event_id es obligatorio");
        }
        if (zoneId == null || zoneId.isBlank()) {
            throw new ReglaNegocioException("El zone_id es obligatorio");
        }
        String normalizedZone = zoneId.trim();
        if (normalizedZone.length() > MAX_ZONE_LENGTH) {
            throw new ReglaNegocioException(
                    "El zone_id supera " + MAX_ZONE_LENGTH + " caracteres: " + normalizedZone);
        }
        if (vehicleCount == null || vehicleCount < 0) {
            throw new ReglaNegocioException(
                    "La cantidad de vehículos debe ser mayor o igual que 0: " + vehicleCount);
        }
        if (readingTimestamp == null) {
            throw new ReglaNegocioException("El timestamp de la lectura es obligatorio");
        }
        return new TrafficReading(eventId, normalizedZone, vehicleCount, readingTimestamp);
    }

    public Long getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getZoneId() {
        return zoneId;
    }

    public Integer getVehicleCount() {
        return vehicleCount;
    }

    public Instant getReadingTimestamp() {
        return readingTimestamp;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "TrafficReading{id=" + id + ", eventId=" + eventId + ", zoneId='" + zoneId
                + "', vehicleCount=" + vehicleCount + ", readingTimestamp=" + readingTimestamp + '}';
    }
}
