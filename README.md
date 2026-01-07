# GeoFleet - Real-Time Fleet Tracking & Geo-Event Alert System

A real-time fleet tracking platform that ingests GPS events via Kafka, detects geospatial events (geofence entry/exit, speeding, idle), and pushes live updates to a React dashboard using Server-Sent Events (SSE).

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         API Gateway (8080)                              │
└─────────────────────────────────────────────────────────────────────────┘
                                  │ 
   ───────────────┬───────────────┼───────────────┬───────────────┐
  │               │               │               │               │
  ▼               ▼               ▼               ▼               ▼
┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐
│ Vehicle │   │  Alert  │   │Geofence │   │  Query  │   │Simulator│
│Tracking │──▶│Processing│──▶│ Service │   │ Service │   │ Service │
│ (8081)  │   │ (8082)  │   │ (8083)  │   │ (8084)  │   │ (8086)  │
└─────────┘   └─────────┘   └─────────┘   └─────────┘   └─────────┘
     │             │
     └─────┬───────┘
           ▼
    ┌─────────────┐
    │    Kafka    │
    │  (vehicle-  │
    │  gps/alerts)│
    └─────────────┘
```

## 🛠️ Technology Stack

| Layer            | Technology                            |
| ---------------- | ------------------------------------- |
| Backend          | Java 17, Spring Boot 3.5.0            |
| Streaming        | Apache Kafka, Kafka Streams           |
| Database         | PostgreSQL 15 + PostGIS 3.3           |
| SSE              | Spring WebFlux                        |
| Frontend         | React 18, Vite, Leaflet, Tailwind CSS |
| Containerization | Docker, Docker Compose                |
| Monitoring       | Prometheus, Grafana                   |

## 📁 Project Structure

```
GeoFleet/
├── backend/
│   └── microservices/
│       ├── api-gateway/              # Spring Cloud Gateway (8080)
│       ├── vehicle-tracking-service/ # GPS ingestion + SSE (8081)
│       ├── alert-processing-service/ # Kafka Streams + Alerts (8082)
│       ├── geofence-service/         # PostGIS queries (8083)
│       ├── query-service/            # Historical data (8084)
│       ├── simulator-service/        # GPS simulator (8086)
│       └── common/                   # Shared DTOs & entities
├── frontend/
│   └── geofleet-dashboard/           # React SPA
├── docker-compose.microservices.yml  # Full stack deployment
└── prometheus.microservices.yml      # Monitoring config
```

## 🚀 Quick Start

### Prerequisites

- Docker & Docker Compose
- Java 17+ (for local development)
- Node.js 18+ (for frontend)

### Run with Docker

```bash
# Start all services
docker-compose -f docker-compose.microservices.yml up --build

# Start GPS simulator
curl -X POST http://localhost:8080/api/simulator/start

# View vehicles
curl http://localhost:8080/api/tracking/vehicles

# Connect to SSE streams
curl http://localhost:8080/api/stream/vehicles
curl http://localhost:8080/api/stream/alerts
```

### Access Points

| Service           | URL                   |
| ----------------- | --------------------- |
| API Gateway       | http://localhost:8080 |
| Frontend (Docker) | http://localhost:3000 |
| Frontend (Dev)    | http://localhost:5173 |
| Prometheus        | http://localhost:9090 |
| Grafana           | http://localhost:3001 |

## 🔔 Alert Types

| Alert          | Detection Method                   | Threshold                |
| -------------- | ---------------------------------- | ------------------------ |
| SPEEDING       | Kafka Streams filter               | > 90 km/h (configurable) |
| IDLE           | Kafka Streams windowed aggregation | < 5 km/h for 3 min       |
| GEOFENCE_ENTER | PostGIS ST_Contains                | Point enters polygon     |
| GEOFENCE_EXIT  | PostGIS + State tracking           | Point exits polygon      |

## 📊 Kafka Topics

| Topic             | Partitions | Purpose             |
| ----------------- | ---------- | ------------------- |
| `vehicle-gps`     | 6          | Raw GPS events      |
| `vehicle-alerts`  | 3          | Alert notifications |
| `vehicle-gps-dlq` | 3          | Dead letter queue   |

## 🗄️ Database Schema

### vehicle_readings (Vehicle Tracking DB - 5433)

| Column          | Type                   |
| --------------- | ---------------------- |
| id              | BIGSERIAL PK           |
| vehicle_id      | VARCHAR(50)            |
| lat             | NUMERIC(9,6)           |
| lng             | NUMERIC(9,6)           |
| location        | GEOGRAPHY(Point, 4326) |
| speed_kph       | NUMERIC(6,2)           |
| heading         | NUMERIC(6,2)           |
| event_timestamp | TIMESTAMPTZ            |

### vehicle_status_cache (Vehicle Tracking DB - 5433)

| Column     | Type           |
| ---------- | -------------- |
| vehicle_id | VARCHAR(50) PK |
| last_lat   | NUMERIC(9,6)   |
| last_lng   | NUMERIC(9,6)   |
| last_speed | NUMERIC(6,2)   |
| last_seen  | TIMESTAMPTZ    |
| status     | VARCHAR(20)    |

### vehicle_alerts (Alert DB - 5434)

| Column      | Type                   |
| ----------- | ---------------------- |
| id          | BIGSERIAL PK           |
| vehicle_id  | VARCHAR(50)            |
| alert_type  | VARCHAR(50)            |
| details     | TEXT (JSON)            |
| detected_at | TIMESTAMPTZ            |
| geom        | GEOGRAPHY(Point, 4326) |

### geofences (Geofence DB - 5435)

| Column          | Type                    |
| --------------- | ----------------------- |
| id              | BIGSERIAL PK            |
| name            | VARCHAR(100)            |
| polygon_geojson | JSONB                   |
| polygon_geom    | GEOMETRY(Polygon, 4326) |

## 📡 SSE Endpoints

| Endpoint               | Description                   |
| ---------------------- | ----------------------------- |
| `/api/stream/vehicles` | Real-time vehicle positions   |
| `/api/stream/alerts`   | Real-time alert notifications |

**Features:**

- WebFlux `Flux<ServerSentEvent>`
- Keep-alive heartbeat (15s)
- Auto-reconnection (3s retry)
- Backpressure handling

## 🐳 Docker Containers

```
DATABASES
├── backend-db-vehicle-tracking  (5433)
├── backend-db-alert             (5434)
├── backend-db-geofence          (5435)
└── backend-db-query             (5436)

