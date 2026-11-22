package com.smarttraffic.dashboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Entidad JPA de solo lectura sobre la tabla {@code traffic_readings_history}
 * (la escribe traffic-archiver). {@code @Immutable} impide que Hibernate
 * intente actualizarla y no hay setters.
 *
 * <p>Nunca se expone en REST: el controlador devuelve un DTO.
 */
@Entity
@Immutable
@Table(name = "traffic_readings_history")
public class TrafficReadingHistory {

    @Id
    private Long id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "zone_id", nullable = false, length = 50)
    private String zoneId;

    @Column(name = "vehicle_count", nullable = false)
    private Integer vehicleCount;

    @Column(name = "reading_timestamp", nullable = false)
    private Instant readingTimestamp;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Constructor exigido por JPA. */
    protected TrafficReadingHistory() {
    }

    public TrafficReadingHistory(
            Long id, UUID eventId, String zoneId, Integer vehicleCount,
            Instant readingTimestamp, Instant createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.zoneId = zoneId;
        this.vehicleCount = vehicleCount;
        this.readingTimestamp = readingTimestamp;
        this.createdAt = createdAt;
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
}
