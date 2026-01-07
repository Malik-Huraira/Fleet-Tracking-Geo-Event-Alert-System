package com.geofleet.tracking.controller;

import com.geofleet.common.dto.VehicleEventDTO;
import com.geofleet.common.dto.VehicleStatusDTO;
import com.geofleet.tracking.service.SseEmitterService;
import com.geofleet.tracking.service.TrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TrackingController {

    private final TrackingService trackingService;
    private final SseEmitterService sseEmitterService;

    // ==========================================
    // GPS Data Ingestion Endpoints
    // ==========================================

    @PostMapping("/api/tracking/gps")
    public ResponseEntity<?> ingestGpsData(@Valid @RequestBody VehicleEventDTO event) {
        // Validate payload
        if (event.getVehicleId() == null || event.getVehicleId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "vehicleId is required"));
        }
        if (event.getLatitude() == null || event.getLongitude() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "coordinates (latitude, longitude) are required"));
        }
        if (event.getLatitude() < -90 || event.getLatitude() > 90) {
            return ResponseEntity.badRequest().body(Map.of("error", "latitude must be between -90 and 90"));
        }
        if (event.getLongitude() < -180 || event.getLongitude() > 180) {
            return ResponseEntity.badRequest().body(Map.of("error", "longitude must be between -180 and 180"));
        }

        log.debug("Received GPS data for vehicle: {}", event.getVehicleId());
        trackingService.processGpsData(event);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/tracking/gps/batch")
    public ResponseEntity<?> ingestGpsBatch(@Valid @RequestBody List<VehicleEventDTO> events) {
        log.debug("Received batch GPS data, count: {}", events.size());
        int processed = 0;
        for (VehicleEventDTO event : events) {
            if (event.getVehicleId() != null && event.getLatitude() != null && event.getLongitude() != null) {
                trackingService.processGpsData(event);
                processed++;
            }
        }
        return ResponseEntity.ok(Map.of("processed", processed, "total", events.size()));
    }

    // ==========================================
    // Vehicle Status Endpoints
    // ==========================================

    @GetMapping("/api/tracking/vehicles")
    public ResponseEntity<List<VehicleStatusDTO>> getAllVehicles() {
        return ResponseEntity.ok(trackingService.getAllVehicleStatuses());
    }

    @GetMapping("/api/tracking/vehicles/{vehicleId}")
    public ResponseEntity<VehicleStatusDTO> getVehicle(@PathVariable String vehicleId) {
        VehicleStatusDTO status = trackingService.getVehicleStatus(vehicleId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    // ==========================================
    // SSE Streaming Endpoint (WebFlux)
    // ==========================================

    /**
     * SSE endpoint for latest vehicle updates using WebFlux.
     * Returns Flux<ServerSentEvent> for reactive streaming.
     * 
     * Features:
     * - Keep-alive heartbeat every 15 seconds
     * - Reconnection-friendly with 3-second retry
     * - Backpressure handling
     * 
     * Usage: EventSource('/stream/vehicles')
     */
    @GetMapping(value = "/stream/vehicles", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<VehicleStatusDTO>> streamVehicles() {
        log.info("New WebFlux SSE connection for /stream/vehicles");
        return sseEmitterService.getVehicleStream();
    }

    // ==========================================
    // Connection Info
    // ==========================================

    @GetMapping("/api/tracking/connections")
    public ResponseEntity<Map<String, Integer>> getActiveConnections() {
        return ResponseEntity.ok(Map.of(
                "vehicleStreams", sseEmitterService.getActiveVehicleConnections()));
    }
}
