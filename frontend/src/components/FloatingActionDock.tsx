import { Command, RefreshCcw, Sparkles } from 'lucide-react';

interface FloatingActionDockProps {
  onOpenCommand: () => void;
  onRefresh: () => void | Promise<void>;
  refreshing?: boolean;
}

export function FloatingActionDock({ onOpenCommand, onRefresh, refreshing = false }: FloatingActionDockProps) {
  return (
    <div className="floating-dock" aria-label="Acciones rápidas">
      <button type="button" className="floating-dock__main" onClick={onOpenCommand}>
        <Command size={18} />
        <span>Buscar</span>
        <kbd>Ctrl K</kbd>
      </button>
      <button type="button" className="floating-dock__icon" onClick={onRefresh} disabled={refreshing} aria-label="Sincronizar datos">
        {refreshing ? <RefreshCcw className="spin" size={17} /> : <Sparkles size={17} />}
      </button>
    </div>
  );
}
