package com.geofleet.query.repository;

import com.geofleet.query.entity.DailyVehicleStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyVehicleStatsRepository extends JpaRepository<DailyVehicleStats, Long> {

       List<DailyVehicleStats> findByVehicleIdOrderByDateDesc(String vehicleId);

       Optional<DailyVehicleStats> findByVehicleIdAndDate(String vehicleId, LocalDate date);

       @Query("SELECT d FROM DailyVehicleStats d WHERE d.vehicleId = :vehicleId " +
                     "AND d.date BETWEEN :start AND :end ORDER BY d.date")
       List<DailyVehicleStats> findByVehicleIdAndDateRange(
                     @Param("vehicleId") String vehicleId,
                     @Param("start") LocalDate start,
                     @Param("end") LocalDate end);
}
