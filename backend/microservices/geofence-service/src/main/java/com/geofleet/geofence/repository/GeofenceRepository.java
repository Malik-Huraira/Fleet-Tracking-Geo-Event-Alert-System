package com.geofleet.geofence.repository;

import com.geofleet.geofence.entity.GeoFence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeofenceRepository extends JpaRepository<GeoFence, Long> {
    
    // Point-in-polygon (PostGIS) - find geofences containing a point
    @Query(value = "SELECT * FROM geofences g " +
           "WHERE ST_Contains(g.polygon_geom, ST_SetSRID(ST_Point(:lng, :lat), 4326))", 
           nativeQuery = true)
    List<GeoFence> findGeofencesContainingPoint(@Param("lat") double lat, @Param("lng") double lng);
    
    // Find geofences near a point within distance (meters)
    @Query(value = "SELECT * FROM geofences g " +
           "WHERE ST_DWithin(g.polygon_geom::geography, ST_SetSRID(ST_Point(:lng, :lat), 4326)::geography, :distance)", 
           nativeQuery = true)
    List<GeoFence> findGeofencesNearPoint(@Param("lat") double lat, @Param("lng") double lng, @Param("distance") double distance);
    
    List<GeoFence> findByNameContainingIgnoreCase(String name);
}
