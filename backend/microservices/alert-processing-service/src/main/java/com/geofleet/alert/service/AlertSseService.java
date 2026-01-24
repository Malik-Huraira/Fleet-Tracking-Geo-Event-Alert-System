package com.geofleet.alert.service;

import com.geofleet.common.dto.AlertEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;

/**
 * SSE Service using Spring WebFlux for real-time alert notifications.
 * Uses Flux<ServerSentEvent> for reactive streaming.
 */
@Service
@Slf4j
@SuppressWarnings("null")
public class AlertSseService {

    // Sink for broadcasting alerts to all subscribers
    // Using a larger buffer (1024) to handle high-frequency alerts
    private final Sinks.Many<AlertEventDTO> alertSink = Sinks.many().multicast().onBackpressureBuffer(1024);

    /**
     * Get SSE stream for /stream/alerts endpoint using WebFlux.
     * Returns Flux<ServerSentEvent> with keep-alive and reconnection support.
     */
    public Flux<ServerSentEvent<AlertEventDTO>> getAlertStream() {
        log.info("New WebFlux SSE connection for alerts");

        // Alert events stream
        Flux<ServerSentEvent<AlertEventDTO>> alertEvents = alertSink.asFlux()
                .map(alert -> ServerSentEvent.<AlertEventDTO>builder()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .event("alert")
                        .data(alert)
                        .retry(Duration.ofSeconds(3))
                        .build());

        // Keep-alive heartbeat every 15 seconds - send as event to keep connection
        // alive
        Flux<ServerSentEvent<AlertEventDTO>> heartbeat = Flux.interval(Duration.ofSeconds(15))
                .map(seq -> ServerSentEvent.<AlertEventDTO>builder()
                        .event("heartbeat")
                        .data(null)
                        .build());

        // Merge both streams
        return Flux.merge(alertEvents, heartbeat)
                .doOnSubscribe(sub -> log.debug("Client subscribed to alert SSE stream"))
                .doOnCancel(() -> log.debug("Client disconnected from alert SSE stream"))
                .doOnError(e -> log.error("Alert SSE stream error: {}", e.getMessage()));
    }

    /**
     * Send alert notification to all connected SSE clients.
     * Uses reactive Sink for broadcasting.
     */
    public void sendAlertNotification(AlertEventDTO alert) {
        Sinks.EmitResult result = alertSink.tryEmitNext(alert);
        if (result.isFailure()) {
            log.warn("Failed to emit alert: {}", result);
        } else {
            log.debug("Alert emitted to {} subscribers", alertSink.currentSubscriberCount());
        }
    }

    /**
     * Get current subscriber count.
     */
    public int getActiveAlertConnections() {
        return alertSink.currentSubscriberCount();
    }
}
