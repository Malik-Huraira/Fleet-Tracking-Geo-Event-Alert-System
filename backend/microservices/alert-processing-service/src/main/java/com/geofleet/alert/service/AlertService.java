package com.geofleet.alert.service;

import com.geofleet.alert.entity.VehicleAlert;
import com.geofleet.alert.repository.VehicleAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final VehicleAlertRepository alertRepository;

    public List<VehicleAlert> getAllAlerts() {
        return alertRepository.findAllOrderByDetectedAtDesc();
    }

    public List<VehicleAlert> getAlertsByVehicle(String vehicleId) {
        return alertRepository.findByVehicleIdOrderByDetectedAtDesc(vehicleId);
    }

    public List<VehicleAlert> getRecentAlerts(int hours) {
        OffsetDateTime since = OffsetDateTime.now().minusHours(hours);
        return alertRepository.findRecentAlerts(since);
    }
}
