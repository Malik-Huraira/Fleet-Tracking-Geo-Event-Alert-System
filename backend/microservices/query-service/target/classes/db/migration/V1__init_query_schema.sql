-- Enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;

-- =============================================
-- Historical vehicle readings (synced via Kafka)
-- =============================================
CREATE TABLE vehicle_readings_history (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id VARCHAR(50) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    location GEOMETRY(Point, 4326),
    speed DOUBLE PRECISION,
    heading DOUBLE PRECISION,
    timestamp TIMESTAMP NOT NULL,
    synced_at TIMESTAMP DEFAULT NOW()
);

-- =============================================
-- Historical alerts (synced via Kafka)
-- =============================================
CREATE TABLE alerts_history (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id VARCHAR(50) NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    message TEXT,
    latitude NUMERIC(9,6),
    longitude NUMERIC(9,6),
    timestamp TIMESTAMP NOT NULL,
    synced_at TIMESTAMP DEFAULT NOW()
);

-- =============================================
-- Analytics aggregations
-- =============================================
CREATE TABLE daily_vehicle_stats (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id VARCHAR(50) NOT NULL,
    date DATE NOT NULL,
    total_distance DOUBLE PRECISION DEFAULT 0,
    max_speed DOUBLE PRECISION DEFAULT 0,
    avg_speed DOUBLE PRECISION DEFAULT 0,
    reading_count INTEGER DEFAULT 0,
    alert_count INTEGER DEFAULT 0,
    UNIQUE(vehicle_id, date)
);

-- Indexes
CREATE INDEX idx_readings_history_vehicle_ts ON vehicle_readings_history(vehicle_id, timestamp DESC);
CREATE INDEX idx_readings_history_location ON vehicle_readings_history USING GIST(location);
CREATE INDEX idx_alerts_history_vehicle ON alerts_history(vehicle_id, timestamp DESC);
CREATE INDEX idx_daily_stats_vehicle_date ON daily_vehicle_stats(vehicle_id, date);

-- Trigger to auto-update location column
CREATE OR REPLACE FUNCTION update_history_location()
RETURNS TRIGGER AS $$
BEGIN
    NEW.location := ST_SetSRID(ST_MakePoint(NEW.lng, NEW.lat), 4326);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_history_location
    BEFORE INSERT OR UPDATE ON vehicle_readings_history
    FOR EACH ROW
    EXECUTE FUNCTION update_history_location();
