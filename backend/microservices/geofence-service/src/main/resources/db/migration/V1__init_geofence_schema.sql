-- Enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;

-- =============================================
-- Table: geofences
-- Store polygons (geojson or PostGIS polygon)
-- =============================================
CREATE TABLE geofences (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,                         -- e.g., "Warehouse A"
    polygon_geojson JSONB,                              -- GeoJSON polygon
    polygon_geom GEOMETRY(Polygon, 4326),               -- PostGIS polygon
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Spatial index for geofence queries
CREATE INDEX idx_geofences_polygon_geom ON geofences USING GIST(polygon_geom);

-- Insert sample geofences
INSERT INTO geofences (name, polygon_geojson, polygon_geom) VALUES
('Warehouse A', 
 '{"type": "Polygon", "coordinates": [[[-122.42, 37.78], [-122.40, 37.78], [-122.40, 37.76], [-122.42, 37.76], [-122.42, 37.78]]]}',
 ST_GeomFromText('POLYGON((-122.42 37.78, -122.40 37.78, -122.40 37.76, -122.42 37.76, -122.42 37.78))', 4326)),
('Airport Zone', 
 '{"type": "Polygon", "coordinates": [[[-122.39, 37.62], [-122.36, 37.62], [-122.36, 37.60], [-122.39, 37.60], [-122.39, 37.62]]]}',
 ST_GeomFromText('POLYGON((-122.39 37.62, -122.36 37.62, -122.36 37.60, -122.39 37.60, -122.39 37.62))', 4326)),
('Industrial Park', 
 '{"type": "Polygon", "coordinates": [[[-122.45, 37.70], [-122.43, 37.70], [-122.43, 37.68], [-122.45, 37.68], [-122.45, 37.70]]]}',
 ST_GeomFromText('POLYGON((-122.45 37.70, -122.43 37.70, -122.43 37.68, -122.45 37.68, -122.45 37.70))', 4326));
