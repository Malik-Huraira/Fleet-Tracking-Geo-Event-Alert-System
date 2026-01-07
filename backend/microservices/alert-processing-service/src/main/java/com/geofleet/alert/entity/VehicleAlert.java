package com.geofleet.alert.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.locationtech.jts.geom.Point;

import java.time.OffsetDateTime;

@Entity
@Table(name = "vehicle_alerts")
@Data
public class VehicleAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id", nullable = false, length = 50)
    private String vehicleId;

    @Column(name = "alert_type", nullable = false, length = 50)
    private String alertType;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt;

    @Column(name = "geom", columnDefinition = "geography(Point,4326)")
    private Point geom;
}
