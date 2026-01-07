package com.geofleet.query.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "daily_vehicle_stats")
@Data
public class DailyVehicleStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id", nullable = false)
    private String vehicleId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "total_distance")
    private Double totalDistance = 0.0;

    @Column(name = "max_speed")
    private Double maxSpeed = 0.0;

    @Column(name = "avg_speed")
    private Double avgSpeed = 0.0;

    @Column(name = "reading_count")
    private Integer readingCount = 0;

    @Column(name = "alert_count")
    private Integer alertCount = 0;
}
