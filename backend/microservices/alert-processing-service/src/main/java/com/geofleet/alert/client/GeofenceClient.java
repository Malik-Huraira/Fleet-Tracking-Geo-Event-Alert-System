package com.geofleet.alert.client;

import com.geofleet.common.dto.GeoFenceDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class GeofenceClient {

    private final WebClient webClient;

    public GeofenceClient(@Value("${geofence.service.url}") String geofenceServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(geofenceServiceUrl)
                .build();
    }

    public List<GeoFenceDTO> getGeofencesContainingPoint(double latitude, double longitude) {
        try {
            return webClient.get()
                    .uri("/api/geofences/containing?lat={lat}&lon={lon}", latitude, longitude)
                    .retrieve()
                    .bodyToFlux(GeoFenceDTO.class)
                    .collectList()
                    .onErrorResume(e -> {
                        log.error("Error calling geofence service: {}", e.getMessage());
                        return Mono.just(Collections.emptyList());
                    })
                    .block();
        } catch (Exception e) {
            log.error("Failed to get geofences for point ({}, {}): {}", latitude, longitude, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<GeoFenceDTO> getAllActiveGeofences() {
        try {
            return webClient.get()
                    .uri("/api/geofences")
                    .retrieve()
                    .bodyToFlux(GeoFenceDTO.class)
                    .collectList()
                    .onErrorResume(e -> {
                        log.error("Error fetching all geofences: {}", e.getMessage());
                        return Mono.just(Collections.emptyList());
                    })
                    .block();
        } catch (Exception e) {
            log.error("Failed to get all geofences: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
