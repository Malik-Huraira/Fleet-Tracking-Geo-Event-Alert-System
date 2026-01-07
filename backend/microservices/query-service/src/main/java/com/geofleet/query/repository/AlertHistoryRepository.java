package com.geofleet.query.repository;

import com.geofleet.query.entity.AlertHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {

    List<AlertHistory> findByVehicleIdOrderByTimestampDesc(String vehicleId);

    @Query("SELECT a FROM AlertHistory a WHERE a.timestamp >= :since ORDER BY a.timestamp DESC")
    List<AlertHistory> findRecentAlerts(@Param("since") LocalDateTime since);

    @Query("SELECT a.alertType, COUNT(a) FROM AlertHistory a GROUP BY a.alertType")
    List<Object[]> countByAlertType();

    @Query("SELECT COUNT(a) FROM AlertHistory a WHERE a.vehicleId = :vehicleId " +
            "AND a.timestamp BETWEEN :start AND :end")
    Long countByVehicleIdAndTimestampBetween(
            @Param("vehicleId") String vehicleId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
