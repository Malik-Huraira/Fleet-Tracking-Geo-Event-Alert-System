package com.geofleet.alert.repository;

import com.geofleet.alert.entity.VehicleAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface VehicleAlertRepository extends JpaRepository<VehicleAlert, Long> {
    
    @Query("SELECT va FROM VehicleAlert va WHERE va.vehicleId = :vehicleId ORDER BY va.detectedAt DESC")
    List<VehicleAlert> findByVehicleIdOrderByDetectedAtDesc(@Param("vehicleId") String vehicleId);
    
    @Query("SELECT va FROM VehicleAlert va ORDER BY va.detectedAt DESC")
    List<VehicleAlert> findAllOrderByDetectedAtDesc();
    
    @Query("SELECT va FROM VehicleAlert va WHERE va.detectedAt >= :since ORDER BY va.detectedAt DESC")
    List<VehicleAlert> findRecentAlerts(@Param("since") OffsetDateTime since);
}
