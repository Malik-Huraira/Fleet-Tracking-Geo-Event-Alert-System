# GeoFleet Microservices Architecture

## 🏗️ Hybrid Architecture (Industry Standard)

```
GPS Devices
    ↓
vehicle-gps (Kafka Topic)
    ↓
┌──────────────────────────────────────────────────────────────┐
│              🅰️ KAFKA STREAMS (FAST PATH)                
|                                                              │                                                             
│  ⚡ Speeding detection (windowed, 5s)                        │
│  ⚡ Idle detection (windowed, 3min)                          │
│  ⚡ In-memory, millisecond latency                           │
│  ⚡ No DB calls                                              │
└──────────────────────────┬───────────────────────────────────┘
                           ↓
                  vehicle-alerts (Kafka Topic)
                           ↓
┌──────────────────────────────────────────────────────────────┐
│              🅱️ MICROSERVICE (HEAVY PATH)                    │
│                                                              │
│  🗺️ PostGIS geofence checks (ST_Contains)                   │
│  💾 Alert persistence to DB                                  │
│  🔄 Alert deduplication                                      │
│  📡 SSE notification to frontend                             │
└──────────────────────────┬───────────────────────────────────┘
                           ↓
                  PostgreSQL + PostGIS
                           ↓
                  SSE / WebFlux → Frontend
```

## Why BOTH Kafka Streams + Microservices?

| Need | Best Tool |
|------|-----------|
| Ultra-low latency (ms) | Kafka Streams |
| High throughput (millions/sec) | Kafka Streams |
| Complex geo logic (PostGIS) | Microservice |
| DB writes & integrations | Microservice |
| Easy scaling | Microservices |

👉 **Kafka Streams = fast brain**
👉 **Microservices = heavy muscles**

---

## 🅰️ Kafka Streams - FAST PATH

**What it does:**
- ✔ Windowed logic
- ✔ Stateless/lightweight state
- ✔ No DB calls
- ✔ Millisecond alerts

### Speeding Detection
```java
stream
  .filter((k, v) -> v.getSpeed() > 90)
  .groupByKey()
  .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(5)))
  .count()
  .filter((k, count) -> count >= 2)  // 2+ events = confirmed
  .to("vehicle-alerts");
```

### Idle Detection
```java
stream
  .groupByKey()
  .windowedBy(TimeWindows.ofSizeAndGrace(Duration.ofMinutes(3)))
  .aggregate(IdleAggregator::new, ...)
  .filter((k, agg) -> agg.getAvgSpeed() < 5.0)
  .to("vehicle-alerts");
```

---

## 🅱️ Microservice - HEAVY PATH

**What it does:**
- ✔ PostGIS queries (ST_Contains)
- ✔ Point-in-Polygon
- ✔ DB persistence
- ✔ Alert deduplication
- ✔ SSE notification

### Geofence Detection (PostGIS)
```sql
SELECT id, name FROM geofences
WHERE ST_Contains(
  polygon_geom,
  ST_SetSRID(ST_Point(:lon, :lat), 4326)
);
```

### Alert Consumer Flow
1. Consume `vehicle-alerts` (from Kafka Streams)
2. Validate & deduplicate
3. Enrich with geofence info
4. Save to DB
5. Push SSE notification

---

## 🔁 How They Work Together

### Two Processing Lanes

| Lane | Purpose | Latency |
|------|---------|---------|
| Fast (Kafka Streams) | Detect immediately | ~ms |
| Slow (Microservice) | Verify + persist | ~100ms |

### Example: Speeding Alert
```
1️⃣ GPS event arrives
2️⃣ Kafka Streams detects speeding in milliseconds
3️⃣ Alert sent to vehicle-alerts topic
4️⃣ Alert Service:
   - Checks if already alerted (dedup)
   - Saves to DB
   - Pushes SSE notification
```

### Example: Geofence Alert
```
1️⃣ GPS event arrives
2️⃣ Geo Consumer:
   - PostGIS polygon check
   - Accurate boundary detection
3️⃣ Alert sent to vehicle-alerts topic
4️⃣ Alert Service persists + notifies
```

---

## Alert Types

| Alert | Detection Method | Path |
|-------|-----------------|------|
| **SPEEDING** | Kafka Streams (5s window) | ⚡ Fast |
| **IDLE** | Kafka Streams (3min window) | ⚡ Fast |
| **GEOFENCE_ENTER** | PostGIS ST_Contains | 🗺️ Heavy |
| **GEOFENCE_EXIT** | PostGIS + State tracking | 🗺️ Heavy |

---

## Docker Containers

```
BACKEND
├── DB
│   ├── backend-db-vehicle-tracking  (5433)
│   ├── backend-db-alert             (5434)
│   ├── backend-db-geofence          (5435)
│   └── backend-db-query             (5436)
│
├── SERVICES
│   ├── backend-kafka
│   ├── backend-zookeeper
│   ├── backend-service-api-gateway        (8080)
│   ├── backend-service-vehicle-tracking   (8081)
│   ├── backend-service-alert-processing   (8082)  ← Kafka Streams + Consumer
│   ├── backend-service-geofence           (8083)
│   ├── backend-service-query              (8084)
│   └── backend-service-simulator          (8086)

FRONTEND
└── frontend (5173)
```

---

## Quick Start

```bash
# Build and start
docker-compose -f docker-compose.microservices.yml up --build

# Start simulator
curl -X POST http://localhost:8080/api/simulator/start

# Watch SSE streams
curl http://localhost:8080/stream/vehicles
curl http://localhost:8080/stream/alerts
```

---

## SSE Endpoints (WebFlux)

| Endpoint | Description |
|----------|-------------|
| `/stream/vehicles` | Real-time vehicle positions |
| `/stream/alerts` | Real-time alert notifications |

**Features:**
- ✅ WebFlux `Flux<ServerSentEvent>`
- ✅ Keep-alive heartbeat (15s)
- ✅ Reconnection support (3s retry)
- ✅ Backpressure handling
