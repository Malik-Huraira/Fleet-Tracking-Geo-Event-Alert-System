import { useEffect, useState, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { Vehicle } from '@/types/fleet';
import { formatDistanceToNow } from 'date-fns';
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

interface GeoFenceDTO {
  id: number;
  name: string;
  polygonGeojson: string;
  coordinates: number[][][]; // [[[lng, lat], ...]]
}

interface FleetMapProps {
  vehicles: Vehicle[];
  highlightedVehicleId: string | null;
  onVehicleSelect: (vehicleId: string | null) => void;
}

// Fix Leaflet default marker icons
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

// Custom vehicle icon - Teal (online), Gold (idle), Red (offline)
const createVehicleIcon = (status: 'online' | 'idle' | 'offline', heading: number, isHighlighted: boolean) => {
  const colors = {
    online: '#14b8a6',   // Bright teal - pops on satellite
    idle: '#fbbf24',     // Amber/yellow
    offline: '#ef4444',  // Bright red
  };
  
  const color = colors[status];
  const size = isHighlighted ? 52 : 42;
  const glowSize = isHighlighted ? 12 : 8;
  
  return L.divIcon({
    className: 'vehicle-marker',
    html: `
      <div style="position: relative; filter: drop-shadow(0 0 ${glowSize}px ${color});">
        <svg width="${size}" height="${size}" viewBox="0 0 36 36" xmlns="http://www.w3.org/2000/svg" style="transform: rotate(${heading}deg);">
          <!-- Strong white outline for contrast -->
          <path d="M18 8 L26 28 L18 24 L10 28 Z" fill="${color}" stroke="white" stroke-width="3"/>
          <!-- Arrow body -->
          <path d="M18 10 L24 26 L18 23 L12 26 Z" fill="${color}"/>
          <!-- Center highlight -->
          <circle cx="18" cy="18" r="4" fill="white"/>
        </svg>
        ${isHighlighted ? `
          <div style="position: absolute; inset: -10px; border: 4px solid ${color}; border-radius: 50%; animation: pulse-strong 1.5s infinite;"></div>
        ` : ''}
      </div>
      <style>
        @keyframes pulse-strong {
          0% { transform: scale(1); opacity: 1; }
          70% { transform: scale(1.4); opacity: 0; }
          100% { transform: scale(1.4); opacity: 0; }
        }
      </style>
    `,
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
  });
};

function animateMarker(marker: L.Marker, from: L.LatLng, to: L.LatLng, duration: number = 600) {
  const startTime = performance.now();
  const animate = (time: number) => {
    const progress = Math.min((time - startTime) / duration, 1);
    const eased = 1 - Math.pow(1 - progress, 3);
    const lat = from.lat + (to.lat - from.lat) * eased;
    const lng = from.lng + (to.lng - from.lng) * eased;
    marker.setLatLng([lat, lng]);
    if (progress < 1) requestAnimationFrame(animate);
  };
  requestAnimationFrame(animate);
}

export function FleetMap({ vehicles, highlightedVehicleId, onVehicleSelect }: FleetMapProps) {
  const mapRef = useRef<L.Map | null>(null);
  const markersRef = useRef<Map<string, L.Marker>>(new Map());
  const polygonsRef = useRef<Map<number, L.Polygon>>(new Map());
  const containerRef = useRef<HTMLDivElement>(null);
  const [isMapReady, setIsMapReady] = useState(false);
  const [geofences, setGeofences] = useState<GeoFenceDTO[]>([]);
  const [isLoading, setIsLoading] = useState(true);

useEffect(() => {
  setIsLoading(true);
  fetch(`${API_BASE_URL}/geofences/geojson`)
    .then(res => {
      if (!res.ok) {
        console.error('Geofences fetch failed:', res.status, res.statusText);
        throw new Error(`HTTP ${res.status}`);
      }
      return res.json();
    })
    .then((data: GeoFenceDTO[]) => {
      console.log('✅ Geofences successfully loaded:', data);
      setGeofences(data);
    })
    .catch(err => {
      console.error('Failed to load geofences:', err);
    })
    .finally(() => setIsLoading(false));
}, []);

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;

    const center: [number, number] = vehicles.length > 0
      ? [
          vehicles.reduce((a, v) => a + v.lat, 0) / vehicles.length,
          vehicles.reduce((a, v) => a + v.lng, 0) / vehicles.length,
        ]
      : [24.8607, 67.0011];

    const map = L.map(containerRef.current, {
      center,
      zoom: 12,
      zoomControl: false,
      preferCanvas: true,
    });

 // Perfect match for your Karachi map + dark sidebar
L.tileLayer('https://{s}.google.com/vt/lyrs=s,h&x={x}&y={y}&z={z}', {
  maxZoom: 20,
  subdomains: ['mt0', 'mt1', 'mt2', 'mt3'],
  attribution: `
    &copy; <a href="https://www.google.com/maps" target="_blank">Google Maps</a> |
    <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors
  `,
}).addTo(map);
    L.control.zoom({ position: 'topright' }).addTo(map);
    L.control.scale({ position: 'bottomleft', imperial: false }).addTo(map);

    mapRef.current = map;
    setIsMapReady(true);

    return () => {
      map.remove();
      mapRef.current = null;
      markersRef.current.clear();
      polygonsRef.current.clear();
    };
  }, []);

  useEffect(() => {
    if (!mapRef.current || !isMapReady) return;

    const vehicleIds = new Set(vehicles.map(v => v.vehicleId));

    markersRef.current.forEach((marker, id) => {
      if (!vehicleIds.has(id)) {
        marker.remove();
        markersRef.current.delete(id);
      }
    });

    vehicles.forEach(vehicle => {
      const isHighlighted = vehicle.vehicleId === highlightedVehicleId;
      const icon = createVehicleIcon(vehicle.status || 'online', vehicle.heading, isHighlighted);
      const latLng = L.latLng(vehicle.lat, vehicle.lng);

      if (markersRef.current.has(vehicle.vehicleId)) {
        const marker = markersRef.current.get(vehicle.vehicleId)!;
        animateMarker(marker, marker.getLatLng(), latLng);
        marker.setIcon(icon);
        marker.setPopupContent(createPopupContent(vehicle));
      } else {
        const marker = L.marker(latLng, { icon })
          .addTo(mapRef.current!)
          .on('click', () => onVehicleSelect(vehicle.vehicleId));

        marker.bindPopup(createPopupContent(vehicle), {
          className: 'vehicle-popup',
          maxWidth: 300,
        });

        markersRef.current.set(vehicle.vehicleId, marker);
      }
    });
  }, [vehicles, highlightedVehicleId, isMapReady, onVehicleSelect]);

  useEffect(() => {
    if (!mapRef.current || !isMapReady || isLoading) return;

    const ids = new Set(geofences.map(g => g.id));
    polygonsRef.current.forEach((poly, id) => {
      if (!ids.has(id)) {
        poly.remove();
        polygonsRef.current.delete(id);
      }
    });

    geofences.forEach(fence => {
      if (fence.coordinates?.[0]?.length >= 3) {
        const latLngs = fence.coordinates[0].map(([lng, lat]) => [lat, lng] as [number, number]);

        if (polygonsRef.current.has(fence.id)) {
          polygonsRef.current.get(fence.id)!.setLatLngs(latLngs);
        } else {
          const polygon = L.polygon(latLngs, {
            color: '#9333ea',        // Purple-600
            weight: 3,
            opacity: 0.9,
            fillColor: '#c084fc',
            fillOpacity: 0.15,
            className: 'geofence-polygon',
          })
            .addTo(mapRef.current!)
            .bindTooltip(fence.name, {
              permanent: true,
              direction: 'center',
              className: 'geofence-label',
              offset: [0, 0],
            });

          polygon.on('mouseover', () => polygon.setStyle({ fillOpacity: 0.3, weight: 4 })).bringToFront();
          polygon.on('mouseout', () => polygon.setStyle({ fillOpacity: 0.15, weight: 3 }));

          polygonsRef.current.set(fence.id, polygon);
        }
      }
    });
  }, [geofences, isMapReady, isLoading]);

  useEffect(() => {
    if (!mapRef.current || !highlightedVehicleId) return;
    const vehicle = vehicles.find(v => v.vehicleId === highlightedVehicleId);
    if (vehicle) {
      mapRef.current.flyTo([vehicle.lat, vehicle.lng], 16, { duration: 1 });
      const marker = markersRef.current.get(highlightedVehicleId);
      if (marker) setTimeout(() => marker.openPopup(), 1000);
    }
  }, [highlightedVehicleId, vehicles]);

  return (
    <div className="relative w-full h-full rounded-2xl overflow-hidden border border-border/50 bg-card shadow-lg">
      <div ref={containerRef} className="w-full h-full" />

      {isLoading && (
        <div className="absolute inset-0 bg-background/80 backdrop-blur-sm z-[1000] flex items-center justify-center">
          <div className="text-center">
            <div className="w-16 h-16 border-4 border-primary/30 border-t-primary rounded-full animate-spin mx-auto mb-4"></div>
            <p className="text-foreground font-medium">Loading geofences...</p>
          </div>
        </div>
      )}

      {/* Legend - Updated for light theme */}
      <div className="absolute bottom-6 left-6 bg-card/95 backdrop-blur-xl border border-border/50 p-5 rounded-2xl z-[1000] shadow-2xl max-w-[260px]">
        <div className="space-y-5">
          <div>
            <p className="text-sm font-semibold text-foreground mb-3">Vehicle Status</p>
            <div className="space-y-3">
              {[
                { status: 'online', color: '#0d9488', label: 'Online' },
                { status: 'idle', color: '#ca8a04', label: 'Idle' },
                { status: 'offline', color: '#dc2626', label: 'Offline' },
              ].map(({ color, label }) => (
                <div key={label} className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-4 h-4 rounded-full shadow-md" style={{ backgroundColor: color }} />
                    <span className="text-sm text-muted-foreground">{label}</span>
                  </div>
                  <span className="text-sm font-semibold text-foreground">
                    {vehicles.filter(v => v.status === label.toLowerCase()).length}
                  </span>
                </div>
              ))}
            </div>
          </div>

          <div className="pt-4 border-t border-border/40">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-3">
                <div className="w-5 h-5 rounded border-2 border-purple-600 bg-purple-100" />
                <span className="text-sm text-muted-foreground">Geofences</span>
              </div>
              <span className="text-sm font-semibold text-foreground">{geofences.length}</span>
            </div>
          </div>

          <div className="pt-4 border-t border-border/40 grid grid-cols-2 gap-4">
            <div className="text-center">
              <div className="text-2xl font-bold text-foreground">{vehicles.length}</div>
              <div className="text-xs text-muted-foreground">Total</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-primary">
                {vehicles.filter(v => v.status === 'online').length}
              </div>
              <div className="text-xs text-muted-foreground">Active</div>
            </div>
          </div>
        </div>
      </div>

      <div className="absolute top-6 right-6 bg-card/95 backdrop-blur-xl border border-border/50 px-4 py-3 rounded-xl z-[1000] shadow-lg">
        <p className="text-sm text-muted-foreground">
          Use <kbd className="px-2 py-1 bg-muted rounded text-xs font-mono">Scroll</kbd> to zoom
        </p>
      </div>
    </div>
  );
}

