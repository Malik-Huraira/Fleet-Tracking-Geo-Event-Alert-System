package com.geofleet.alert.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geofleet.common.dto.AlertEventDTO;
import com.geofleet.common.dto.GeoFenceDTO;
import com.geofleet.alert.entity.VehicleAlert;
import com.geofleet.common.util.GeometryUtil;
import com.geofleet.alert.client.GeofenceClient;
import com.geofleet.alert.repository.VehicleAlertRepository;
import com.geofleet.alert.service.AlertSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🅱️ MICROSERVICE - HEAVY PATH
 * 
 * Consumes alerts from vehicle-alerts topic (produced by Kafka Streams)
 * and performs:
 * ✔ PostGIS queries (geofence verification)
 * ✔ Alert deduplication
 * ✔ DB persistence
 * ✔ SSE notification
 * 
 * Flow:
 * 1. Consume vehicle-alerts
 * 2. Validate & enrich
 * 3. Run PostGIS check (if geofence)
 * 4. Save to DB
 * 5. Notify frontend (SSE)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertConsumerService {

    private final VehicleAlertRepository alertRepository;
    private final GeofenceClient geofenceClient;
    private final AlertSseService alertSseService;
    private final ObjectMapper objectMapper;

    // Deduplication cache: vehicleId -> lastAlertTime (per type)
    private final Map<String, OffsetDateTime> recentAlerts = new ConcurrentHashMap<>();
    private static final long DEDUP_WINDOW_SECONDS = 60; // 1 minute dedup window

    /**
     * Consume alerts from vehicle-alerts topic (from Kafka Streams fast path)
     */
    @KafkaListener(topics = "vehicle-alerts", groupId = "alert-consumer-group", containerFactory = "alertKafkaListenerContainerFactory")
    @Transactional
    public void consumeAlert(String alertJson) {
        try {
            AlertEventDTO alert = objectMapper.readValue(alertJson, AlertEventDTO.class);
            log.debug("🔔 HEAVY PATH: Received alert for {} - {}", alert.getVehicleId(), alert.getAlertType());

            // 1. Deduplication check
            if (isDuplicate(alert)) {
                log.debug("Skipping duplicate alert for {} - {}", alert.getVehicleId(), alert.getAlertType());
                return;
            }

            // 2. Enrich alert (add geofence info if applicable)
            enrichAlert(alert);

            // 3. Persist to DB
            VehicleAlert savedAlert = persistAlert(alert);
            log.info("💾 Alert persisted: {} - {} (ID: {})", 
                    alert.getVehicleId(), alert.getAlertType(), savedAlert.getId());

            // 4. Send SSE notification to frontend
            alertSseService.sendAlertNotification(alert);
            log.debug("📡 SSE notification sent for alert: {}", savedAlert.getId());

        } catch (Exception e) {
            log.error("Error processing alert: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if this is a duplicate alert (same vehicle + type within dedup window)
     */
    private boolean isDuplicate(AlertEventDTO alert) {
        String key = alert.getVehicleId() + ":" + alert.getAlertType();
        OffsetDateTime lastAlert = recentAlerts.get(key);
        OffsetDateTime now = OffsetDateTime.now();

        if (lastAlert != null && 
            lastAlert.plusSeconds(DEDUP_WINDOW_SECONDS).isAfter(now)) {
            return true;
        }

        recentAlerts.put(key, now);
        return false;
    }

    /**
     * Enrich alert with additional info (e.g., geofence name)
     */
    private void enrichAlert(AlertEventDTO alert) {
        if (alert.getLatitude() != null && alert.getLongitude() != null) {
            // Check if vehicle is in any geofence
            List<GeoFenceDTO> geofences = geofenceClient.getGeofencesContainingPoint(
                    alert.getLatitude(), alert.getLongitude());
            
            if (!geofences.isEmpty()) {
                // Add geofence info to message
                StringBuilder geofenceNames = new StringBuilder();
                for (GeoFenceDTO gf : geofences) {
                    if (geofenceNames.length() > 0) geofenceNames.append(", ");
                    geofenceNames.append(gf.getName());
                }
                
                String enrichedMessage = alert.getMessage();
                if (enrichedMessage == null) enrichedMessage = "{}";
                enrichedMessage = enrichedMessage.replace("}", 
                        ",\"geofences\":\"" + geofenceNames + "\"}");
                alert.setMessage(enrichedMessage);
            }
        }
    }

    /**
     * Persist alert to database with snapshot geometry
     */
    private VehicleAlert persistAlert(AlertEventDTO alert) {
        VehicleAlert entity = new VehicleAlert();
        entity.setVehicleId(alert.getVehicleId());
        entity.setAlertType(alert.getAlertType());
        entity.setDetails(alert.getMessage());
        entity.setDetectedAt(OffsetDateTime.now());
        
        // Store snapshot geometry
        if (alert.getLatitude() != null && alert.getLongitude() != null) {
            entity.setGeom(GeometryUtil.createPoint(alert.getLongitude(), alert.getLatitude()));
        }

        return alertRepository.save(entity);
    }
}
