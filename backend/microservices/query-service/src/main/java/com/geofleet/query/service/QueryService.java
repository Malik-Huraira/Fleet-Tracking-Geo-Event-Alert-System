package com.geofleet.query.service;

import com.geofleet.query.entity.AlertHistory;
import com.geofleet.query.entity.DailyVehicleStats;
import com.geofleet.query.entity.VehicleReadingHistory;
import com.geofleet.query.repository.AlertHistoryRepository;
import com.geofleet.query.repository.DailyVehicleStatsRepository;
import com.geofleet.query.repository.VehicleReadingHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryService {

    private final VehicleReadingHistoryRepository readingRepository;
    private final AlertHistoryRepository alertRepository;
    private final DailyVehicleStatsRepository statsRepository;

    public List<VehicleReadingHistory> getVehicleHistory(String vehicleId, LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null) {
            return readingRepository.findByVehicleIdAndTimeRange(vehicleId, start, end);
        }
        return readingRepository.findByVehicleIdOrderByTimestampDesc(vehicleId);
    }

    public List<AlertHistory> getVehicleAlerts(String vehicleId) {
        return alertRepository.findByVehicleIdOrderByTimestampDesc(vehicleId);
    }

    public List<DailyVehicleStats> getVehicleStats(String vehicleId, LocalDate start, LocalDate end) {
        if (start != null && end != null) {
            return statsRepository.findByVehicleIdAndDateRange(vehicleId, start, end);
        }
        return statsRepository.findByVehicleIdOrderByDateDesc(vehicleId);
    }

    public List<String> getAllVehicleIds() {
        return readingRepository.findAllVehicleIds();
    }

    public List<AlertHistory> getRecentAlerts(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return alertRepository.findRecentAlerts(since);
    }

    public Map<String, Object> getAnalyticsSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        List<String> vehicleIds = readingRepository.findAllVehicleIds();
        summary.put("totalVehicles", vehicleIds.size());
        summary.put("totalReadings", readingRepository.count());
        summary.put("totalAlerts", alertRepository.count());
        
        // Alert breakdown by type
        List<Object[]> alertsByType = alertRepository.countByAlertType();
        Map<String, Long> alertBreakdown = new HashMap<>();
        for (Object[] row : alertsByType) {
            alertBreakdown.put((String) row[0], (Long) row[1]);
        }
        summary.put("alertsByType", alertBreakdown);
        
        return summary;
    }
}
