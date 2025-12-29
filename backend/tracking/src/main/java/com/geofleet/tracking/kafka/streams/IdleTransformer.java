package com.geofleet.tracking.kafka.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geofleet.tracking.model.dto.AlertEventDTO;
import com.geofleet.tracking.model.dto.VehicleEventDTO;
import com.geofleet.tracking.model.enums.AlertType;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.stereotype.Component;
import java.time.ZoneOffset;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class IdleTransformer implements Transformer<String, VehicleEventDTO, KeyValue<String, String>> {

    private final Duration idleThreshold;
    private final ObjectMapper objectMapper;

    private KeyValueStore<String, IdleState> idleStateStore;
    private ProcessorContext context;

    public IdleTransformer(Duration idleThreshold, ObjectMapper objectMapper) {
        this.idleThreshold = idleThreshold;
        this.objectMapper = objectMapper;
    }

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        this.idleStateStore = (KeyValueStore<String, IdleState>) context.getStateStore("idle-state-store");
    }

    @Override
    public KeyValue<String, String> transform(String vehicleId, VehicleEventDTO event) {
        if (event == null || event.getSpeedKph() == null) {
            return KeyValue.pair(vehicleId, null);
        }

        try {
            // Use event timestamp if available, otherwise use processing time
            Instant timestamp = event.getTimestamp() != null
                    ? event.getTimestamp().toInstant(ZoneOffset.UTC)
                    : Instant.ofEpochMilli(context.timestamp());

            IdleState idleState = idleStateStore.get(vehicleId);

            if (idleState == null) {
                idleState = new IdleState();
                idleState.setVehicleId(vehicleId);
            }

            // Update idle state with current speed
            idleState.update(event.getSpeedKph(), timestamp);

            // Check if we should trigger an alert
            String alertJson = null;
            if (idleState.shouldTriggerAlert(idleThreshold)) {
                alertJson = createIdleAlert(event, idleState);
                idleState.markAlertSent();
                log.info("🚨 Triggered idle alert for {} after {} minutes of being stationary",
                        vehicleId, idleThreshold.toMinutes());
            }

            // Save updated state
            idleStateStore.put(vehicleId, idleState);

            return KeyValue.pair(vehicleId, alertJson);

        } catch (Exception e) {
            log.error("❌ Error processing idle detection for vehicle {}: {}", vehicleId, e.getMessage(), e);
            return KeyValue.pair(vehicleId, null);
        }
    }

    @Override
    public void close() {
        log.debug("Closing IdleTransformer");
    }

    private String createIdleAlert(VehicleEventDTO event, IdleState idleState) {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("idleMinutes", idleThreshold.toMinutes());
            details.put("firstIdleTime", idleState.getFirstIdleTime().toString());
            details.put("currentSpeed", event.getSpeedKph());
            details.put("location", String.format("%f,%f", event.getLat(), event.getLng()));
            details.put("durationMinutes", idleThreshold.toMinutes());
            details.put("reason", "Vehicle has been stationary for " + idleThreshold.toMinutes() + " minutes");

            AlertEventDTO alert = new AlertEventDTO();
            alert.setVehicleId(event.getVehicleId());
            alert.setAlertType(AlertType.IDLE);
            alert.setDetails(details);
            alert.setTimestamp(LocalDateTime.now());
            alert.setLat(event.getLat());
            alert.setLng(event.getLng());

            String alertJson = objectMapper.writeValueAsString(alert);
            log.debug("Created idle alert JSON: {}", alertJson);
            return alertJson;

        } catch (Exception e) {
            log.error("❌ Failed to create idle alert: {}", e.getMessage());
            return null;
        }
    }
}