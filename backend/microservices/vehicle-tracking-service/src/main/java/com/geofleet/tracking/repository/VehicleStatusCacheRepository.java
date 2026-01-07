package com.geofleet.tracking.repository;

import com.geofleet.tracking.entity.VehicleStatusCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleStatusCacheRepository extends JpaRepository<VehicleStatusCache, String> {
    
    Optional<VehicleStatusCache> findByVehicleId(String vehicleId);
}
