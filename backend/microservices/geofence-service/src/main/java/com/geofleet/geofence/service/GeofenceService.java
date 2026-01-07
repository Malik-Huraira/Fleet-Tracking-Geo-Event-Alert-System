package com.geofleet.geofence.service;

import com.geofleet.common.dto.GeoFenceDTO;
import com.geofleet.geofence.entity.GeoFence;
import com.geofleet.geofence.repository.GeofenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeofenceService {

    private final GeofenceRepository geofenceRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public List<GeoFenceDTO> getAllGeofences() {
        return geofenceRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<GeoFenceDTO> getAllActiveGeofences() {
        return getAllGeofences();
    }

    public GeoFenceDTO getGeofenceById(Long id) {
        return geofenceRepository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }

    // Point-in-polygon query
    public List<GeoFenceDTO> getGeofencesContainingPoint(double lat, double lon) {
        log.debug("Finding geofences containing point: ({}, {})", lat, lon);
        return geofenceRepository.findGeofencesContainingPoint(lat, lon).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<GeoFenceDTO> getGeofencesNearPoint(double lat, double lon, double distanceMeters) {
        return geofenceRepository.findGeofencesNearPoint(lat, lon, distanceMeters).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public GeoFenceDTO createGeofence(GeoFenceDTO dto) {
        GeoFence geofence = new GeoFence();
        geofence.setName(dto.getName());

        // Create polygon from coordinates
        if (dto.getCoordinates() != null && !dto.getCoordinates().isEmpty()) {
            geofence.setPolygonGeom(createPolygon(dto.getCoordinates()));
            geofence.setPolygonGeojson(createGeoJson(dto.getCoordinates()));
        }

        GeoFence saved = geofenceRepository.save(geofence);
        log.info("Created geofence: {} with id: {}", saved.getName(), saved.getId());
        return convertToDTO(saved);
    }

    @Transactional
    public GeoFenceDTO updateGeofence(Long id, GeoFenceDTO dto) {
        GeoFence geofence = geofenceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Geofence not found: " + id));

        geofence.setName(dto.getName());
        if (dto.getCoordinates() != null && !dto.getCoordinates().isEmpty()) {
            geofence.setPolygonGeom(createPolygon(dto.getCoordinates()));
            geofence.setPolygonGeojson(createGeoJson(dto.getCoordinates()));
        }

        return convertToDTO(geofenceRepository.save(geofence));
    }

    @Transactional
    public void deleteGeofence(Long id) {
        geofenceRepository.deleteById(id);
        log.info("Deleted geofence: {}", id);
    }

    private Polygon createPolygon(List<double[]> coordinates) {
        Coordinate[] coords = new Coordinate[coordinates.size() + 1];
        for (int i = 0; i < coordinates.size(); i++) {
            coords[i] = new Coordinate(coordinates.get(i)[0], coordinates.get(i)[1]);
        }
        // Close the polygon
        coords[coordinates.size()] = coords[0];
        return geometryFactory.createPolygon(coords);
    }

    private String createGeoJson(List<double[]> coordinates) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"type\": \"Polygon\", \"coordinates\": [[");
            for (int i = 0; i < coordinates.size(); i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append("[").append(coordinates.get(i)[0]).append(", ").append(coordinates.get(i)[1]).append("]");
            }
            // Close the polygon
            sb.append(", [").append(coordinates.get(0)[0]).append(", ").append(coordinates.get(0)[1]).append("]");
            sb.append("]]}");
            return sb.toString();
        } catch (Exception e) {
            log.error("Error creating GeoJSON: {}", e.getMessage());
            return null;
        }
    }

    private GeoFenceDTO convertToDTO(GeoFence geofence) {
        GeoFenceDTO dto = new GeoFenceDTO();
        dto.setId(geofence.getId());
        dto.setName(geofence.getName());

        if (geofence.getPolygonGeom() != null) {
            Coordinate[] coords = geofence.getPolygonGeom().getCoordinates();
            List<double[]> coordList = new java.util.ArrayList<>();
            for (int i = 0; i < coords.length - 1; i++) {
                coordList.add(new double[] { coords[i].x, coords[i].y });
            }
            dto.setCoordinates(coordList);
        }

        return dto;
    }
}
