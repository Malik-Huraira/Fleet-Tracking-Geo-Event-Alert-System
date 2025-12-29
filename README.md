# 🚚 GeoFleet - Real-Time Fleet Tracking & Geofencing System

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.java.net/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3.1-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.8.3-blue.svg)](https://www.typescriptlang.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)](https://docs.docker.com/compose/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

GeoFleet is a production-grade, real-time fleet tracking and geofencing platform designed for enterprise-scale vehicle monitoring. Built with event-driven architecture using Spring Boot WebFlux, Kafka Streams, and React, it provides instant alerts, geofence monitoring, and comprehensive fleet analytics.

## 🎯 Key Features

### 🚗 Real-Time Vehicle Tracking
- **Live GPS Monitoring**: Continuous position, speed, heading, and status tracking
- **Automatic Status Classification**: ONLINE, IDLE, OFFLINE with intelligent state management
- **Interactive Map Interface**: Real-time vehicle markers with movement trails using Leaflet
- **Fleet Statistics**: Live dashboard with comprehensive fleet metrics

### 🗺️ Advanced Geofencing (PostGIS)
- **Polygon-Based Zones**: Complex geofence shapes stored using PostGIS geometry
- **Boundary-Inclusive Detection**: Accurate entry/exit detection using `ST_Covers` spatial queries
- **Real-Time Alerts**: Instant ENTER/EXIT notifications with zone identification
- **Stateful Processing**: Kafka Streams-based state management for reliable event generation

### 🚨 Intelligent Alert System
- **Speeding Detection**: Configurable speed thresholds (default: 80 km/h) with instant alerts
- **Idle Monitoring**: Precise zero-speed detection with configurable duration (10+ minutes)
- **Geofence Events**: Entry and exit alerts with zone name and coordinates
- **Exactly-Once Semantics**: Kafka Streams configuration ensures no duplicate alerts

### 🔄 Event-Driven Architecture
- **Server-Sent Events (SSE)**: Ultra-low latency real-time updates to dashboard
- **Kafka Streams Processing**: Scalable rule evaluation and alert generation
- **Dead Letter Queue**: Comprehensive error handling with diagnostic preservation
- **Reactive Backend**: Spring WebFlux for high-throughput, non-blocking operations

## 🏗️ System Architecture

```
┌─────────────────────┐    Kafka Topics     ┌──────────────────────┐
│  Vehicle Simulator  │ ──────────────────▶ │   vehicle-gps        │
│  (Route-based GPS)  │                     │   vehicle-alerts     │
└─────────────────────┘                     │   vehicle-gps-dlq    │
                                             └──────────┬───────────┘
                                                        │
                                             Kafka Streams (Rules Engine)
                                                        │
                                            ┌───────────▼────────────┐
                                            │  Spring Boot Backend   │
                                            │  (WebFlux + PostGIS)   │
                                            └───────────┬────────────┘
                                                        │
                                            Server-Sent Events (SSE)
                                                        │
                                            ┌───────────▼────────────┐
                                            │   React Dashboard      │
                                            │ (TypeScript + Leaflet) │
                                            └────────────────────────┘
```

## 🛠️ Technology Stack

### Backend
- **Java 17** with Spring Boot 3.1.5
- **Spring WebFlux** for reactive programming
- **Apache Kafka + Kafka Streams** for event processing
- **PostgreSQL + PostGIS** for spatial data storage
- **Hibernate Spatial** for JPA spatial queries
- **Flyway** for database migrations
- **Micrometer + Prometheus** for metrics
- **MapStruct** for DTO mapping
- **Testcontainers** for integration testing

### Frontend
- **React 18.3.1** with TypeScript 5.8.3
- **Vite 7.3.0** for fast development and building
- **Tailwind CSS 3.4.17** for styling
- **shadcn/ui** component library with Radix UI
- **React Leaflet 4.2.1** for interactive maps
- **TanStack Query 5.83.0** for data fetching
- **React Hook Form** for form management
- **Date-fns** for date manipulation

### Infrastructure
- **Docker & Docker Compose** for containerization
- **Confluent Kafka 7.6.0** for message streaming
- **PostGIS 16-3.4** for spatial database
- **Prometheus** for monitoring and metrics

## 📁 Project Structure

```
GeoFleet/
├── backend/tracking/                    # Spring Boot Backend
│   ├── src/main/java/com/geofleet/tracking/
│   │   ├── config/                      # Configuration classes
│   │   ├── controller/                  # REST & SSE endpoints
│   │   ├── exception/                   # Exception handling
│   │   ├── kafka/
│   │   │   ├── consumer/               # GPS, Alert, DLQ consumers
│   │   │   ├── producer/               # Message producers
│   │   │   └── streams/                # Kafka Streams processors
│   │   ├── model/                      # JPA entities & DTOs
│   │   ├── repository/                 # JPA repositories with PostGIS
│   │   ├── service/                    # Business logic layer
│   │   ├── simulator/                  # GPS data simulator
│   │   ├── sse/                        # Server-Sent Events publishers
│   │   └── util/                       # Utility classes
│   ├── src/main/resources/
│   │   ├── db/migration/               # Flyway database migrations
│   │   ├── application.yml             # Spring configuration
│   │   └── application-docker.yml      # Docker-specific config
│   ├── pom.xml                         # Maven dependencies
│   └── Dockerfile                      # Backend container
│
├── frontend/geofleet-dashboard/         # React Frontend
│   ├── src/
│   │   ├── components/
│   │   │   ├── ui/                     # shadcn/ui components
│   │   │   ├── AlertsPanel.tsx         # Real-time alerts display
│   │   │   ├── FleetMap.tsx            # Interactive Leaflet map
│   │   │   ├── StatsBar.tsx            # Fleet statistics
│   │   │   └── VehicleList.tsx         # Vehicle status list
│   │   ├── hooks/
│   │   │   ├── useFleetStream.ts       # SSE connection management
│   │   │   └── use-toast.ts            # Toast notifications
│   │   ├── pages/
│   │   │   ├── Index.tsx               # Main dashboard
│   │   │   └── NotFound.tsx            # 404 page
│   │   ├── types/
│   │   │   └── fleet.ts                # TypeScript interfaces
│   │   └── lib/
│   │       └── utils.ts                # Utility functions
│   ├── package.json                    # Node.js dependencies
│   ├── vite.config.ts                  # Vite configuration
│   ├── tailwind.config.ts              # Tailwind CSS config
│   └── Dockerfile                      # Frontend container
│
├── docker-compose.yml                  # Multi-service orchestration
├── prometheus.yml                      # Prometheus configuration
└── README.md                          # This file
```

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose
- 8GB+ RAM recommended for all services

### 1. Clone & Start
```bash
git clone <repository-url>
cd GeoFleet
docker compose up --build
```

### 2. Access Services
| Service | URL | Description |
|---------|-----|-------------|
| **Dashboard** | http://localhost:8081 | Main fleet tracking interface |
| **Backend API** | http://localhost:8080/api | REST API endpoints |
| **Vehicle Stream** | http://localhost:8080/api/stream/vehicles | SSE vehicle updates |
| **Alert Stream** | http://localhost:8080/api/stream/alerts | SSE alert notifications |
| **Prometheus** | http://localhost:9090 | Metrics and monitoring |
| **Health Check** | http://localhost:8080/api/actuator/health | Service health status |

### 3. View Live Data
The system includes a realistic GPS simulator that generates vehicle movements along predefined routes. You'll immediately see:
- Live vehicle positions on the map
- Real-time speed and status updates
- Geofence entry/exit alerts
- Fleet statistics and metrics

## ⚙️ Configuration

### Environment Variables
```bash
# Frontend (.env)
VITE_API_BASE_URL=http://localhost:8080/api
VITE_SSE_URL=http://localhost:8080/api/stream
VITE_USE_MOCK_DATA=false

# Backend (application-docker.yml)
SPRING_PROFILES_ACTIVE=docker
JAVA_OPTS=-Duser.timezone=Asia/Karachi
```

### Alert Thresholds
```yaml
# application.yml
fleet:
  alerts:
    speed-threshold: 80        # km/h
    idle-threshold: 600000     # 10 minutes in milliseconds
    geofence-enabled: true
```

## 📡 API Contracts

### Vehicle Stream Event
```json
{
  "vehicleId": "TRK-04",
  "lat": 24.8899,
  "lng": 67.0282,
  "speedKph": 55.4,
  "heading": 120,
  "status": "ONLINE",
  "statusColor": "green",
  "region": "Warehouse A",
  "timestamp": "2025-12-29T10:30:00Z"
}
```

### Alert Stream Event
```json
{
  "vehicleId": "TRK-04",
  "alertType": "GEOFENCE",
  "details": {
    "geofence": "Warehouse A",
    "zone": "Warehouse A",
    "action": "entered",
    "lat": 24.8899,
    "lng": 67.0282
  },
  "timestamp": "2025-12-29T10:30:06Z",
  "lat": 24.8899,
  "lng": 67.0282
}
```

## 🔄 Data Flow

1. **GPS Ingestion**: Simulator produces realistic GPS events → `vehicle-gps` topic
2. **Stream Processing**: Kafka Streams evaluates rules (speed, idle, geofence) in real-time
3. **Alert Generation**: Rule violations produce alerts → `vehicle-alerts` topic
4. **Persistence**: Consumers save data to PostgreSQL with PostGIS spatial indexing
5. **Real-Time Updates**: SSE streams push updates to React dashboard instantly
6. **Error Handling**: Failed messages route to `vehicle-gps-dlq` for analysis

## 🏥 Health & Monitoring

### Health Endpoints
- **Application Health**: `/api/actuator/health`
- **Database Health**: Includes PostGIS connectivity check
- **Kafka Health**: Stream processor status monitoring

### Prometheus Metrics
- **Custom Metrics**: Vehicle counts, alert rates, processing latencies
- **JVM Metrics**: Memory, GC, thread pools
- **Kafka Metrics**: Consumer lag, throughput, error rates
- **Database Metrics**: Connection pool, query performance

### Logging
- **Structured Logging**: JSON format with correlation IDs
- **Alert Processing**: Detailed logs for debugging geofence issues
- **Performance Monitoring**: Stream processing latencies and throughput

## 🧪 Testing

### Backend Testing
```bash
cd backend/tracking
./mvnw test                    # Unit tests
./mvnw verify                  # Integration tests with Testcontainers
```

### Frontend Testing
```bash
cd frontend/geofleet-dashboard
npm test                       # Jest unit tests
npm run lint                   # ESLint code quality
```

## 🔧 Development

### Backend Development
```bash
cd backend/tracking
./mvnw spring-boot:run
```

### Frontend Development
```bash
cd frontend/geofleet-dashboard
npm run dev                    # Vite dev server with HMR
```

### Database Migrations
```bash
# New migration
./mvnw flyway:migrate

# Migration info
./mvnw flyway:info
```

## 🚀 Production Deployment

### Docker Swarm
```bash
docker stack deploy -c docker-compose.yml geofleet
```

### Kubernetes
Helm charts and Kubernetes manifests available in `/k8s` directory.

### Environment-Specific Configs
- **Development**: `application.yml`
- **Docker**: `application-docker.yml`
- **Production**: `application-prod.yml`

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **PostGIS** for powerful spatial database capabilities
- **Apache Kafka** for reliable event streaming
- **Spring Boot** for robust backend framework
- **React & TypeScript** for modern frontend development
- **Leaflet** for interactive mapping capabilities