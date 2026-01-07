package com.geofleet.simulator.controller;

import com.geofleet.simulator.service.SimulatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/simulator")
@RequiredArgsConstructor
public class SimulatorController {

    private final SimulatorService simulatorService;

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start() {
        simulatorService.start();
        return ResponseEntity.ok(Map.of(
                "status", "started",
                "vehicles", simulatorService.getVehicleCount()));
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stop() {
        simulatorService.stop();
        return ResponseEntity.ok(Map.of("status", "stopped"));
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset() {
        simulatorService.reset();
        return ResponseEntity.ok(Map.of(
                "status", "reset",
                "vehicles", simulatorService.getVehicleCount()));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "running", simulatorService.isRunning(),
                "vehicles", simulatorService.getVehicleCount()));
    }

    @GetMapping("/vehicles")
    public ResponseEntity<Map<String, SimulatorService.VehicleState>> getVehicles() {
        return ResponseEntity.ok(simulatorService.getVehicleStates());
    }
}
