package com.geofleet.tracking.service;

import com.geofleet.common.dto.VehicleStatusDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;

/**
 * SSE Service using Spring WebFlux for real-time vehicle updates.
 * Uses Flux<ServerSentEvent> for reactive streaming.
 */
@Service
@Slf4j
@SuppressWarnings("null")
public class SseEmitterService {

    // Sink for broadcasting vehicle updates to all subscribers
    // Using a larger buffer (256) to handle high-frequency updates
    private final Sinks.Many<VehicleStatusDTO> vehicleSink = Sinks.many().multicast().onBackpressureBuffer(256);

    /**
     * Get SSE stream for /stream/vehicles endpoint using WebFlux.
     * Returns Flux<ServerSentEvent> with keep-alive and reconnection support.
     */
    public Flux<ServerSentEvent<VehicleStatusDTO>> getVehicleStream() {
        log.info("New WebFlux SSE connection for vehicles");

        // Merge vehicle updates with keep-alive heartbeats
        Flux<ServerSentEvent<VehicleStatusDTO>> vehicleEvents = vehicleSink.asFlux()
                .map(status -> ServerSentEvent.<VehicleStatusDTO>builder()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .event("vehicle-update")
                        .data(status)
                        .retry(Duration.ofSeconds(3))
                        .build());

        // Keep-alive heartbeat every 15 seconds - send as event to keep connection
        // alive
        Flux<ServerSentEvent<VehicleStatusDTO>> heartbeat = Flux.interval(Duration.ofSeconds(15))
                .map(seq -> ServerSentEvent.<VehicleStatusDTO>builder()
                        .event("heartbeat")
                        .data(null)
                        .build());

        // Merge both streams
        return Flux.merge(vehicleEvents, heartbeat)
                .doOnSubscribe(sub -> log.debug("Client subscribed to vehicle SSE stream"))
                .doOnCancel(() -> log.debug("Client disconnected from vehicle SSE stream"))
                .doOnError(e -> log.error("SSE stream error: {}", e.getMessage()));
    }

    /**
     * Send vehicle update to all connected SSE clients.
     * Uses reactive Sink for broadcasting.
     */
    public void sendVehicleUpdate(VehicleStatusDTO status) {
        // Use tryEmitNext with FAIL_FAST to drop if buffer is full (non-blocking)
        Sinks.EmitResult result = vehicleSink.tryEmitNext(status);
        if (result.isFailure()) {
            // Only log at debug level to avoid log spam
            log.debug("Dropped vehicle update for {} (buffer full)", status.getVehicleId());
        }
    }

    /**
     * Get current subscriber count (approximate).
     */
    public int getActiveVehicleConnections() {
        return vehicleSink.currentSubscriberCount();
    }
}
