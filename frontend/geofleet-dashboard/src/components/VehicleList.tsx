import { useState, useMemo } from 'react';
import { Search, Filter, Truck, Gauge, Clock, ChevronRight } from 'lucide-react';
import { Vehicle } from '@/types/fleet';
import { formatDistanceToNow } from 'date-fns';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';

interface VehicleListProps {
  vehicles: Vehicle[];
  selectedVehicleId: string | null;
  onVehicleSelect: (vehicleId: string) => void;
}

export function VehicleList({ vehicles, selectedVehicleId, onVehicleSelect }: VehicleListProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [regionFilter, setRegionFilter] = useState<string>('all');

  const regions = useMemo(() => {
    const uniqueRegions = new Set(vehicles.map(v => v.region).filter(Boolean));
    return Array.from(uniqueRegions) as string[];
  }, [vehicles]);

  const filteredVehicles = useMemo(() => {
    return vehicles.filter(vehicle => {
      const matchesSearch = vehicle.vehicleId.toLowerCase().includes(searchQuery.toLowerCase());
      const matchesRegion = regionFilter === 'all' || vehicle.region === regionFilter;
      return matchesSearch && matchesRegion;
    });
  }, [vehicles, searchQuery, regionFilter]);

  const statusCounts = useMemo(() => ({
    online: vehicles.filter(v => v.status === 'online').length,
    idle: vehicles.filter(v => v.status === 'idle').length,
    offline: vehicles.filter(v => v.status === 'offline').length,
  }), [vehicles]);

  return (
    <div className="glass-panel h-full flex flex-col">
      <div className="px-4 py-3 border-b border-border">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Truck className="w-4 h-4 text-primary" />
            <h2 className="font-semibold text-foreground">Vehicles</h2>
          </div>
          <div className="flex items-center gap-2 text-xs">
            <span className="flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-success" />
              {statusCounts.online}
            </span>
            <span className="flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-warning" />
              {statusCounts.idle}
            </span>
            <span className="flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-destructive" />
              {statusCounts.offline}
            </span>
          </div>
        </div>
        
        {/* Search and Filter */}
        <div className="flex gap-2">
          <div className="relative flex-1">
            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
            <Input
              placeholder="Search vehicle..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-8 h-8 text-sm bg-muted/50 border-border"
            />
          </div>
          <Select value={regionFilter} onValueChange={setRegionFilter}>
            <SelectTrigger className="w-[120px] h-8 text-sm bg-muted/50 border-border">
              <Filter className="w-3 h-3 mr-1" />
              <SelectValue placeholder="Region" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Regions</SelectItem>
              {regions.map(region => (
                <SelectItem key={region} value={region}>{region}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>
      
      <ScrollArea className="flex-1 scrollbar-thin">
        <div className="p-2 space-y-1">
          {filteredVehicles.length === 0 ? (
            <div className="text-center py-8">
              <Truck className="w-8 h-8 text-muted-foreground mx-auto mb-2" />
              <p className="text-sm text-muted-foreground">No vehicles found</p>
            </div>
          ) : (
            filteredVehicles.map((vehicle) => (
              <VehicleItem
                key={vehicle.vehicleId}
                vehicle={vehicle}
                isSelected={vehicle.vehicleId === selectedVehicleId}
                onClick={() => onVehicleSelect(vehicle.vehicleId)}
              />
            ))
          )}
        </div>
      </ScrollArea>
    </div>
  );
}

interface VehicleItemProps {
  vehicle: Vehicle;
  isSelected: boolean;
  onClick: () => void;
}

function VehicleItem({ vehicle, isSelected, onClick }: VehicleItemProps) {
  const statusColors = {
    online: 'bg-success',
    idle: 'bg-warning',
    offline: 'bg-destructive',
  };

  const statusLabels = {
    online: '🟢',
    idle: '🟠',
    offline: '🔴',
  };

  return (
    <button
      onClick={onClick}
      className={`w-full text-left p-3 rounded-lg transition-all duration-200 hover:bg-accent/50 cursor-pointer ${
        isSelected ? 'bg-primary/10 border border-primary/30' : 'border border-transparent'
      }`}
    >
      <div className="flex items-center gap-3">
        <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${statusColors[vehicle.status || 'online']}/20`}>
          <span className="text-sm">{statusLabels[vehicle.status || 'online']}</span>
        </div>
        
        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between">
            <span className="font-medium text-sm text-foreground">{vehicle.vehicleId}</span>
            <ChevronRight className="w-4 h-4 text-muted-foreground" />
          </div>
          
          <div className="flex items-center gap-4 mt-1">
            <span className="flex items-center gap-1 text-xs text-muted-foreground">
              <Gauge className="w-3 h-3" />
              {Math.round(vehicle.speedKph)} kph
            </span>
            <span className="flex items-center gap-1 text-xs text-muted-foreground">
              <Clock className="w-3 h-3" />
              {formatDistanceToNow(new Date(vehicle.timestamp), { addSuffix: true })}
            </span>
          </div>
        </div>
      </div>
    </button>
  );
}