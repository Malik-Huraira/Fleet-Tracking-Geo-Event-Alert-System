package com.geofleet.alert.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geofleet.common.dto.AlertEventDTO;
import com.geofleet.common.dto.GeoFenceDTO;
import com.geofleet.common.dto.VehicleEventDTO;
import com.geofleet.alert.client.GeofenceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Consumes GPS events and performs PostGIS geofence checks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GpsConsumerService {

    private final GeofenceClient geofenceClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String VEHICLE_ALERTS_TOPIC = "vehicle-alerts";

    // Track which geofences each vehicle is currently in (geofenceId ->
    // geofenceName)
    private final Map<String, Map<Long, String>> vehicleGeofenceState = new ConcurrentHashMap<>();

    @KafkaListener(topics = "vehicle-gps", groupId = "geofence-check-group")
    public void consumeForGeofenceCheck(VehicleEventDTO event) {
        if (event == null || event.getVehicleId() == null)
            return;
        if (event.getLatitude() == null || event.getLongitude() == null)
            return;

        log.debug("Checking geofence for {} at ({}, {})",
                event.getVehicleId(), event.getLatitude(), event.getLongitude());

        try {
            List<GeoFenceDTO> currentGeofences = geofenceClient.getGeofencesContainingPoint(
                    event.getLatitude(), event.getLongitude());

            Map<Long, String> currentGeofenceMap = currentGeofences.stream()
                    .collect(Collectors.toMap(GeoFenceDTO::getId, GeoFenceDTO::getName));

            Map<Long, String> previousGeofenceMap = vehicleGeofenceState.getOrDefault(
                    event.getVehicleId(), new ConcurrentHashMap<>());

            // Detect GEOFENCE_ENTER
            for (GeoFenceDTO geofence : currentGeofences) {
                if (!previousGeofenceMap.containsKey(geofence.getId())) {
                    log.info("GEOFENCE_ENTER: {} entered '{}'",
                            event.getVehicleId(), geofence.getName());
                    sendGeofenceAlert(event, "GEOFENCE_ENTER", geofence.getId(), geofence.getName());
                }
            }

            // Detect GEOFENCE_EXIT
            for (Map.Entry<Long, String> prevEntry : previousGeofenceMap.entrySet()) {
                if (!currentGeofenceMap.containsKey(prevEntry.getKey())) {
                    log.info("GEOFENCE_EXIT: {} exited '{}'",
                            event.getVehicleId(), prevEntry.getValue());
                    sendGeofenceAlert(event, "GEOFENCE_EXIT", prevEntry.getKey(), prevEntry.getValue());
                }
            }

            // Update state
            vehicleGeofenceState.put(event.getVehicleId(), new ConcurrentHashMap<>(currentGeofenceMap));

        } catch (Exception e) {
            log.error("Error checking geofence for {}: {}", event.getVehicleId(), e.getMessage());
        }
    }

    private void sendGeofenceAlert(VehicleEventDTO event, String alertType, Long geofenceId, String geofenceName) {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("geofence_id", geofenceId);
            details.put("geofence_name", geofenceName);

            AlertEventDTO alert = new AlertEventDTO();
            alert.setVehicleId(event.getVehicleId());
            alert.setAlertType(alertType);
            alert.setMessage(objectMapper.writeValueAsString(details));
            alert.setLatitude(event.getLatitude());
            alert.setLongitude(event.getLongitude());
            alert.setTimestamp(OffsetDateTime.now());
            alert.setSeverity("MEDIUM");

            String alertJson = objectMapper.writeValueAsString(alert);
            kafkaTemplate.send(VEHICLE_ALERTS_TOPIC, event.getVehicleId(), alertJson);

        } catch (Exception e) {
            log.error("Error sending geofence alert: {}", e.getMessage());
        }
    }
}
