package com.geofleet.tracking.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "vehicle_status_cache")
@Data
public class VehicleStatusCache {
    @Id
    @Column(name = "vehicle_id", length = 50)
    private String vehicleId;

    @Column(name = "last_lat", precision = 9, scale = 6)
    private BigDecimal lastLat;

    @Column(name = "last_lng", precision = 9, scale = 6)
    private BigDecimal lastLng;

    @Column(name = "last_speed", precision = 6, scale = 2)
    private BigDecimal lastSpeed;

    @Column(name = "last_seen")
    private OffsetDateTime lastSeen;

    @Column(name = "status", length = 20)
    private String status = "ACTIVE";

    @Column(name = "status_updated_at")
    private OffsetDateTime statusUpdatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        statusUpdatedAt = OffsetDateTime.now();
    }
}
