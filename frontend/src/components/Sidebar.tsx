import {
  BarChart3,
  ClipboardCheck,
  ClipboardList,
  Droplets,
  Home,
  Layers3,
  PackageCheck,
  Leaf,
  LogOut,
  Route,
  Settings,
  ShieldCheck,
  Sprout,
  Truck,
  UserRound,
  UsersRound,
  X,
  type LucideIcon
} from 'lucide-react';
import { useRef, type KeyboardEvent, type WheelEvent } from 'react';
import blueberryLogoMark from '../assets/brand/blueberry-logo-mark.webp';
import { initials } from '../lib/format';
import type { AuthenticatedUserResponse, ModuleResponse } from '../types/api';

interface SidebarProps {
  modules: ModuleResponse[];
  activeKey: string;
  user: AuthenticatedUserResponse | null;
  onSelect: (key: string) => void;
  onLogout?: () => void | Promise<void>;
  onOpenProfile?: () => void;
  onClose?: () => void;
}

interface SidebarNavigationItem {
  key: string;
  label: string;
  icon: LucideIcon;
  targetKey?: string;
  requiresModule?: string;
  activeWhen?: string[];
  action?: 'profile';
}

interface SidebarSection {
  title: string;
  items: SidebarNavigationItem[];
}

const sidebarSections: SidebarSection[] = [
  {
    title: 'General',
    items: [{ key: 'dashboard', label: 'Inicio', icon: Home, targetKey: 'dashboard', requiresModule: 'dashboard' }]
  },
  {
    title: 'Administración',
    items: [
      { key: 'usuarios', label: 'Usuarios', icon: UserRound, targetKey: 'usuarios', requiresModule: 'usuarios' },
      { key: 'roles', label: 'Roles', icon: UsersRound, targetKey: 'roles', requiresModule: 'roles' }
    ]
  },
  {
    title: 'Estructura',
    items: [
      { key: 'lotes', label: 'Lotes', icon: ClipboardCheck, targetKey: 'lotes', requiresModule: 'lotes' },
      { key: 'camas', label: 'Camas', icon: Layers3, targetKey: 'camas', requiresModule: 'camas' },
      { key: 'jabas', label: 'Jabas de siembra', icon: PackageCheck, targetKey: 'jabas', requiresModule: 'jabas' },
      { key: 'lotes-trazables', label: 'Lotes trazables', icon: Route, targetKey: 'lotes-trazables', requiresModule: 'lotes_trazables' }
    ]
  },
  {
    title: 'Operación',
    items: [
      { key: 'siembra', label: 'Siembras', icon: Sprout, targetKey: 'siembra', requiresModule: 'siembra' },
      { key: 'riegos', label: 'Riegos programados', icon: Droplets, targetKey: 'riegos', requiresModule: 'riegos' },
      { key: 'uniformizaciones', label: 'Uniformizaciones', icon: Leaf, targetKey: 'uniformizaciones', requiresModule: 'procesos' },
      { key: 'formalizaciones', label: 'Formalizaciones', icon: ClipboardList, targetKey: 'formalizaciones', requiresModule: 'procesos' },
      { key: 'clasificacion', label: 'Clasificaciones', icon: ShieldCheck, targetKey: 'clasificacion', requiresModule: 'clasificacion' },
      { key: 'recuperacion', label: 'Recuperación por riego', icon: Droplets, targetKey: 'recuperacion', requiresModule: 'recuperacion' },
      { key: 'pedidos', label: 'Pedidos por variedad', icon: ClipboardList, targetKey: 'pedidos', requiresModule: 'pedidos' },
      { key: 'empaques', label: 'Empaques', icon: PackageCheck, targetKey: 'empaques', requiresModule: 'empaques' },
      { key: 'despacho', label: 'Despachos', icon: Truck, targetKey: 'despacho', requiresModule: 'despacho' },
      { key: 'mermas', label: 'Mermas', icon: ClipboardList, targetKey: 'mermas', requiresModule: 'mermas' }
    ]
  },
  {
    title: 'Seguimiento',
    items: [
      { key: 'trazabilidad', label: 'Trazabilidad', icon: Route, targetKey: 'trazabilidad', requiresModule: 'trazabilidad' },
      { key: 'reportes', label: 'Reportes', icon: BarChart3, targetKey: 'reportes', requiresModule: 'reportes' },
      { key: 'auditoria', label: 'Auditoría', icon: ShieldCheck, targetKey: 'auditoria', requiresModule: 'auditoria' }
    ]
  },
  {
    title: 'Mi cuenta',
    items: [{ key: 'configuracion', label: 'Configuración', icon: Settings, action: 'profile' }]
  }
];

