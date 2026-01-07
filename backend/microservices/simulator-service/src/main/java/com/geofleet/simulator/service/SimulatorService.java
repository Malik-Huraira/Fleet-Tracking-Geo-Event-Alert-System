package com.geofleet.simulator.service;

import com.geofleet.common.dto.VehicleEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class SimulatorService {

    private final WebClient webClient;
    private final Random random = new Random();
    private final Map<String, VehicleState> vehicles = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${simulator.vehicles.count:10}")
    private int vehicleCount;

    @Value("${simulator.vehicles.prefix:VH}")
    private String vehiclePrefix;

    @Value("${simulator.area.center-lat:37.7749}")
    private double centerLat;

    @Value("${simulator.area.center-lon:-122.4194}")
    private double centerLon;

    @Value("${simulator.area.radius-km:10}")
    private double radiusKm;

    public SimulatorService(@Value("${tracking.service.url}") String trackingServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(trackingServiceUrl)
                .build();
    }

    @Value("${simulator.auto-start:true}")
    private boolean autoStart;

    @Value("${simulator.startup-delay-seconds:15}")
    private int startupDelaySeconds;

    @PostConstruct
    public void init() {
        initializeVehicles();

        // Auto-start simulator on boot with delay to wait for tracking service
        if (autoStart) {
            log.info("Waiting {} seconds for tracking service to be ready...", startupDelaySeconds);
            new Thread(() -> {
                try {
                    Thread.sleep(startupDelaySeconds * 1000L);
                    log.info("Auto-starting simulator...");
                    start();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    private void initializeVehicles() {
        vehicles.clear();
        for (int i = 1; i <= vehicleCount; i++) {
            String vehicleId = String.format("%s-%03d", vehiclePrefix, i);
            double lat = centerLat + (random.nextDouble() - 0.5) * (radiusKm / 111.0);
            double lon = centerLon + (random.nextDouble() - 0.5) * (radiusKm / 85.0);
            double heading = random.nextDouble() * 360;
            double speed = 20 + random.nextDouble() * 60;

            vehicles.put(vehicleId, new VehicleState(lat, lon, heading, speed));
        }
        log.info("Initialized {} vehicles", vehicleCount);
    }

    @Scheduled(fixedRateString = "${simulator.update.interval-ms:2000}")
    public void simulateMovement() {
        if (!running.get())
            return;

        vehicles.forEach((vehicleId, state) -> {
            updateVehiclePosition(state);
            sendGpsUpdate(vehicleId, state);
        });
    }

    private void updateVehiclePosition(VehicleState state) {
        // Randomly adjust heading
        state.heading += (random.nextDouble() - 0.5) * 20;
        if (state.heading < 0)
            state.heading += 360;
        if (state.heading >= 360)
            state.heading -= 360;

        // Randomly adjust speed
        state.speed += (random.nextDouble() - 0.5) * 10;
        state.speed = Math.max(0, Math.min(150, state.speed));

        // Calculate new position
        double distanceKm = (state.speed / 3600.0) * 2; // 2 seconds
        double latChange = distanceKm * Math.cos(Math.toRadians(state.heading)) / 111.0;
        double lonChange = distanceKm * Math.sin(Math.toRadians(state.heading)) / 85.0;

        state.latitude += latChange;
        state.longitude += lonChange;

        // Keep within bounds
        double maxLatDiff = radiusKm / 111.0;
        double maxLonDiff = radiusKm / 85.0;
        if (Math.abs(state.latitude - centerLat) > maxLatDiff ||
                Math.abs(state.longitude - centerLon) > maxLonDiff) {
            state.heading = (state.heading + 180) % 360;
        }
    }

    private void sendGpsUpdate(String vehicleId, VehicleState state) {
        VehicleEventDTO event = new VehicleEventDTO();
        event.setVehicleId(vehicleId);
        event.setLatitude(state.latitude);
        event.setLongitude(state.longitude);
        event.setSpeed(state.speed);
        event.setHeading(state.heading);
        event.setTimestamp(LocalDateTime.now());

        webClient.post()
                .uri("/api/tracking/gps")
                .bodyValue(event)
                .retrieve()
                .bodyToMono(Void.class)
                .retry(3)
                .doOnError(e -> log.debug("Failed to send GPS update for {}: {}", vehicleId, e.getMessage()))
                .subscribe();
    }

    public void start() {
        running.set(true);
        log.info("Simulator started");
    }

    public void stop() {
        running.set(false);
        log.info("Simulator stopped");
    }

    public boolean isRunning() {
        return running.get();
    }

    public void reset() {
        stop();
        initializeVehicles();
    }

    public int getVehicleCount() {
        return vehicles.size();
    }

    public Map<String, VehicleState> getVehicleStates() {
        return vehicles;
    }

    public static class VehicleState {
        public double latitude;
        public double longitude;
        public double heading;
        public double speed;

        public VehicleState(double lat, double lon, double heading, double speed) {
            this.latitude = lat;
            this.longitude = lon;
            this.heading = heading;
            this.speed = speed;
        }
    }
}