function createPopupContent(vehicle: Vehicle): string {
  const colors = {
    online: '#0d9488',
    idle: '#ca8a04',
    offline: '#dc2626',
  };
  const status = vehicle.status || 'online';
  const color = colors[status];
  const lastSeen = formatDistanceToNow(new Date(vehicle.timestamp), { addSuffix: true });

  return `
    <div style="font-family: 'Inter', system-ui, sans-serif; padding: 16px; min-width: 260px; background: white; border-radius: 12px; box-shadow: 0 10px 25px rgba(0,0,0,0.1);">
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; padding-bottom: 12px; border-bottom: 1px solid #e5e7eb;">
        <strong style="font-size: 16px; color: #111827;">${vehicle.vehicleId}</strong>
        <span style="background: ${color}; color: white; padding: 6px 12px; border-radius: 9999px; font-size: 12px; font-weight: 600; text-transform: capitalize;">
          ${status}
        </span>
      </div>

      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin: 16px 0;">
        <div>
          <div style="font-size: 12px; color: #6b7280; text-transform: uppercase; letter-spacing: 0.05em;">Speed</div>
          <div style="font-size: 20px; font-weight: 700; color: #111827; margin-top: 4px;">
            ${Math.round(vehicle.speedKph)} <span style="font-size: 14px; font-weight: 500; color: #6b7280;">kph</span>
          </div>
        </div>
        <div>
          <div style="font-size: 12px; color: #6b7280; text-transform: uppercase; letter-spacing: 0.05em;">Heading</div>
          <div style="font-size: 20px; font-weight: 700; color: #111827; margin-top: 4px;">
            ${Math.round(vehicle.heading)}°
          </div>
        </div>
      </div>

      <div style="background: #f9fafb; padding: 12px; border-radius: 8px; margin-bottom: 12px;">
        <div style="font-size: 13px; color: #4b5563; margin-bottom: 4px;">Last updated</div>
        <div style="font-size: 15px; font-weight: 600; color: #111827;">${lastSeen}</div>
      </div>

      ${vehicle.region ? `
        <div style="display: flex; align-items: center; gap: 8px; padding-top: 8px; border-top: 1px solid #e5e7eb;">
          <span style="font-size: 16px;">📍</span>
          <span style="font-size: 13px; color: #4b5563;">Region:</span>
          <span style="font-size: 14px; font-weight: 600; color: #111827; margin-left: auto;">${vehicle.region}</span>
        </div>
      ` : ''}
    </div>
  `;
}