🚚 GeoFleet — Real-Time Fleet Tracking & Geo-Event Alert System

GeoFleet is a production-grade, real-time fleet tracking and geo-event alert platform designed to monitor vehicle movement, enforce operational rules, and deliver instant alerts at scale.
It combines a reactive, event-driven Spring Boot backend (WebFlux + Kafka Streams) with a modern React dashboard powered by Server-Sent Events (SSE) for ultra-low-latency updates.

📌 Executive Summary

Purpose
Provide real-time visibility into fleet operations with immediate detection of:

Speeding violations

Prolonged idling

Geofence entry and exit events

Architecture
Fully event-driven system leveraging Kafka Streams, reactive APIs, and push-based browser updates.

Scalability
Continuous rule evaluation via Kafka Streams and efficient fan-out using SSE.

Reliability
Built-in Dead Letter Queue (DLQ) handling, strict idle detection logic, and comprehensive observability.

🏗️ System Architecture
┌────────────────────┐       Kafka Topics       ┌────────────────────┐
│ Vehicle Simulator  │ ──────────────────────▶ │ vehicle-gps        │
└────────────────────┘                          │ vehicle-alerts     │
                                                 │ vehicle-gps-dlq    │
                                                 └─────────┬──────────┘
                                                           │
                                                  Kafka Streams (Rules Engine)
                                                           │
                                               ┌───────────▼───────────┐
                                               │ Spring Boot Backend   │
                                               │ (WebFlux + SSE)       │
                                               └───────────┬───────────┘
                                                           │
                                               Server-Sent Events (SSE)
                                                           │
                                               ┌───────────▼───────────┐
                                               │ React Dashboard       │
                                               │ (Vite + Leaflet)      │
                                               └───────────────────────┘

⚙️ Core Capabilities
🚗 Vehicle Tracking

Continuous ingestion of GPS telemetry

Real-time position, speed, heading, and last-seen tracking

Automatic status classification:

ONLINE

IDLE

OFFLINE

🗺️ Geo-Fencing (PostGIS)

Polygon-based geofences stored using PostGIS geometry + JSONB

Accurate boundary-inclusive detection using ST_Covers

Stateful entry and exit event generation in real time

🚨 Alerting Engine

Speeding: Immediate detection when speed exceeds threshold (default: 80 km/h)

Idle: Strict zero-speed detection (consecutive zero-speed events for >10 minutes)

Geofence: ENTER and EXIT alerts

Exactly-once semantics via Kafka Streams configuration

🔄 Real-Time Streaming

Dedicated SSE streams for:

Vehicle updates

Alerts

30-second keep-alive heartbeats for resilient reconnections

Replay buffer support for late-connecting clients

🧯 Dead Letter Queue (DLQ)

Failed messages routed to vehicle-gps-dlq

Full diagnostic headers preserved

Dedicated DLQ consumer for monitoring and debugging

📊 Dashboard & Observability

Live map with vehicle markers and movement trails

Real-time alert feed and vehicle list

Fleet-wide statistics panel

Prometheus metrics and health endpoints

🧰 Technology Stack
Backend

Java 17

Spring Boot 3 (WebFlux)

Spring for Apache Kafka + Kafka Streams

PostgreSQL + PostGIS

Flyway migrations

Project Reactor

Micrometer + Prometheus

Frontend

React 18

Vite

Tailwind CSS

Leaflet / React-Leaflet

Server-Sent Events (EventSource)

Lucide Icons

Infrastructure

Docker & Docker Compose

Prometheus monitoring

📁 Repository Structure
GeoFleet/
├── backend/
│   ├── src/main/java/com/geofleet/tracking/
│   │   ├── controller/       # REST + SSE endpoints
│   │   ├── sse/              # Reactive publishers
│   │   ├── service/          # Business logic & status handling
│   │   ├── repository/       # JPA + PostGIS queries
│   │   ├── model/            # Entities, DTOs, enums
│   │   ├── kafka/
│   │   │   ├── consumer/     # GPS, Alert, DLQ consumers
│   │   │   ├── producer/     # Simulator producer
│   │   │   └── streams/      # Speeding, idle, geofence processors
│   │   ├── simulator/        # Realistic route-based simulator
│   │   └── util/             # Geometry utilities
│   └── TrackingApplication.java
│
├── frontend/geofleet-dashboard/
│   ├── src/
│   │   ├── components/       # Map, alerts panel, vehicle list, stats
│   │   ├── services/         # SSE connection management
│   │   ├── utils/            # Status formatting helpers
│   │   ├── App.jsx
│   │   └── main.jsx
│   └── vite.config.js
│
├── docker-compose.yml
├── prometheus.yml
└── README.md

▶️ Running the Platform (Docker)
docker compose up --build

🌐 Service Endpoints
Component	URL
Frontend Dashboard	http://localhost:8081

Backend API	http://localhost:8080/api

Vehicle SSE Stream	http://localhost:8080/api/stream/vehicles

Alert SSE Stream	http://localhost:8080/api/stream/alerts

Prometheus	http://localhost:9090

Health Check	http://localhost:8080/api/actuator/health
⚙️ Frontend Configuration (.env)
VITE_API_BASE_URL=http://localhost:8080/api
VITE_SSE_URL=http://localhost:8080/api/stream
VITE_USE_MOCK_DATA=false

📡 Event Contracts
Vehicle Stream Event
{
  "vehicleId": "TRK-11",
  "lat": 24.8899,
  "lng": 67.0282,
  "speedKph": 55.4,
  "heading": 120,
  "status": "ONLINE",
  "statusColor": "green",
  "region": "Warehouse A",
  "timestamp": "2025-12-29T10:30:00Z"
}

Alert Stream Event
{
  "vehicleId": "TRK-11",
  "alertType": "SPEEDING",
  "details": {
    "speedKph": 120,
    "threshold": 80,
    "excess": 40
  },
  "timestamp": "2025-12-29T10:30:06Z",
  "lat": 24.8899,
  "lng": 67.0282
}

🔁 End-to-End Processing Flow

Simulator produces GPS events → vehicle-gps topic

Kafka Streams evaluates rules in real time

Alerts are published to vehicle-alerts topic

Consumers persist data and publish updates via SSE

React dashboard receives and renders updates instantly

❤️ Health & Metrics

Health: /api/actuator/health

Metrics: /api/actuator/prometheus

📄 License

MIT License
