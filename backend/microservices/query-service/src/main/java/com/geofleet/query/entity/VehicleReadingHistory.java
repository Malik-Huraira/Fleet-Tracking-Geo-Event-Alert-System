package com.geofleet.query.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_readings_history")
@Data
public class VehicleReadingHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id", nullable = false)
    private String vehicleId;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(columnDefinition = "geometry(Point,4326)", insertable = false, updatable = false)
    private Point location;

    private Double speed;
    private Double heading;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;
}
