// src/types/fleet.ts

// Define allowed alert types
export type AlertType = 'SPEEDING' | 'IDLE' | 'GEOFENCE';

// Vehicle interface
export interface Vehicle {
  vehicleId: string;
  lat: number;
  lng: number;
  speedKph: number;
  heading: number;
  timestamp: string;
  status?: 'online' | 'idle' | 'offline';
  region?: string; // e.g., "Warehouse A", "Karachi", etc.
}

// Alert details interface
export interface AlertDetails {
  speedKph?: number;
  threshold?: number;
  geofence?: string;
  zone?: string;
  // Legacy field name for backward compatibility
  geofenceName?: string;
  idleMinutes?: number;
  // Allow any extra fields from backend (e.g., excess speed, location details)
  [key: string]: any;
}

// Alert interface
export interface Alert {
  id: string;
  vehicleId: string;
  alertType: AlertType;
  details: Record<string, any>; // can also use AlertDetails if you want stricter typing
  timestamp: string;
  lat: number;
  lng: number;
}

// Fleet statistics interface
export interface FleetStats {
  totalOnline: number;
  alertsLastHour: number;
  averageSpeed: number;
}
