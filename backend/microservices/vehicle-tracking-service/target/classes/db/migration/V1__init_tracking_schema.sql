-- Enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;

-- =============================================
-- Table: vehicle_readings
-- Stores raw GPS events
-- =============================================
CREATE TABLE vehicle_readings (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id VARCHAR(50) NOT NULL,                    -- Partition key for queries
    lat NUMERIC(9,6) NOT NULL,                          -- Latitude
    lng NUMERIC(9,6) NOT NULL,                          -- Longitude
    location GEOGRAPHY(Point, 4326),                    -- PostGIS geography column
    speed_kph NUMERIC(6,2),                             -- Speed in km/h
    heading NUMERIC(6,2),                               -- Heading degrees
    event_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,  -- Time reported
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()   -- Inserted time
);

-- Indexes for vehicle_readings
CREATE INDEX idx_vehicle_readings_vehicle_ts ON vehicle_readings(vehicle_id, event_timestamp DESC);
CREATE INDEX idx_vehicle_readings_location ON vehicle_readings USING GIST(location);

-- =============================================
-- Table: vehicle_status_cache
-- Latest known location for quick SSE reads
-- =============================================
CREATE TABLE vehicle_status_cache (
    vehicle_id VARCHAR(50) PRIMARY KEY,
    last_lat NUMERIC(9,6),
    last_lng NUMERIC(9,6),
    last_speed NUMERIC(6,2),
    last_seen TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    status_updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Trigger to auto-update location column from lat/lng
CREATE OR REPLACE FUNCTION update_location_geography()
RETURNS TRIGGER AS $$
BEGIN
    NEW.location := ST_SetSRID(ST_MakePoint(NEW.lng, NEW.lat), 4326)::geography;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_location
    BEFORE INSERT OR UPDATE ON vehicle_readings
    FOR EACH ROW
    EXECUTE FUNCTION update_location_geography();
