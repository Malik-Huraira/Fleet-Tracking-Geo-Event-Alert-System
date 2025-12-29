package com.geofleet.tracking.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DlqConsumer {

    private static final String DLQ_TOPIC = "vehicle-gps-dlq";

    @KafkaListener(topics = DLQ_TOPIC, groupId = "dlq-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeDlqMessage(
            ConsumerRecord<String, String> record,
            Acknowledgment acknowledgment) {

        String key = record.key();
        String payload = record.value();
        long offset = record.offset();
        int partition = record.partition();

        // Extract diagnostic headers
        Map<String, String> headers = new HashMap<>();
        for (Header header : record.headers()) {
            String headerKey = header.key();
            String headerValue = new String(header.value(), StandardCharsets.UTF_8);
            headers.put(headerKey, headerValue);
        }

        log.error("🚨 DLQ MESSAGE RECEIVED - Failed GPS event routed to dead letter queue");
        log.error("   Vehicle Key      : {}", key);
        log.error("   Partition        : {}", partition);
        log.error("   Offset           : {}", offset);
        log.error("   Payload          : {}", payload);
        log.error("   Diagnostic Headers:");
        headers.forEach((k, v) -> log.error("       {} = {}", k, v));

        // Optional: You could save this to a failed_messages table here for admin
        // review

        // Acknowledge to commit offset - message won't be redelivered
        acknowledgment.acknowledge();

        log.info("✅ DLQ message acknowledged (offset {})", offset);
    }
}