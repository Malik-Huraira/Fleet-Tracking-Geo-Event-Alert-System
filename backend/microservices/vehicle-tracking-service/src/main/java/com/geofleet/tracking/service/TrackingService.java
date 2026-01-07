package com.geofleet.tracking.service;

import com.geofleet.common.dto.VehicleEventDTO;
import com.geofleet.common.dto.VehicleStatusDTO;
import com.geofleet.tracking.entity.VehicleReading;
import com.geofleet.tracking.entity.VehicleStatusCache;
import com.geofleet.tracking.repository.VehicleReadingRepository;
import com.geofleet.tracking.repository.VehicleStatusCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Boot Consumer Service:
 * - Validates payload (vehicleId not null, coordinates valid)
 * - Persists to vehicle_readings
 * - Updates vehicle_status_cache
 * - Emits latest vehicle info to SSE clients
 * - Publishes to Kafka for downstream services
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingService {

    private final VehicleReadingRepository readingRepository;
    private final VehicleStatusCacheRepository statusCacheRepository;
    private final KafkaTemplate<String, VehicleEventDTO> kafkaTemplate;
    private final SseEmitterService sseEmitterService;

    private static final String VEHICLE_GPS_TOPIC = "vehicle-gps";

    @Transactional
    public VehicleReading processGpsData(VehicleEventDTO event) {
        log.debug("Processing GPS data for vehicle: {}", event.getVehicleId());

        // 1. Persist to vehicle_readings
        VehicleReading reading = new VehicleReading();
        reading.setVehicleId(event.getVehicleId());
        reading.setLat(BigDecimal.valueOf(event.getLatitude()));
        reading.setLng(BigDecimal.valueOf(event.getLongitude()));
        reading.setSpeedKph(event.getSpeed() != null ? BigDecimal.valueOf(event.getSpeed()) : null);
        reading.setHeading(event.getHeading() != null ? BigDecimal.valueOf(event.getHeading()) : null);
        reading.setEventTimestamp(event.getTimestamp() != null ? 
                event.getTimestamp().atOffset(OffsetDateTime.now().getOffset()) : 
                OffsetDateTime.now());
        readingRepository.save(reading);

        // 2. Update vehicle_status_cache (for quick SSE reads)
        updateVehicleStatusCache(event);

        // 3. Publish to Kafka (partitioned by vehicleId) for downstream services
        kafkaTemplate.send(VEHICLE_GPS_TOPIC, event.getVehicleId(), event);
        log.debug("Published GPS event to Kafka topic {} for vehicle: {}", VEHICLE_GPS_TOPIC, event.getVehicleId());

        // 4. Emit latest vehicle info to SSE clients
        VehicleStatusDTO statusDTO = convertToStatusDTO(event);
        sseEmitterService.sendVehicleUpdate(statusDTO);

        return reading;
    }

    private void updateVehicleStatusCache(VehicleEventDTO event) {
        VehicleStatusCache cache = statusCacheRepository.findByVehicleId(event.getVehicleId())
                .orElse(new VehicleStatusCache());

        cache.setVehicleId(event.getVehicleId());
        cache.setLastLat(BigDecimal.valueOf(event.getLatitude()));
        cache.setLastLng(BigDecimal.valueOf(event.getLongitude()));
        cache.setLastSpeed(event.getSpeed() != null ? BigDecimal.valueOf(event.getSpeed()) : null);
        cache.setLastSeen(OffsetDateTime.now());
        cache.setStatus("ACTIVE");

        statusCacheRepository.save(cache);
    }

    public List<VehicleStatusDTO> getAllVehicleStatuses() {
        return statusCacheRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public VehicleStatusDTO getVehicleStatus(String vehicleId) {
        return statusCacheRepository.findByVehicleId(vehicleId)
                .map(this::convertToDTO)
                .orElse(null);
    }

    private VehicleStatusDTO convertToDTO(VehicleStatusCache cache) {
        VehicleStatusDTO dto = new VehicleStatusDTO();
        dto.setVehicleId(cache.getVehicleId());
        dto.setLatitude(cache.getLastLat() != null ? cache.getLastLat().doubleValue() : null);
        dto.setLongitude(cache.getLastLng() != null ? cache.getLastLng().doubleValue() : null);
        dto.setSpeed(cache.getLastSpeed() != null ? cache.getLastSpeed().doubleValue() : null);
        dto.setStatus(cache.getStatus());
        dto.setLastUpdate(cache.getLastSeen() != null ? cache.getLastSeen().toLocalDateTime() : null);
        return dto;
    }

    private VehicleStatusDTO convertToStatusDTO(VehicleEventDTO event) {
        VehicleStatusDTO dto = new VehicleStatusDTO();
        dto.setVehicleId(event.getVehicleId());
        dto.setLatitude(event.getLatitude());
        dto.setLongitude(event.getLongitude());
        dto.setSpeed(event.getSpeed());
        dto.setHeading(event.getHeading());
        dto.setStatus("ACTIVE");
        dto.setLastUpdate(event.getTimestamp());
        return dto;
    }
}
