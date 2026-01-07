package com.geofleet.geofence.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.locationtech.jts.geom.Polygon;

import java.time.OffsetDateTime;

@Entity
@Table(name = "geofences")
@Data
public class GeoFence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "polygon_geojson", columnDefinition = "jsonb")
    private String polygonGeojson;

    @Column(name = "polygon_geom", columnDefinition = "geometry(Polygon,4326)")
    private Polygon polygonGeom;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
