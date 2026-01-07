-- Enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;

-- =============================================
-- Table: vehicle_alerts
-- Alerts produced by processing
-- =============================================
CREATE TABLE vehicle_alerts (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id VARCHAR(50) NOT NULL,
    alert_type VARCHAR(50) NOT NULL,                    -- SPEEDING / GEOFENCE_ENTER / GEOFENCE_EXIT / IDLE
    details TEXT,                                        -- JSON string e.g., {"speed": 120, "threshold": 80}
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL,      -- When alert was detected
    geom GEOGRAPHY(Point, 4326)                         -- Optional snapshot location
);

-- Indexes for vehicle_alerts
CREATE INDEX idx_vehicle_alerts_vehicle_detected ON vehicle_alerts(vehicle_id, detected_at DESC);
CREATE INDEX idx_vehicle_alerts_type ON vehicle_alerts(alert_type);
CREATE INDEX idx_vehicle_alerts_geom ON vehicle_alerts USING GIST(geom);
