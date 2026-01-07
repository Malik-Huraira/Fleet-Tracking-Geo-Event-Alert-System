package com.geofleet.query.controller;

import com.geofleet.query.entity.AlertHistory;
import com.geofleet.query.entity.DailyVehicleStats;
import com.geofleet.query.entity.VehicleReadingHistory;
import com.geofleet.query.service.QueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    @GetMapping("/vehicles/{vehicleId}/history")
    public ResponseEntity<List<VehicleReadingHistory>> getVehicleHistory(
            @PathVariable String vehicleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(queryService.getVehicleHistory(vehicleId, start, end));
    }

    @GetMapping("/vehicles/{vehicleId}/alerts")
    public ResponseEntity<List<AlertHistory>> getVehicleAlerts(@PathVariable String vehicleId) {
        return ResponseEntity.ok(queryService.getVehicleAlerts(vehicleId));
    }

    @GetMapping("/vehicles/{vehicleId}/stats")
    public ResponseEntity<List<DailyVehicleStats>> getVehicleStats(
            @PathVariable String vehicleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(queryService.getVehicleStats(vehicleId, start, end));
    }

    @GetMapping("/vehicles")
    public ResponseEntity<List<String>> getAllVehicleIds() {
        return ResponseEntity.ok(queryService.getAllVehicleIds());
    }

    @GetMapping("/alerts/recent")
    public ResponseEntity<List<AlertHistory>> getRecentAlerts(
            @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(queryService.getRecentAlerts(hours));
    }

    @GetMapping("/analytics/summary")
    public ResponseEntity<Map<String, Object>> getAnalyticsSummary() {
        return ResponseEntity.ok(queryService.getAnalyticsSummary());
    }
}
