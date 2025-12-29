import { useState, useCallback } from 'react';
import { Truck, AlertTriangle, Gauge, Clock, Zap, MapPin, Search, ChevronRight } from 'lucide-react';
import { FleetMap } from '@/components/FleetMap';
import { useVehicleStream, useAlertStream } from '@/hooks/useFleetStream';
import { Vehicle, Alert } from '@/types/fleet';
import { formatDistanceToNow } from 'date-fns';
import { Input } from '@/components/ui/input';
import { ScrollArea } from '@/components/ui/scroll-area';

const Index = () => {
  const { vehicles, isConnected: vehiclesConnected } = useVehicleStream();
  const { alerts, isConnected: alertsConnected } = useAlertStream();
  const [highlightedVehicleId, setHighlightedVehicleId] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');

  const handleAlertClick = useCallback((vehicleId: string) => {
    setHighlightedVehicleId(vehicleId);
  }, []);

  const handleVehicleSelect = useCallback((vehicleId: string) => {
    setHighlightedVehicleId(prev => prev === vehicleId ? null : vehicleId);
  }, []);

  const isConnected = vehiclesConnected && alertsConnected;
  const filteredVehicles = vehicles.filter(v => v.vehicleId.toLowerCase().includes(searchQuery.toLowerCase()));
  const onlineCount = vehicles.filter(v => v.status === 'online').length;
  const alertsLastHour = alerts.filter(a => new Date(a.timestamp).getTime() > Date.now() - 3600000).length;
  const averageSpeed = vehicles.length > 0 ? Math.round(vehicles.reduce((acc, v) => acc + v.speedKph, 0) / vehicles.length) : 0;

  return (
    <div className="h-screen w-screen flex flex-col bg-background overflow-hidden p-3 gap-3">
      {/* Stats Bar */}
      <header className="shrink-0 bg-card border border-border rounded-lg px-6 py-3 flex items-center justify-between">
        <div className="flex items-center gap-8">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-primary/20 flex items-center justify-center"><Truck className="w-5 h-5 text-primary" /></div>
            <div><h1 className="text-lg font-semibold text-foreground">FleetTrack</h1><p className="text-xs text-muted-foreground">Real-time Monitoring</p></div>
          </div>
          <div className="flex items-center gap-2"><div className={`w-2 h-2 rounded-full ${isConnected ? 'bg-success animate-pulse' : 'bg-destructive'}`} /><span className="text-sm text-muted-foreground">{isConnected ? 'Live' : 'Disconnected'}</span></div>
        </div>
        <div className="flex items-center gap-6">
          <StatItem icon={<Truck className="w-4 h-4" />} label="Vehicles Online" value={`${onlineCount}/${vehicles.length}`} variant="success" />
          <div className="w-px h-8 bg-border" />
          <StatItem icon={<AlertTriangle className="w-4 h-4" />} label="Alerts (1h)" value={alertsLastHour.toString()} variant={alertsLastHour > 5 ? 'destructive' : 'warning'} />
          <div className="w-px h-8 bg-border" />
          <StatItem icon={<Gauge className="w-4 h-4" />} label="Avg Speed" value={`${averageSpeed} kph`} variant="primary" />
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 flex gap-3 min-h-0">
        {/* Vehicle List */}
        <aside className="w-80 shrink-0 bg-card border border-border rounded-lg flex flex-col">
          <div className="px-4 py-3 border-b border-border">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2"><Truck className="w-4 h-4 text-primary" /><h2 className="font-semibold text-foreground">Vehicles</h2></div>
              <div className="flex items-center gap-2 text-xs">
                <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-success" />{vehicles.filter(v => v.status === 'online').length}</span>
                <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-warning" />{vehicles.filter(v => v.status === 'idle').length}</span>
                <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-destructive" />{vehicles.filter(v => v.status === 'offline').length}</span>
              </div>
            </div>
            <div className="relative"><Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" /><Input placeholder="Search vehicle..." value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} className="pl-8 h-8 text-sm bg-muted/50 border-border" /></div>
          </div>
          <ScrollArea className="flex-1"><div className="p-2 space-y-1">{filteredVehicles.map((v) => <VehicleItem key={v.vehicleId} vehicle={v} isSelected={v.vehicleId === highlightedVehicleId} onClick={() => handleVehicleSelect(v.vehicleId)} />)}</div></ScrollArea>
        </aside>

        {/* Map Area */}
        <section className="flex-1 min-w-0">
          <FleetMap
            vehicles={vehicles}
            highlightedVehicleId={highlightedVehicleId}
            onVehicleSelect={handleVehicleSelect}
          />
        </section>

        {/* Alerts */}
        <aside className="w-80 shrink-0 bg-card border border-border rounded-lg flex flex-col">
          <div className="px-4 py-3 border-b border-border flex items-center justify-between"><div className="flex items-center gap-2"><AlertTriangle className="w-4 h-4 text-warning" /><h2 className="font-semibold text-foreground">Recent Alerts</h2></div><span className="text-xs text-muted-foreground bg-muted px-2 py-1 rounded-full">{alerts.length} alerts</span></div>
          <ScrollArea className="flex-1"><div className="p-2 space-y-2">{alerts.map((a) => <AlertItem key={a.id} alert={a} onClick={() => handleAlertClick(a.vehicleId)} />)}</div></ScrollArea>
        </aside>
      </main>
    </div>
  );
};

