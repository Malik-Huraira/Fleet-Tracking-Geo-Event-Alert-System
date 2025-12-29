package com.geofleet.tracking.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geofleet.tracking.exception.InvalidEventException;
import com.geofleet.tracking.model.dto.VehicleEventDTO;
import com.geofleet.tracking.service.VehicleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;

import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleGpsConsumer {

    private final ObjectMapper objectMapper;
    private final VehicleService vehicleService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String DLQ_TOPIC = "vehicle-gps-dlq";

    @KafkaListener(topics = "vehicle-gps", groupId = "vehicle-consumer-group")
    public void consume(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String originalTopic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
            @Header(KafkaHeaders.OFFSET) Long offset,
            Acknowledgment acknowledgment) {

        log.info("📨 Received GPS event | vehicleKey: {} | partition: {} | offset: {}", key, partition, offset);

        try {
            // Deserialize the message
            VehicleEventDTO event = objectMapper.readValue(message, VehicleEventDTO.class);

            // Basic validation - treat as invalid payload if critical fields missing
            if (event.getVehicleId() == null || event.getLat() == null || event.getLng() == null) {
                throw new InvalidEventException("Missing required fields: vehicleId, lat, or lng");
            }

            log.info("🚗 Processing vehicle event: {} | lat: {}, lng: {} | speed: {} kph",
                    event.getVehicleId(), event.getLat(), event.getLng(), event.getSpeedKph());

            // Core business processing
            vehicleService.processVehicleEvent(event);

            // Success → acknowledge immediately
            acknowledgment.acknowledge();
            log.debug("✅ Successfully processed and acknowledged event for vehicle: {}", event.getVehicleId());

        } catch (InvalidEventException e) {
            // Invalid message format → send to DLQ immediately (no retry needed)
            log.warn("⚠️ Invalid GPS event format - routing to DLQ: {}", e.getMessage());
            sendToDlq(message, key, originalTopic, partition, offset, "InvalidPayload", e.getMessage());
            acknowledgment.acknowledge(); // Don't retry invalid messages

        } catch (Exception e) {
            // Any other unexpected error (DB, network, etc.)
            log.error("❌ Failed to process GPS event - routing to DLQ: {}", e.getMessage(), e);
            sendToDlq(message, key, originalTopic, partition, offset, "ProcessingError", e.getMessage());
            acknowledgment.acknowledge(); // Prevent poison pill loop
        }
    }

    /**
     * Sends the failed message to the Dead Letter Queue with rich diagnostic
     * headers
     */
    private void sendToDlq(String originalPayload,
            String key,
            String originalTopic,
            Integer partition,
            Long offset,
            String errorType,
            String errorMessage) {

        try {
            ProducerRecord<String, String> dlqRecord = new ProducerRecord<>(
                    DLQ_TOPIC,
                    partition, // Optional: preserve partition if desired
                    key,
                    originalPayload);

            // Add meaningful headers for later analysis/reprocessing
            dlqRecord.headers().add("dlq-original-topic", originalTopic.getBytes(StandardCharsets.UTF_8));
            dlqRecord.headers().add("dlq-original-partition",
                    String.valueOf(partition).getBytes(StandardCharsets.UTF_8));
            dlqRecord.headers().add("dlq-original-offset", String.valueOf(offset).getBytes(StandardCharsets.UTF_8));
            dlqRecord.headers().add("dlq-error-type", errorType.getBytes(StandardCharsets.UTF_8));
            dlqRecord.headers().add("dlq-error-message", errorMessage.getBytes(StandardCharsets.UTF_8));
            dlqRecord.headers().add("dlq-failed-at",
                    String.valueOf(Instant.now().toEpochMilli()).getBytes(StandardCharsets.UTF_8));
            dlqRecord.headers().add("dlq-original-payload-length",
                    String.valueOf(originalPayload.length()).getBytes(StandardCharsets.UTF_8));

            kafkaTemplate.send(dlqRecord)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("✅ Successfully routed failed message to DLQ: {} | offset: {}", key, offset);
                        } else {
                            log.error("💥 Failed to send message to DLQ topic '{}': {}", DLQ_TOPIC, ex.getMessage(),
                                    ex);
                        }
                    });

        } catch (Exception e) {
            log.error("💥 CRITICAL: Failed even to route message to DLQ - data loss risk!", e);
            // At this point, you might want to alert (e.g., via metrics or external
            // monitoring)
        }
    }
}