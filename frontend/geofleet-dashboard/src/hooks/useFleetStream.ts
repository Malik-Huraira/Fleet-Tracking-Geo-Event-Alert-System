import { useState, useEffect, useCallback, useRef } from 'react';
import { Vehicle, Alert, FleetStats } from '@/types/fleet';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
const SSE_URL = import.meta.env.VITE_SSE_URL || 'http://localhost:8080/api/stream';
const USE_MOCK_DATA = import.meta.env.VITE_USE_MOCK_DATA === 'true' || !SSE_URL;

console.log('Fleet Stream ENV:', { API_BASE_URL, SSE_URL, USE_MOCK_DATA });

// Mock data
const mockVehicles: Vehicle[] = [
  { vehicleId: 'TRK-01', lat: 24.8607, lng: 67.0011, speedKph: 65, heading: 45, timestamp: new Date().toISOString(), status: 'online', region: 'Karachi' },
  { vehicleId: 'TRK-02', lat: 24.9056, lng: 67.0822, speedKph: 0, heading: 180, timestamp: new Date(Date.now() - 300000).toISOString(), status: 'idle', region: 'Karachi' },
  { vehicleId: 'TRK-03', lat: 24.8899, lng: 67.0282, speedKph: 78, heading: 90, timestamp: new Date().toISOString(), status: 'online', region: 'Warehouse A' },
  { vehicleId: 'TRK-04', lat: 31.5497, lng: 74.3436, speedKph: 55, heading: 270, timestamp: new Date().toISOString(), status: 'online', region: 'Lahore' },
  { vehicleId: 'TRK-05', lat: 31.5204, lng: 74.3587, speedKph: 42, heading: 135, timestamp: new Date().toISOString(), status: 'online', region: 'Lahore' },
  { vehicleId: 'TRK-06', lat: 33.6844, lng: 73.0479, speedKph: 88, heading: 0, timestamp: new Date().toISOString(), status: 'online', region: 'Islamabad' },
  { vehicleId: 'TRK-07', lat: 33.7294, lng: 73.0931, speedKph: 0, heading: 45, timestamp: new Date(Date.now() - 900000).toISOString(), status: 'offline', region: 'Islamabad' },
  { vehicleId: 'TRK-08', lat: 24.8700, lng: 66.9900, speedKph: 120, heading: 225, timestamp: new Date().toISOString(), status: 'online', region: 'Karachi' },
];

const mockAlerts: Alert[] = [
  { id: '1', vehicleId: 'TRK-08', alertType: 'SPEEDING', details: { speedKph: 120, threshold: 80 }, timestamp: new Date().toISOString(), lat: 24.8700, lng: 66.9900 },
  { id: '2', vehicleId: 'TRK-02', alertType: 'IDLE', details: { idleMinutes: 15 }, timestamp: new Date(Date.now() - 60000).toISOString(), lat: 24.9056, lng: 67.0822 },
  { id: '3', vehicleId: 'TRK-03', alertType: 'GEOFENCE', details: { geofenceName: 'Warehouse A', action: 'entered' }, timestamp: new Date(Date.now() - 120000).toISOString(), lat: 24.8899, lng: 67.0282 },
];

// === MAPPING HELPERS ===
function mapVehicleEvent(event: any): Vehicle {
  const timestamp = typeof event.timestamp === 'string' ? event.timestamp : new Date().toISOString();

  return {
    vehicleId: event.vehicleId || 'unknown',
    lat: Number(event.lat) || 0,
    lng: Number(event.lng) || 0,
    speedKph: Number(event.speedKph) || 0,
    heading: Number(event.heading) || 0,
    timestamp,
    status: event.status?.toLowerCase() as 'online' | 'idle' | 'offline' | undefined,
    region: event.region || 'Unknown',
  };
}