function StatItem({ icon, label, value, variant }: { icon: React.ReactNode; label: string; value: string; variant: 'primary' | 'success' | 'warning' | 'destructive' }) {
  const colors = { primary: 'text-primary bg-primary/10', success: 'text-success bg-success/10', warning: 'text-warning bg-warning/10', destructive: 'text-destructive bg-destructive/10' };
  return <div className="flex items-center gap-3"><div className={`w-8 h-8 rounded-lg flex items-center justify-center ${colors[variant]}`}>{icon}</div><div><p className="text-xs text-muted-foreground">{label}</p><p className="text-sm font-semibold text-foreground">{value}</p></div></div>;
}

function VehicleItem({ vehicle, isSelected, onClick }: { vehicle: Vehicle; isSelected: boolean; onClick: () => void }) {
  const emoji = { online: '🟢', idle: '🟠', offline: '🔴' };
  return <button onClick={onClick} className={`w-full text-left p-3 rounded-lg transition-all hover:bg-accent/50 ${isSelected ? 'bg-primary/10 border border-primary/30' : 'border border-transparent'}`}><div className="flex items-center gap-3"><div className="w-8 h-8 rounded-lg flex items-center justify-center bg-muted"><span className="text-sm">{emoji[vehicle.status || 'online']}</span></div><div className="flex-1"><div className="flex items-center justify-between"><span className="font-medium text-sm text-foreground">{vehicle.vehicleId}</span><ChevronRight className="w-4 h-4 text-muted-foreground" /></div><div className="flex items-center gap-4 mt-1"><span className="flex items-center gap-1 text-xs text-muted-foreground"><Gauge className="w-3 h-3" />{Math.round(vehicle.speedKph)} kph</span><span className="flex items-center gap-1 text-xs text-muted-foreground"><Clock className="w-3 h-3" />{formatDistanceToNow(new Date(vehicle.timestamp), { addSuffix: true })}</span></div></div></div></button>;
}

function AlertItem({ alert, onClick }: { alert: Alert; onClick: () => void }) {
  const styles = { SPEEDING: 'border-l-4 border-l-destructive bg-destructive/10', GEOFENCE: 'border-l-4 border-l-primary bg-primary/10', IDLE: 'border-l-4 border-l-warning bg-warning/10' };
  const icons = { SPEEDING: <Zap className="w-4 h-4 text-destructive" />, GEOFENCE: <MapPin className="w-4 h-4 text-primary" />, IDLE: <Clock className="w-4 h-4 text-warning" /> };
  const detail = alert.alertType === 'SPEEDING' ? `Speed: ${alert.details.speedKph} kph` : alert.alertType === 'GEOFENCE' ? `Zone: ${alert.details.geofence || alert.details.zone}` : `Idle: ${alert.details.idleMinutes}m`;
  return <button onClick={onClick} className={`w-full text-left p-3 rounded-lg transition-all hover:bg-accent/50 ${styles[alert.alertType]}`}><div className="flex items-start gap-3"><div className="mt-0.5">{icons[alert.alertType]}</div><div className="flex-1"><div className="flex items-center justify-between gap-2"><span className="font-medium text-sm text-foreground">{alert.vehicleId}</span><span className="text-xs text-muted-foreground">{formatDistanceToNow(new Date(alert.timestamp), { addSuffix: true })}</span></div><p className="text-xs text-muted-foreground mt-0.5 capitalize">{alert.alertType.toLowerCase()} Alert</p><p className="text-xs text-foreground/70 mt-1">{detail}</p></div></div></button>;
}

export default Index;