function canShow(item: SidebarNavigationItem, availableModuleKeys: Set<string>) {
  return !item.requiresModule || availableModuleKeys.has(item.requiresModule);
}

export function Sidebar({ modules, activeKey, user, onSelect, onLogout, onOpenProfile, onClose }: SidebarProps) {
  const navigationRef = useRef<HTMLDivElement>(null);
  const availableModuleKeys = new Set(modules.map((module) => module.key));
  const visibleSections = sidebarSections
    .map((section) => ({
      ...section,
      items: section.items.filter((item) => canShow(item, availableModuleKeys))
    }))
    .filter((section) => section.items.length > 0);

  function scrollNavigation(delta: number) {
    const navigation = navigationRef.current;
    if (!navigation || navigation.scrollHeight <= navigation.clientHeight) return false;

    const nextPosition = Math.max(0, Math.min(
      navigation.scrollHeight - navigation.clientHeight,
      navigation.scrollTop + delta
    ));

    if (nextPosition === navigation.scrollTop) return false;
    navigation.scrollTop = nextPosition;
    return true;
  }

  function handleSidebarWheel(event: WheelEvent<HTMLElement>) {
    if (scrollNavigation(event.deltaY)) {
      event.preventDefault();
    }
  }

  function handleSidebarKeyDown(event: KeyboardEvent<HTMLElement>) {
    const navigation = navigationRef.current;
    if (!navigation || navigation.scrollHeight <= navigation.clientHeight) return;

    const keyboardScroll: Record<string, number> = {
      ArrowDown: 56,
      ArrowUp: -56,
      PageDown: navigation.clientHeight * 0.82,
      PageUp: -(navigation.clientHeight * 0.82),
      Home: -navigation.scrollHeight,
      End: navigation.scrollHeight
    };

    const delta = keyboardScroll[event.key];
    if (delta === undefined) return;

    if (scrollNavigation(delta)) {
      event.preventDefault();
    }
  }

  return (
    <aside className="sidebar sidebar--reference" tabIndex={0} onWheel={handleSidebarWheel} onKeyDown={handleSidebarKeyDown}>
      <div className="sidebar__brand-row">
        <div className="brand brand--sidebar" aria-label="BlueberryTrace">
          <img src={blueberryLogoMark} alt="Logo BlueberryTrace" />
          <strong><span>Blueberry</span>Trace</strong>
        </div>
        <button type="button" className="icon-button sidebar-close" aria-label="Cerrar menú" onClick={onClose}>
          <X size={18} />
        </button>
      </div>

      <div ref={navigationRef} className="sidebar__sections sidebar__sections--stacked">
        {visibleSections.map((section) => (
          <section key={section.title} className="sidebar__section">
            <span className="sidebar__section-title">{section.title}</span>
            <nav className="sidebar__nav sidebar__nav--reference" aria-label={section.title}>
              {section.items.map((item) => {
                const Icon = item.icon;
                const isActive = item.key === activeKey || item.activeWhen?.includes(activeKey) || false;

                return (
                  <button
                    key={item.key}
                    type="button"
                    className={isActive ? 'sidebar__link sidebar__link--active' : 'sidebar__link'}
                    onClick={() => {
                      if (item.action === 'profile') {
                        onOpenProfile?.();
                        return;
                      }
                      if (item.targetKey) {
                        onSelect(item.targetKey);
                      }
                    }}
                  >
                    <Icon size={18} strokeWidth={1.85} />
                    <span>{item.label}</span>
                  </button>
                );
              })}
            </nav>
          </section>
        ))}
      </div>

      <div className="sidebar__footer sidebar__footer--compact">
        <div className="sidebar-user-card">
          <span className={`sidebar-user-card__avatar sidebar-user-card__avatar--${user?.avatarColor || 'emerald'} ${user?.avatarImage ? 'sidebar-user-card__avatar--image' : ''}`}>{user?.avatarImage ? <img src={user.avatarImage} alt="Foto de perfil" /> : initials(user?.nombreCompleto)}</span>
          <div>
            <strong>{user?.nombreCompleto || 'Sesión activa'}</strong>
            <small>{user?.cargo || user?.rol || user?.username || 'Operario'}</small>
          </div>
        </div>
        <button type="button" className="sidebar-logout" onClick={() => { onClose?.(); onLogout?.(); }}>
          <LogOut size={16} />
          Cerrar sesión
        </button>
      </div>
    </aside>
  );
}
