import { useEffect, useMemo, useRef, useState } from 'react';
import { ArrowRight, Command, RefreshCcw, Search, X } from 'lucide-react';
import { getModuleIcon } from '../lib/moduleIcons';
import type { ModuleResponse } from '../types/api';

export interface CommandSearchItem {
  id: string;
  label: string;
  description: string;
  moduleKey: string;
  type: string;
}

interface CommandPaletteProps {
  open: boolean;
  modules: ModuleResponse[];
  activeKey: string;
  searchItems?: CommandSearchItem[];
  onClose: () => void;
  onSelect: (key: string) => void;
  onRefresh: () => void | Promise<void>;
}

const hints: Record<string, string> = {
  dashboard: 'Resumen visual de producción, despacho y calidad',
  lotes: 'Consulta lotes, invernaderos y estado productivo',
  camas: 'Revisa camas productivas y capacidad referencial',
  siembra: 'Registra nuevas siembras por lote y cama',
  procesos: 'Controla uniformización y formalización',
  clasificacion: 'Gestiona clasificación y validación de plantas',
  despacho: 'Registra salidas y seguimiento de despachos',
  reportes: 'Genera reportes y lectura de trazabilidad',
  usuarios: 'Administra usuarios, roles y accesos'
};

export function CommandPalette({ open, modules, activeKey, searchItems = [], onClose, onSelect, onRefresh }: CommandPaletteProps) {
  const [query, setQuery] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (!open) {
      setQuery('');
      return;
    }

    const timeout = window.setTimeout(() => inputRef.current?.focus(), 80);
    return () => window.clearTimeout(timeout);
  }, [open]);

  useEffect(() => {
    if (!open) {
      return;
    }

    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose();
      }
    }

    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [open, onClose]);

  const filteredModules = useMemo(() => {
    const term = query.trim().toLowerCase();
    if (!term) {
      return modules;
    }
    return modules.filter((module) => [module.label, module.key, hints[module.key]]
      .some((value) => String(value || '').toLowerCase().includes(term)));
  }, [modules, query]);

  const filteredItems = useMemo(() => {
    const term = query.trim().toLowerCase();
    if (!term) {
      return searchItems.slice(0, 8);
    }

    return searchItems
      .filter((item) => [item.label, item.description, item.type]
        .some((value) => String(value || '').toLowerCase().includes(term)))
      .slice(0, 10);
  }, [searchItems, query]);

  if (!open) {
    return null;
  }

  return (
    <div className="command-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="command-palette" role="dialog" aria-modal="true" aria-label="Comandos rápidos" onMouseDown={(event) => event.stopPropagation()}>
        <header className="command-palette__header">
          <div className="command-palette__brand">
            <span><Command size={18} /></span>
            <div>
              <strong>Centro de búsqueda</strong>
              <small>Busca módulos y registros operativos</small>
            </div>
          </div>
          <button type="button" className="icon-button" aria-label="Cerrar buscador" onClick={onClose}>
            <X size={17} />
          </button>
        </header>

        <label className="command-search">
          <Search size={18} />
          <input ref={inputRef} value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Buscar lote, cama, usuario, despacho o módulo..." />
          <kbd>ESC</kbd>
        </label>

        <div className="command-section">
          <span className="command-section__title">Acciones rápidas</span>
          <button
            type="button"
            className="command-item command-item--accent"
            onClick={async () => {
              await onRefresh();
              onClose();
            }}
          >
            <span className="command-item__icon"><RefreshCcw size={17} /></span>
            <div>
              <strong>Sincronizar datos</strong>
              <small>Recarga la información operativa actual</small>
            </div>
            <ArrowRight size={16} />
          </button>
        </div>

        <div className="command-section">
          <span className="command-section__title">Resultados de datos</span>
          <div className="command-list">
            {filteredItems.map((item) => {
              const ItemIcon = getModuleIcon(item.moduleKey);
              return (
              <button
                key={item.id}
                type="button"
                className="command-item command-item--data"
                onClick={() => {
                  onSelect(item.moduleKey);
                  onClose();
                }}
              >
                <span className="command-item__icon"><ItemIcon size={16} /></span>
                <div>
                  <strong>{item.label}</strong>
                  <small>{item.description}</small>
                </div>
                <em>{item.type}</em>
              </button>
              );
            })}
            {filteredItems.length === 0 ? (
              <div className="command-empty">
                No hay registros reales que coincidan con “{query || 'tu búsqueda'}”.
              </div>
            ) : null}
          </div>
        </div>

        <div className="command-section">
          <span className="command-section__title">Módulos</span>
          <div className="command-list">
            {filteredModules.map((module) => {
              const ModuleIcon = getModuleIcon(module.key);
              return (
              <button
                key={module.key}
                type="button"
                className={module.key === activeKey ? 'command-item command-item--active' : 'command-item'}
                onClick={() => {
                  onSelect(module.key);
                  onClose();
                }}
              >
                <span className="command-item__icon"><ModuleIcon size={16} /></span>
                <div>
                  <strong>{module.label}</strong>
                  <small>{hints[module.key] || 'Acceso al módulo'}</small>
                </div>
                <ArrowRight size={16} />
              </button>
              );
            })}
            {filteredModules.length === 0 && (
              <div className="command-empty">
                No encontré módulos para “{query}”.
              </div>
            )}
          </div>
        </div>
      </section>
    </div>
  );
}