function mapAlertEvent(event: any): Alert {
  const timestamp = typeof event.timestamp === 'string' ? event.timestamp : new Date().toISOString();

  let details: Record<string, any> = {};
  if (event.details) {
    if (typeof event.details === 'string') {
      try {
        details = JSON.parse(event.details);
      } catch (e) {
        console.warn('Failed to parse alert details string:', event.details, e);
      }
    } else if (typeof event.details === 'object' && event.details !== null) {
      details = event.details;
    }
  }

  // Normalize alertType
  let alertType: Alert['alertType'] = 'SPEEDING';
  const rawType = event.alertType?.toUpperCase();
  if (rawType?.includes('GEOFENCE')) {
    alertType = 'GEOFENCE';
  } else if (rawType === 'IDLE') {
    alertType = 'IDLE';
  } else if (rawType === 'SPEEDING') {
    alertType = 'SPEEDING';
  } else {
    console.warn('Unknown alertType, defaulting to SPEEDING:', rawType);
  }

  return {
    id: event.id || `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
    vehicleId: event.vehicleId || 'unknown',
    alertType,
    details,
    timestamp,
    lat: Number(event.lat) || 0,
    lng: Number(event.lng) || 0,
  };
}

function mapFleetStats(data: any): FleetStats {
  return {
    totalOnline: Number(data.totalOnline || data.onlineVehicles || 0),
    alertsLastHour: Number(data.alertsLastHour || 0),
    averageSpeed: Number(data.averageSpeed || data.avgSpeed || 0),
  };
}

// === Dedicated Geofences Hook ===
export function useGeofences() {
  const [geofences, setGeofences] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (USE_MOCK_DATA) {
      const mockGeoJSON = {
        type: "FeatureCollection",
        features: [
          {
            type: "Feature",
            properties: { name: "Warehouse A" },
            geometry: {
              type: "Polygon",
              coordinates: [[
                [67.0232, 24.8849],
                [67.0332, 24.8849],
                [67.0332, 24.8949],
                [67.0232, 24.8949],
                [67.0232, 24.8849],
              ]]
            }
          },
          {
            type: "Feature",
            properties: { name: "Delivery Zone" },
            geometry: {
              type: "Polygon",
              coordinates: [[
                [67.0300, 24.9100],
                [67.0500, 24.9100],
                [67.0500, 24.9200],
                [67.0300, 24.9200],
                [67.0300, 24.9100],
              ]]
            }
          }
        ]
      };
      setGeofences(mockGeoJSON);
      setLoading(false);
      return;
    }

    fetch(`${API_BASE_URL}/geofences/geojson`)
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
      })
      .then(data => {
        console.log('Geofences GeoJSON loaded:', data);
        setGeofences(data);
        setLoading(false);
      })
      .catch(err => {
        console.error('Error loading geofences:', err);
        setError(err.message || 'Failed to load geofences');
        setLoading(false);
      });
  }, []);

  return { geofences, loading, error };
}

// === VEHICLE STREAM HOOK ===
export function useVehicleStream() {
  const [vehicles, setVehicles] = useState<Map<string, Vehicle>>(new Map());
  const [isConnected, setIsConnected] = useState(false);
  const eventSourceRef = useRef<EventSource | null>(null);

  const calculateStatus = useCallback((vehicle: Vehicle): 'online' | 'idle' | 'offline' => {
    const minutesSinceLastSeen = (Date.now() - new Date(vehicle.timestamp).getTime()) / 60000;
    if (minutesSinceLastSeen > 30) return 'offline';
    if (minutesSinceLastSeen > 10 || vehicle.speedKph < 5) return 'idle';
    return 'online';
  }, []);

  // Initial load
  useEffect(() => {
    if (USE_MOCK_DATA) {
      const map = new Map<string, Vehicle>();
      mockVehicles.forEach(v => map.set(v.vehicleId, v));
      setVehicles(map);
      setIsConnected(true);
      return;
    }

    fetch(`${API_BASE_URL}/vehicles/status/all`)
      .then(res => res.ok ? res.json() : [])
      .then(data => {
        const map = new Map<string, Vehicle>();
        (Array.isArray(data) ? data : []).forEach((v: any) => {
          map.set(v.vehicleId, mapVehicleEvent(v));
        });
        setVehicles(map);
      })
      .catch(err => console.error('Failed to load initial vehicles:', err));
  }, []);

  // SSE connection
  useEffect(() => {
    if (USE_MOCK_DATA) {
      const interval = setInterval(() => {
        setVehicles(prev => {
          const updated = new Map(prev);
          updated.forEach((v, id) => {
            const delta = (Math.random() - 0.5) * 0.002;
            updated.set(id, {
              ...v,
              lat: v.lat + delta,
              lng: v.lng + delta,
              speedKph: Math.max(0, v.speedKph + (Math.random() - 0.5) * 15),
              heading: (v.heading + (Math.random() - 0.5) * 20 + 360) % 360,
              timestamp: new Date().toISOString(),
            });
          });
          return updated;
        });
      }, 2000);
      return () => clearInterval(interval);
    }

    const es = new EventSource(`${SSE_URL}/vehicles`);
    eventSourceRef.current = es;

    es.onopen = () => {
      console.log('✅ Vehicle SSE connected');
      setIsConnected(true);
    };

    es.addEventListener('vehicle-update', (e: MessageEvent) => {
      try {
        const data = JSON.parse(e.data);
        const vehicle = mapVehicleEvent(data);
        setVehicles(prev => new Map(prev).set(vehicle.vehicleId, vehicle));
      } catch (err) {
        console.error('Failed to parse vehicle update:', err);
      }
    });

    es.addEventListener('keepalive', () => console.trace('❤️ Vehicle SSE heartbeat'));

    es.onerror = () => {
      console.error('❌ Vehicle SSE error');
      setIsConnected(false);
    };

    return () => {
      es.close();
      eventSourceRef.current = null;
    };
  }, []);

  const vehicleArray = Array.from(vehicles.values()).map(v => ({
    ...v,
    status: calculateStatus(v),
  }));

  const getVehicleById = useCallback((id: string) => vehicleArray.find(v => v.vehicleId === id), [vehicleArray]);

  return {
    vehicles: vehicleArray,
    isConnected,
    totalVehicles: vehicleArray.length,
    onlineVehicles: vehicleArray.filter(v => calculateStatus(v) === 'online').length,
    getVehicleById,
  };
}

// === ALERT STREAM HOOK ===
export function useAlertStream() {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [isConnected, setIsConnected] = useState(false);
  const eventSourceRef = useRef<EventSource | null>(null);

  useEffect(() => {
    if (USE_MOCK_DATA) {
      console.log('🚦 Using mock alerts');
      setAlerts(mockAlerts);
      setIsConnected(true);

      const interval = setInterval(() => {
        if (Math.random() > 0.7) {
          const v = mockVehicles[Math.floor(Math.random() * mockVehicles.length)];
          const rawTypes = ['SPEEDING', 'GEOFENCE_ENTER', 'GEOFENCE_EXIT', 'IDLE'];
          const rawType = rawTypes[Math.floor(Math.random() * rawTypes.length)];

          const alertType: Alert['alertType'] = rawType.includes('GEOFENCE') ? 'GEOFENCE' : rawType as Alert['alertType'];

          const newAlert: Alert = {
            id: Date.now().toString(),
            vehicleId: v.vehicleId,
            alertType,
            details: alertType === 'SPEEDING'
              ? { speedKph: 85 + Math.random() * 35, threshold: 80 }
              : alertType === 'IDLE'
              ? { idleMinutes: 10 + Math.random() * 20 }
              : { geofenceName: 'Warehouse A', action: Math.random() > 0.5 ? 'entered' : 'exited' },
            timestamp: new Date().toISOString(),
            lat: v.lat,
            lng: v.lng,
          };

          console.log('📩 Mock alert generated:', newAlert);
          setAlerts(prev => [newAlert, ...prev].slice(0, 100));
        }
      }, 8000);

      return () => clearInterval(interval);
    }

    console.log('🌐 Connecting to real SSE alerts...');
    const es = new EventSource(`${SSE_URL}/alerts`);
    eventSourceRef.current = es;

    es.onopen = () => {
      console.log('✅ Alert SSE connected');
      setIsConnected(true);
    };

    es.addEventListener('alert', (e: MessageEvent) => {
      console.log('📩 Received alert event raw:', e.data);
      try {
        const data = JSON.parse(e.data);
        const alert = mapAlertEvent(data);
        console.log('✅ Mapped alert:', alert);
        setAlerts(prev => [alert, ...prev].slice(0, 100));
      } catch (err) {
        console.error('❌ Failed to parse alert event:', err, 'raw data:', e.data);
      }
    });

    es.addEventListener('message', (e: MessageEvent) => {
      console.log('📩 Received default message (fallback) raw:', e.data);
      try {
        const data = JSON.parse(e.data);
        const alert = mapAlertEvent(data);
        console.log('✅ Mapped fallback alert:', alert);
        setAlerts(prev => [alert, ...prev].slice(0, 100));
      } catch (err) {
        console.error('❌ Failed to parse fallback message:', err, 'raw data:', e.data);
      }
    });

    es.addEventListener('keepalive', () => console.trace('❤️ Alert SSE heartbeat'));

    es.onerror = (err) => {
      console.error('❌ Alert SSE error – reconnecting...', err);
      setIsConnected(false);
    };

    return () => {
      es.close();
      eventSourceRef.current = null;
    };
  }, []);

  const getAlertsByVehicleId = useCallback(
    (id: string) => alerts.filter(a => a.vehicleId === id),
    [alerts]
  );

  return {
    alerts,
    isConnected,
    totalAlerts: alerts.length,
    getAlertsByVehicleId,
  };
}

// === STATS HOOK ===
export function useFleetStats() {
  const [stats, setStats] = useState<FleetStats | null>(null);

  useEffect(() => {
    if (USE_MOCK_DATA) {
      setStats({ totalOnline: 6, alertsLastHour: 4, averageSpeed: 68 });
      return;
    }

    const fetchStats = () => {
      fetch(`${API_BASE_URL}/vehicles/stats`)
        .then(res => res.ok ? res.json() : {})
        .then(data => setStats(mapFleetStats(data)))
        .catch(err => console.error('Failed to fetch stats:', err));
    };

    fetchStats();
    const interval = setInterval(fetchStats, 30000);
    return () => clearInterval(interval);
  }, []);

  return stats;
}

// Utility hooks
export const useVehicleById = (vehicles: Vehicle[], id: string) =>
  vehicles.find(v => v.vehicleId === id);

export const useAlertsByVehicleId = (alerts: Alert[], id: string) =>
  alerts.filter(a => a.vehicleId === id);