import { AlertTriangle, Zap, MapPin, Clock } from 'lucide-react';
import { Alert } from '@/types/fleet';
import { formatDistanceToNow } from 'date-fns';
import { ScrollArea } from '@/components/ui/scroll-area';
import { cn } from '@/lib/utils';

interface AlertsPanelProps {
  alerts: Alert[];
  onAlertClick: (vehicleId: string) => void;
}

export function AlertsPanel({ alerts, onAlertClick }: AlertsPanelProps) {
  return (
    <div className="glass-panel h-full flex flex-col rounded-2xl overflow-hidden">
      {/* Header */}
      <div className="px-5 py-4 border-b border-border/60 bg-card/80 backdrop-blur-sm">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <AlertTriangle className="w-5 h-5 text-warning" />
            <h2 className="text-lg font-semibold text-foreground">Recent Alerts</h2>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-2xl font-bold text-warning">{alerts.length}</span>
            <span className="text-sm text-muted-foreground">active</span>
          </div>
        </div>
      </div>

      {/* Alerts List */}
      <ScrollArea className="flex-1">
        <div className="p-3 space-y-2">
          {alerts.length === 0 ? (
            <EmptyState />
          ) : (
            alerts.map((alert, index) => (
              <AlertItem
                key={alert.id}
                alert={alert}
                onClick={() => onAlertClick(alert.vehicleId)}
                isNew={index === 0}
              />
            ))
          )}
        </div>
      </ScrollArea>
    </div>
  );
}

function EmptyState() {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      <div className="w-20 h-20 rounded-full bg-muted/50 flex items-center justify-center mb-4">
        <AlertTriangle className="w-10 h-10 text-muted-foreground/60" />
      </div>
      <h3 className="text-lg font-medium text-foreground mb-1">All clear</h3>
      <p className="text-sm text-muted-foreground max-w-xs">
        No active alerts at the moment. Your fleet is operating normally.
      </p>
    </div>
  );
}

interface AlertItemProps {
  alert: Alert;
  onClick: () => void;
  isNew?: boolean;
}

function AlertItem({ alert, onClick, isNew = false }: AlertItemProps) {
  const typeConfig = {
    SPEEDING: {
      icon: Zap,
      color: 'text-destructive',
      bg: 'bg-destructive/10',
      border: 'border-destructive/20',
      label: 'Speeding',
    },
    GEOFENCE: {
      icon: MapPin,
      color: 'text-[hsl(262,83%,58%)]',
      bg: 'bg-[hsl(262,83%,58%,0.1)]',
      border: 'border-[hsl(262,83%,58%,0.2)]',
      label: 'Geofence',
    },
    IDLE: {
      icon: Clock,
      color: 'text-warning',
      bg: 'bg-warning/10',
      border: 'border-warning/20',
      label: 'Idle',
    },
  }[alert.alertType];

  const Icon = typeConfig.icon;

  const getPrimaryMessage = () => {
    switch (alert.alertType) {
      case 'SPEEDING': {
        const speedKph = alert.details.speedKph || alert.details.speed || 0;
        const threshold = alert.details.threshold || alert.details.speed_limit || 80;
        const excess = alert.details.excess || Math.round(speedKph - threshold);
        return `Exceeded speed limit by ${excess} kph`;
      }
      case 'GEOFENCE': {
        const geofenceName = alert.details.geofenceName || alert.details.geofence_name || 'Unknown zone';
        const action = alert.details.action || 'entered';
        return `${action === 'entered' ? 'Entered' : 'Exited'} ${geofenceName}`;
      }
      case 'IDLE': {
        const idleMinutes = alert.details.idleMinutes || alert.details.idle_minutes || 0;
        return `Idle for ${idleMinutes} minutes`;
      }
      default:
        return 'Unknown alert';
    }
  };

  return (
    <button
      onClick={onClick}
      className={cn(
        "w-full text-left p-4 rounded-xl transition-all duration-300",
        "hover:scale-[1.01] hover:shadow-md",
        "border border-transparent hover:border-border/50",
        "bg-card/70 backdrop-blur-sm",
        typeConfig.bg,
        typeConfig.border,
        isNew && "animate-pulse-once ring-2 ring-primary/30"
      )}
      aria-label={`Alert for vehicle ${alert.vehicleId}`}
    >
      <div className="flex items-start gap-4">
        <div className={cn("mt-0.5 p-2 rounded-lg", typeConfig.bg)}>
          <Icon className={cn("w-5 h-5", typeConfig.color)} />
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-start justify-between gap-3 mb-1">
            <div>
              <p className="font-semibold text-foreground">{alert.vehicleId}</p>
              <p className="text-sm text-muted-foreground capitalize">{typeConfig.label} Alert</p>
            </div>
            <time className="text-xs text-muted-foreground whitespace-nowrap">
              {formatDistanceToNow(new Date(alert.timestamp), { addSuffix: true })}
            </time>
          </div>

          <p className="text-sm text-foreground/80 mt-2 line-clamp-2">
            {getPrimaryMessage()}
          </p>

          {alert.alertType === 'SPEEDING' && (
            <div className="flex items-center gap-4 mt-3 text-xs">
              <span className="text-muted-foreground">
                Current: <span className="font-medium text-foreground">{alert.details.speedKph || alert.details.speed || 0} kph</span>
              </span>
              <span className="text-muted-foreground">
                Limit: <span className="font-medium text-foreground">{alert.details.threshold || alert.details.speed_limit || 80} kph</span>
              </span>
            </div>
          )}
        </div>
      </div>
    </button>
  );
}