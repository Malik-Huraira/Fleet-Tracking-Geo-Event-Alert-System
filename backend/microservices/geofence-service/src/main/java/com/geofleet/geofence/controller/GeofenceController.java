package com.geofleet.geofence.controller;

import com.geofleet.common.dto.GeoFenceDTO;
import com.geofleet.geofence.service.GeofenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/geofences")
@RequiredArgsConstructor
public class GeofenceController {

    private final GeofenceService geofenceService;

    @GetMapping
    public ResponseEntity<List<GeoFenceDTO>> getAllGeofences() {
        return ResponseEntity.ok(geofenceService.getAllActiveGeofences());
    }

    /**
     * Returns geofences in a format compatible with the frontend map.
     * Transforms coordinates to GeoJSON polygon format: [[[lng, lat], ...]]
     */
    @GetMapping("/geojson")
    public ResponseEntity<List<Map<String, Object>>> getGeofencesAsGeoJson() {
        List<GeoFenceDTO> geofences = geofenceService.getAllActiveGeofences();

        List<Map<String, Object>> result = geofences.stream().map(fence -> {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("id", fence.getId());
            item.put("name", fence.getName());

            // Transform coordinates to GeoJSON polygon format: [[[lng, lat], ...]]
            if (fence.getCoordinates() != null && !fence.getCoordinates().isEmpty()) {
                List<double[]> coords = fence.getCoordinates();
                // Wrap in extra array for GeoJSON polygon ring format
                item.put("coordinates", List.of(coords));
            } else {
                item.put("coordinates", List.of());
            }

            return item;
        }).toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GeoFenceDTO> getGeofence(@PathVariable Long id) {
        GeoFenceDTO geofence = geofenceService.getGeofenceById(id);
        if (geofence == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(geofence);
    }

    @GetMapping("/containing")
    public ResponseEntity<List<GeoFenceDTO>> getGeofencesContainingPoint(
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon) {
        return ResponseEntity.ok(geofenceService.getGeofencesContainingPoint(lat, lon));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<GeoFenceDTO>> getGeofencesNearPoint(
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon,
            @RequestParam(value = "distance", defaultValue = "1000") double distance) {
        return ResponseEntity.ok(geofenceService.getGeofencesNearPoint(lat, lon, distance));
    }

    @PostMapping
    public ResponseEntity<GeoFenceDTO> createGeofence(@Valid @RequestBody GeoFenceDTO dto) {
        return ResponseEntity.ok(geofenceService.createGeofence(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GeoFenceDTO> updateGeofence(
            @PathVariable Long id,
            @Valid @RequestBody GeoFenceDTO dto) {
        return ResponseEntity.ok(geofenceService.updateGeofence(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGeofence(@PathVariable Long id) {
        geofenceService.deleteGeofence(id);
        return ResponseEntity.noContent().build();
    }
}
