package com.geofleet.query.kafka;

import com.geofleet.common.dto.AlertEventDTO;
import com.geofleet.common.dto.VehicleEventDTO;
import com.geofleet.query.entity.AlertHistory;
import com.geofleet.query.entity.VehicleReadingHistory;
import com.geofleet.query.repository.AlertHistoryRepository;
import com.geofleet.query.repository.VehicleReadingHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueryKafkaConsumer {

    private final VehicleReadingHistoryRepository readingRepository;
    private final AlertHistoryRepository alertRepository;

    @KafkaListener(topics = "vehicle-gps", groupId = "query-service-group", containerFactory = "vehicleKafkaListenerContainerFactory")
    public void consumeVehicleEvent(VehicleEventDTO event) {
        log.debug("Syncing vehicle event to query DB: {}", event.getVehicleId());
        try {
            if (event.getLatitude() == null || event.getLongitude() == null) {
                log.warn("Skipping event with null coordinates for vehicle: {}", event.getVehicleId());
                return;
            }

            VehicleReadingHistory reading = new VehicleReadingHistory();
            reading.setVehicleId(event.getVehicleId());
            reading.setLat(event.getLatitude());
            reading.setLng(event.getLongitude());
            reading.setSpeed(event.getSpeed());
            reading.setHeading(event.getHeading());
            reading.setTimestamp(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now());
            reading.setSyncedAt(LocalDateTime.now());

            readingRepository.save(reading);
        } catch (Exception e) {
            log.error("Error syncing vehicle event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "vehicle-alerts", groupId = "query-service-group", containerFactory = "alertKafkaListenerContainerFactory")
    public void consumeAlertEvent(AlertEventDTO event) {
        log.debug("Syncing alert event to query DB: {}", event.getVehicleId());
        try {
            AlertHistory alert = new AlertHistory();
            alert.setVehicleId(event.getVehicleId());
            alert.setAlertType(event.getAlertType());
            alert.setMessage(event.getMessage());
            alert.setLatitude(event.getLatitude() != null ? BigDecimal.valueOf(event.getLatitude()) : null);
            alert.setLongitude(event.getLongitude() != null ? BigDecimal.valueOf(event.getLongitude()) : null);
            alert.setTimestamp(
                    event.getTimestamp() != null ? event.getTimestamp().toLocalDateTime() : LocalDateTime.now());
            alert.setSyncedAt(LocalDateTime.now());

            alertRepository.save(alert);
        } catch (Exception e) {
            log.error("Error syncing alert event: {}", e.getMessage(), e);
        }
    }
}
