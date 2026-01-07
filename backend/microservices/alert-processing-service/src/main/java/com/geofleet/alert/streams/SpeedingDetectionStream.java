package com.geofleet.alert.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geofleet.common.dto.AlertEventDTO;
import com.geofleet.common.dto.VehicleEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.OffsetDateTime;

/**
 * Kafka Streams - SPEEDING detection with location tracking.
 */
@Component
@Slf4j
public class SpeedingDetectionStream {

    @Autowired
    private StreamsBuilder streamsBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${alert.speed.max:90.0}")
    private double speedThreshold;

    @Value("${alert.speed.window-seconds:5}")
    private int windowSeconds;

    private static final String VEHICLE_GPS_TOPIC = "vehicle-gps";
    private static final String VEHICLE_ALERTS_TOPIC = "vehicle-alerts";

    @PostConstruct
    public void buildSpeedingPipeline() {
        log.info("Building Kafka Streams SPEEDING detection pipeline (threshold: {} km/h, window: {}s)",
                speedThreshold, windowSeconds);

        KStream<String, String> gpsStream = streamsBuilder.stream(
                VEHICLE_GPS_TOPIC,
                Consumed.with(Serdes.String(), Serdes.String()));

        // Filter speeding events and map to alert immediately (simpler approach)
        gpsStream
                .filter((vehicleId, eventJson) -> {
                    try {
                        VehicleEventDTO event = objectMapper.readValue(eventJson, VehicleEventDTO.class);
                        return event.getSpeed() != null && event.getSpeed() > speedThreshold;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .mapValues((vehicleId, eventJson) -> {
                    try {
                        VehicleEventDTO event = objectMapper.readValue(eventJson, VehicleEventDTO.class);

                        AlertEventDTO alert = new AlertEventDTO();
                        alert.setVehicleId(vehicleId);
                        alert.setAlertType("SPEEDING");
                        alert.setMessage(String.format(
                                "{\"threshold\":%.1f,\"current_speed\":%.1f}",
                                speedThreshold, event.getSpeed()));
                        alert.setTimestamp(OffsetDateTime.now());
                        alert.setSeverity("HIGH");
                        alert.setLatitude(event.getLatitude());
                        alert.setLongitude(event.getLongitude());

                        log.info("⚡ SPEEDING detected for {} at ({}, {}) - speed: {:.1f} km/h",
                                vehicleId, event.getLatitude(), event.getLongitude(), event.getSpeed());

                        return objectMapper.writeValueAsString(alert);
                    } catch (Exception e) {
                        log.error("Error creating speeding alert: {}", e.getMessage());
                        return null;
                    }
                })
                .filter((k, v) -> v != null)
                .to(VEHICLE_ALERTS_TOPIC, Produced.with(Serdes.String(), Serdes.String()));

        log.info("✅ Kafka Streams SPEEDING pipeline built");
    }
}
