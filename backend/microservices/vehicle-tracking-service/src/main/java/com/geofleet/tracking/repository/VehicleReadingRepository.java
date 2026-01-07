package com.geofleet.tracking.repository;

import com.geofleet.tracking.entity.VehicleReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleReadingRepository extends JpaRepository<VehicleReading, Long> {
}
