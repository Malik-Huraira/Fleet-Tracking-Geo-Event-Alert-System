import { Truck, AlertTriangle, Gauge } from 'lucide-react';
import { Vehicle, Alert } from '@/types/fleet';

interface StatsBarProps {
  vehicles: Vehicle[];
  alerts: Alert[];
  isConnected: boolean;
}

export function StatsBar({ vehicles, alerts, isConnected }: StatsBarProps) {
  const onlineCount = vehicles.filter(v => v.status === 'online').length;
  const alertsLastHour = alerts.filter(a => {
    const alertTime = new Date(a.timestamp).getTime();
    const hourAgo = Date.now() - 3600000;
    return alertTime > hourAgo;
  }).length;
  const averageSpeed = vehicles.length > 0 
    ? Math.round(vehicles.reduce((acc, v) => acc + v.speedKph, 0) / vehicles.length)
    : 0;

  return (
    <div className="glass-panel px-6 py-3 flex items-center justify-between">
      <div className="flex items-center gap-8">
        {/* Logo/Title */}
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-lg bg-primary/20 flex items-center justify-center">
            <Truck className="w-5 h-5 text-primary" />
          </div>
          <div>
            <h1 className="text-lg font-semibold text-foreground">FleetTrack</h1>
            <p className="text-xs text-muted-foreground">Real-time Monitoring</p>
          </div>
        </div>

        {/* Connection Status */}
        <div className="flex items-center gap-2">
          <div className={`w-2 h-2 rounded-full ${isConnected ? 'bg-success animate-pulse' : 'bg-destructive'}`} />
          <span className="text-sm text-muted-foreground">
            {isConnected ? 'Live' : 'Disconnected'}
          </span>
        </div>
      </div>

      {/* Stats */}
      <div className="flex items-center gap-6">
        <StatItem
          icon={<Truck className="w-4 h-4" />}
          label="Vehicles Online"
          value={`${onlineCount}/${vehicles.length}`}
          color="success"
        />
        <div className="w-px h-8 bg-border" />
        <StatItem
          icon={<AlertTriangle className="w-4 h-4" />}
          label="Alerts (1h)"
          value={alertsLastHour.toString()}
          color={alertsLastHour > 5 ? 'destructive' : 'warning'}
        />
        <div className="w-px h-8 bg-border" />
        <StatItem
          icon={<Gauge className="w-4 h-4" />}
          label="Avg Speed"
          value={`${averageSpeed} kph`}
          color="primary"
        />
      </div>
    </div>
  );
}

interface StatItemProps {
  icon: React.ReactNode;
  label: string;
  value: string;
  color: 'primary' | 'success' | 'warning' | 'destructive';
}

function StatItem({ icon, label, value, color }: StatItemProps) {
  const colorClasses = {
    primary: 'text-primary bg-primary/10',
    success: 'text-success bg-success/10',
    warning: 'text-warning bg-warning/10',
    destructive: 'text-destructive bg-destructive/10',
  };

  return (
    <div className="flex items-center gap-3">
      <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${colorClasses[color]}`}>
        {icon}
      </div>
      <div>
        <p className="text-xs text-muted-foreground">{label}</p>
        <p className="text-sm font-semibold text-foreground">{value}</p>
      </div>
    </div>
  );
}