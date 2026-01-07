package com.geofleet.query.service;

import com.geofleet.query.entity.DailyVehicleStats;
import com.geofleet.query.repository.AlertHistoryRepository;
import com.geofleet.query.repository.DailyVehicleStatsRepository;
import com.geofleet.query.repository.VehicleReadingHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsAggregationService {

    private final VehicleReadingHistoryRepository readingRepository;
    private final AlertHistoryRepository alertRepository;
    private final DailyVehicleStatsRepository statsRepository;

    /**
     * Aggregate daily stats every 5 minutes for today's data
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void aggregateTodayStats() {
        LocalDate today = LocalDate.now();
        log.debug("Aggregating daily stats for {}", today);

        try {
            List<String> vehicleIds = readingRepository.findDistinctVehicleIds();

            for (String vehicleId : vehicleIds) {
                aggregateStatsForVehicle(vehicleId, today);
            }

            log.info("Aggregated daily stats for {} vehicles", vehicleIds.size());
        } catch (Exception e) {
            log.error("Error aggregating daily stats: {}", e.getMessage(), e);
        }
    }

    private void aggregateStatsForVehicle(String vehicleId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        // Get or create stats record
        DailyVehicleStats stats = statsRepository.findByVehicleIdAndDate(vehicleId, date)
                .orElse(new DailyVehicleStats());

        stats.setVehicleId(vehicleId);
        stats.setDate(date);

        // Aggregate readings
        Double maxSpeed = readingRepository.findMaxSpeedByVehicleIdAndTimestampBetween(
                vehicleId, startOfDay, endOfDay);
        Double avgSpeed = readingRepository.findAvgSpeedByVehicleIdAndTimestampBetween(
                vehicleId, startOfDay, endOfDay);
        Long readingCount = readingRepository.countByVehicleIdAndTimestampBetween(
                vehicleId, startOfDay, endOfDay);
        Double totalDistance = readingRepository.calculateDistanceByVehicleIdAndTimestampBetween(
                vehicleId, startOfDay, endOfDay);

        // Aggregate alerts
        Long alertCount = alertRepository.countByVehicleIdAndTimestampBetween(
                vehicleId, startOfDay, endOfDay);

        stats.setMaxSpeed(maxSpeed != null ? maxSpeed : 0.0);
        stats.setAvgSpeed(avgSpeed != null ? avgSpeed : 0.0);
        stats.setReadingCount(readingCount != null ? readingCount.intValue() : 0);
        stats.setTotalDistance(totalDistance != null ? totalDistance : 0.0);
        stats.setAlertCount(alertCount != null ? alertCount.intValue() : 0);

        statsRepository.save(stats);
    }
}
