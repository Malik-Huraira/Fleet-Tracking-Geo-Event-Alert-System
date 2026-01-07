package com.geofleet.alert.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geofleet.common.dto.AlertEventDTO;
import com.geofleet.common.dto.VehicleEventDTO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * 🅰️ KAFKA STREAMS - FAST PATH
 * 
 * Detects IDLE vehicles using windowed aggregation.
 * No movement (speed < threshold) for X minutes = IDLE alert
 * 
 * ⚡ Runs in-memory, extremely fast
 * ✔ Windowed computations
 * ✔ Stateless/lightweight state
 */
@Component
@Slf4j
public class IdleDetectionStream {

    @Autowired
    private StreamsBuilder streamsBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${alert.idle.minutes:3}")
    private int idleMinutes;

    @Value("${alert.idle.speed-threshold:5.0}")
    private double idleSpeedThreshold;

    private static final String VEHICLE_GPS_TOPIC = "vehicle-gps";
    private static final String VEHICLE_ALERTS_TOPIC = "vehicle-alerts";

    @PostConstruct
    public void buildIdlePipeline() {
        log.info("Building Kafka Streams IDLE detection pipeline (threshold: {} km/h, window: {} min)", 
                idleSpeedThreshold, idleMinutes);

        KStream<String, String> gpsStream = streamsBuilder.stream(
                VEHICLE_GPS_TOPIC,
                Consumed.with(Serdes.String(), Serdes.String())
        );

        // FAST PATH: Detect idle in windowed aggregation
        gpsStream
                .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
                .windowedBy(TimeWindows.ofSizeAndGrace(
                        Duration.ofMinutes(idleMinutes),
                        Duration.ofSeconds(30)))
                .aggregate(
                        // Initialize
                        IdleAggregator::new,
                        // Aggregate
                        (vehicleId, eventJson, agg) -> {
                            try {
                                VehicleEventDTO event = objectMapper.readValue(eventJson, VehicleEventDTO.class);
                                agg.addReading(event);
                            } catch (Exception e) {
                                log.debug("Error parsing event: {}", e.getMessage());
                            }
                            return agg;
                        },
                        Materialized.<String, IdleAggregator, WindowStore<org.apache.kafka.common.utils.Bytes, byte[]>>
                                as("idle-store")
                                .withKeySerde(Serdes.String())
                                .withValueSerde(new IdleAggregatorSerde(objectMapper))
                )
                .toStream()
                // Filter: All readings below speed threshold = IDLE
                .filter((windowedKey, agg) -> {
                    if (agg == null || agg.getCount() < 3) return false;
                    return agg.isIdle(idleSpeedThreshold);
                })
                .map((windowedKey, agg) -> {
                    String vehicleId = windowedKey.key();
                    log.info("⚡ FAST PATH: IDLE detected for {} (avg speed: {:.1f} km/h, {} readings)", 
                            vehicleId, agg.getAvgSpeed(), agg.getCount());
                    
                    try {
                        AlertEventDTO alert = new AlertEventDTO();
                        alert.setVehicleId(vehicleId);
                        alert.setAlertType("IDLE");
                        alert.setMessage(String.format(
                                "{\"avg_speed\":%.1f,\"threshold\":%.1f,\"idle_minutes\":%d,\"readings\":%d}",
                                agg.getAvgSpeed(), idleSpeedThreshold, idleMinutes, agg.getCount()));
                        alert.setLatitude(agg.getLastLat());
                        alert.setLongitude(agg.getLastLng());
                        alert.setTimestamp(OffsetDateTime.now());
                        alert.setSeverity("MEDIUM");
                        
                        String alertJson = objectMapper.writeValueAsString(alert);
                        return KeyValue.pair(vehicleId, alertJson);
                    } catch (Exception e) {
                        return KeyValue.pair(vehicleId, "");
                    }
                })
                .filter((k, v) -> v != null && !v.isEmpty())
                .to(VEHICLE_ALERTS_TOPIC, Produced.with(Serdes.String(), Serdes.String()));

        log.info("✅ Kafka Streams IDLE pipeline built");
    }

    /**
     * Aggregator for idle detection
     */
    @Data
    public static class IdleAggregator {
        private double totalSpeed = 0;
        private int count = 0;
        private double lastLat = 0;
        private double lastLng = 0;

        public void addReading(VehicleEventDTO event) {
            if (event.getSpeed() != null) {
                totalSpeed += event.getSpeed();
                count++;
            }
            if (event.getLatitude() != null) lastLat = event.getLatitude();
            if (event.getLongitude() != null) lastLng = event.getLongitude();
        }

        public double getAvgSpeed() {
            return count > 0 ? totalSpeed / count : 0;
        }

        public boolean isIdle(double threshold) {
            return getAvgSpeed() < threshold;
        }
    }
}