MESSAGE BROKER
├── backend-zookeeper            (2181)
├── backend-kafka                (9092, 29092)
└── backend-kafka-init           (topic creation)

MICROSERVICES
├── backend-service-api-gateway        (8080)
├── backend-service-vehicle-tracking   (8081)
├── backend-service-alert-processing   (8082)
├── backend-service-geofence           (8083)
├── backend-service-query              (8084)
└── backend-service-simulator          (8086)

MONITORING
├── prometheus                   (9090)
└── grafana                      (3001)

FRONTEND
└── fleet-frontend               (3000)
```

## 📈 Monitoring

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3001 (admin/admin)
- **Health Check**: http://localhost:8080/actuator/health

## 🔧 API Endpoints

### Vehicle Tracking Service

```
POST /api/tracking/gps              # Ingest single GPS event
POST /api/tracking/gps/batch        # Ingest batch GPS events
GET  /api/tracking/vehicles         # Get all vehicle statuses
GET  /api/tracking/vehicles/{id}    # Get vehicle by ID
GET  /api/tracking/connections      # Get active SSE connections
GET  /api/stream/vehicles           # SSE stream (via gateway)
```

### Alert Processing Service

```
GET  /api/alerts                    # Get all alerts
GET  /api/alerts/vehicle/{id}       # Get alerts by vehicle
GET  /api/alerts/recent?hours=24    # Get recent alerts
GET  /api/alerts/connections        # Get active SSE connections
GET  /api/stream/alerts             # SSE stream (via gateway)
```

### Geofence Service

```
GET  /api/geofences                 # Get all geofences
GET  /api/geofences/geojson         # Get geofences as GeoJSON
GET  /api/geofences/{id}            # Get geofence by ID
GET  /api/geofences/containing?lat=&lon=  # Point-in-polygon query
GET  /api/geofences/nearby?lat=&lon=&distance=  # Nearby geofences
POST /api/geofences                 # Create geofence
PUT  /api/geofences/{id}            # Update geofence
DELETE /api/geofences/{id}          # Delete geofence
```

### Query Service

```
GET  /api/query/vehicles                        # Get all vehicle IDs
GET  /api/query/vehicles/{id}/history           # Get vehicle history
GET  /api/query/vehicles/{id}/alerts            # Get vehicle alerts
GET  /api/query/vehicles/{id}/stats             # Get vehicle stats
GET  /api/query/alerts/recent?hours=24          # Get recent alerts
GET  /api/query/analytics/summary               # Get analytics summary
```

### Simulator Service

```
POST /api/simulator/start           # Start GPS simulation
POST /api/simulator/stop            # Stop simulation
POST /api/simulator/reset           # Reset simulation
GET  /api/simulator/status          # Get simulator status
GET  /api/simulator/vehicles        # Get simulated vehicle states
```

## 🖥️ Frontend Features

- **Live Map**: Leaflet map with animated vehicle markers
- **Real-time Updates**: SSE-powered position and alert streaming
- **Alerts Panel**: Color-coded alerts (speeding, idle, geofence)
- **Vehicle List**: Sortable list with status badges (online/idle/offline)
- **Geofence Overlay**: Visual polygon display on map
- **Stats Bar**: Fleet statistics (online count, alerts, avg speed)

## 📝 License

MIT License
