package com.geofleet.alert.controller;

import com.geofleet.common.dto.AlertEventDTO;
import com.geofleet.alert.entity.VehicleAlert;
import com.geofleet.alert.service.AlertService;
import com.geofleet.alert.service.AlertSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;
    private final AlertSseService alertSseService;

    // ==========================================
    // Alert Query Endpoints
    // ==========================================

    @GetMapping("/api/alerts")
    public ResponseEntity<List<VehicleAlert>> getAllAlerts() {
        return ResponseEntity.ok(alertService.getAllAlerts());
    }

    @GetMapping("/api/alerts/vehicle/{vehicleId}")
    public ResponseEntity<List<VehicleAlert>> getAlertsByVehicle(@PathVariable String vehicleId) {
        return ResponseEntity.ok(alertService.getAlertsByVehicle(vehicleId));
    }

    @GetMapping("/api/alerts/recent")
    public ResponseEntity<List<VehicleAlert>> getRecentAlerts(
            @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(alertService.getRecentAlerts(hours));
    }

    // ==========================================
    // SSE Streaming Endpoint (WebFlux)
    // ==========================================

    /**
     * SSE endpoint for alert notifications using WebFlux.
     * Returns Flux<ServerSentEvent> for reactive streaming.
     * 
     * Features:
     * - Keep-alive heartbeat every 15 seconds
     * - Reconnection-friendly with 3-second retry
     * - Backpressure handling
     * 
     * Usage: EventSource('/stream/alerts')
     */
    @GetMapping(value = "/stream/alerts", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AlertEventDTO>> streamAlerts() {
        return alertSseService.getAlertStream();
    }

    // ==========================================
    // Connection Info
    // ==========================================

    @GetMapping("/api/alerts/connections")
    public ResponseEntity<Map<String, Integer>> getActiveConnections() {
        return ResponseEntity.ok(Map.of(
                "alertStreams", alertSseService.getActiveAlertConnections()));
    }
}
