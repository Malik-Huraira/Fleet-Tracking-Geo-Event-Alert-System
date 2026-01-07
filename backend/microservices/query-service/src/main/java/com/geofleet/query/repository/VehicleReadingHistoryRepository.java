package com.geofleet.query.repository;

import com.geofleet.query.entity.VehicleReadingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VehicleReadingHistoryRepository extends JpaRepository<VehicleReadingHistory, Long> {

        List<VehicleReadingHistory> findByVehicleIdOrderByTimestampDesc(String vehicleId);

        @Query("SELECT v FROM VehicleReadingHistory v WHERE v.vehicleId = :vehicleId " +
                        "AND v.timestamp BETWEEN :start AND :end ORDER BY v.timestamp")
        List<VehicleReadingHistory> findByVehicleIdAndTimeRange(
                        @Param("vehicleId") String vehicleId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("SELECT DISTINCT v.vehicleId FROM VehicleReadingHistory v")
        List<String> findAllVehicleIds();

        @Query("SELECT DISTINCT v.vehicleId FROM VehicleReadingHistory v")
        List<String> findDistinctVehicleIds();

        @Query("SELECT MAX(v.speed) FROM VehicleReadingHistory v WHERE v.vehicleId = :vehicleId " +
                        "AND v.timestamp BETWEEN :start AND :end")
        Double findMaxSpeedByVehicleIdAndTimestampBetween(
                        @Param("vehicleId") String vehicleId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("SELECT AVG(v.speed) FROM VehicleReadingHistory v WHERE v.vehicleId = :vehicleId " +
                        "AND v.timestamp BETWEEN :start AND :end")
        Double findAvgSpeedByVehicleIdAndTimestampBetween(
                        @Param("vehicleId") String vehicleId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("SELECT COUNT(v) FROM VehicleReadingHistory v WHERE v.vehicleId = :vehicleId " +
                        "AND v.timestamp BETWEEN :start AND :end")
        Long countByVehicleIdAndTimestampBetween(
                        @Param("vehicleId") String vehicleId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query(value = "SELECT COALESCE(SUM(ST_Distance(v1.location::geography, v2.location::geography)) / 1000.0, 0) "
                        +
                        "FROM vehicle_readings_history v1 " +
                        "JOIN vehicle_readings_history v2 ON v1.vehicle_id = v2.vehicle_id AND v2.id = v1.id + 1 " +
                        "WHERE v1.vehicle_id = :vehicleId AND v1.timestamp BETWEEN :start AND :end", nativeQuery = true)
        Double calculateDistanceByVehicleIdAndTimestampBetween(
                        @Param("vehicleId") String vehicleId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);
}
