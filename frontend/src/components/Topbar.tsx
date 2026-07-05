import { useEffect, useMemo, useRef, useState } from 'react';
import { Bell, ChevronDown, Clock3, LogOut, Mail, Menu, RefreshCcw, Search, ShieldCheck, UserRound } from 'lucide-react';
import { dateShort, initials } from '../lib/format';
import type { AuthenticatedUserResponse } from '../types/api';

export interface TopbarNotification {
  id: string;
  title: string;
  description: string;
  tone?: 'success' | 'info' | 'warning' | 'danger';
  createdAt?: string | null;
  moduleKey?: string;
}

interface TopbarProps {
  user: AuthenticatedUserResponse | null;
  activeModule: string;
  activeKey?: string;
  notifications?: TopbarNotification[];
  onOpenSearch: () => void;
  onOpenSidebar?: () => void;
  onRefresh?: () => void | Promise<void>;
  onNavigate?: (key: string) => void;
  onOpenProfile?: () => void;
  onLogout?: () => void | Promise<void>;
  refreshing?: boolean;
}

const subtitles: Record<string, string> = {
  dashboard: 'Resumen general de operaciones',
  usuarios: 'Administración de usuarios y accesos',
  lotes: 'Gestión de lotes e invernaderos',
  camas: 'Control de camas productivas',
  siembra: 'Registro de siembras por lote',
  uniformizaciones: 'Seguimiento y control de uniformizaciones',
  formalizaciones: 'Seguimiento y control de formalizaciones',
  clasificacion: 'Control de calidad y clasificación',
  despacho: 'Despachos y salidas registradas',
  trazabilidad: 'Seguimiento por lote productivo',
  reportes: 'Indicadores y reportes operativos'
};

const titles: Record<string, string> = {
  dashboard: 'Panel principal'
};

export function Topbar({
  user,
  activeModule,
  activeKey = 'dashboard',
  notifications = [],
  onOpenSearch,
  onOpenSidebar,
  onRefresh,
  onNavigate,
  onOpenProfile,
  onLogout,
  refreshing = false
}: TopbarProps) {
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const notificationRef = useRef<HTMLDivElement>(null);
  const profileRef = useRef<HTMLDivElement>(null);
  const unreadCount = notifications.length;

  const displayRole = useMemo(() => user?.cargo || user?.rol || user?.authorities?.[0] || 'Operario', [user]);
  const avatarColor = user?.avatarColor || 'emerald';
  const avatarImage = user?.avatarImage || null;
  const pageTitle = titles[activeKey] || activeModule;
  const pageSubtitle = subtitles[activeKey] || 'Gestión operativa BlueberryTrace';

  useEffect(() => {
    function onPointerDown(event: MouseEvent) {
      const target = event.target as Node;
      if (notificationsOpen && notificationRef.current && !notificationRef.current.contains(target)) {
        setNotificationsOpen(false);
      }
      if (profileOpen && profileRef.current && !profileRef.current.contains(target)) {
        setProfileOpen(false);
      }
    }

    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setNotificationsOpen(false);
        setProfileOpen(false);
      }
    }

    window.addEventListener('mousedown', onPointerDown);
    window.addEventListener('keydown', onKeyDown);
    return () => {
      window.removeEventListener('mousedown', onPointerDown);
      window.removeEventListener('keydown', onKeyDown);
    };
  }, [notificationsOpen, profileOpen]);

  return (
    <header className="topbar topbar--reference">
      <div className="topbar__inner">
        <button className="icon-button topbar-menu-trigger" type="button" aria-label="Abrir menú de navegación" onClick={onOpenSidebar}>
          <Menu size={20} strokeWidth={2} />
        </button>

        <div className="topbar__page-title">
          <h1>{pageTitle}</h1>
          <p>{pageSubtitle}</p>
        </div>

        <button className="topbar-search-trigger topbar-search-trigger--wide" type="button" onClick={onOpenSearch} aria-label="Abrir búsqueda global">
          <Search size={18} strokeWidth={1.9} />
          <span>Buscar lotes, camas, siembras...</span>
          <kbd>Ctrl + K</kbd>
        </button>

        <div className="topbar__actions">
          {onRefresh ? (
            <button className="icon-button topbar-refresh" type="button" aria-label="Sincronizar" onClick={onRefresh} disabled={refreshing}>
              <RefreshCcw className={refreshing ? 'spin' : undefined} size={17} />
            </button>
          ) : null}

          <div className="topbar-menu" ref={notificationRef}>
            <button
              className={notificationsOpen ? 'icon-button notification-button notification-button--active' : 'icon-button notification-button'}
              type="button"
              aria-label="Notificaciones"
              aria-expanded={notificationsOpen}
              onClick={() => {
                setNotificationsOpen((current) => !current);
                setProfileOpen(false);
              }}
            >
              <Bell size={23} strokeWidth={1.85} />
              {unreadCount > 0 ? <span>{unreadCount > 9 ? '9+' : unreadCount}</span> : null}
            </button>

            {notificationsOpen ? (
              <section className="topbar-popover notification-panel" aria-label="Panel de notificaciones">
                <header className="topbar-popover__header">
                  <div>
                    <strong>Notificaciones</strong>
                    <small>{unreadCount > 0 ? `${unreadCount} eventos operativos detectados` : 'Sin alertas pendientes'}</small>
                  </div>
                </header>

                <div className="notification-list">
                  {notifications.length > 0 ? notifications.map((notification) => (
                    <button
                      key={notification.id}
                      type="button"
                      className={`notification-item notification-item--${notification.tone || 'info'}`}
                      onClick={() => {
                        if (notification.moduleKey && onNavigate) {
                          onNavigate(notification.moduleKey);
                        }
                        setNotificationsOpen(false);
                      }}
                    >
                      <span className="notification-item__dot" />
                      <div>
                        <strong>{notification.title}</strong>
                        <small>{notification.description}</small>
                        {notification.createdAt ? <em><Clock3 size={12} /> {dateShort(notification.createdAt)}</em> : null}
                      </div>
                    </button>
                  )) : (
                    <div className="notification-empty">
                      <Bell size={22} />
                      <strong>Todo está al día</strong>
                      <small>No hay eventos críticos generados por los datos actuales.</small>
                    </div>
                  )}
                </div>
              </section>
            ) : null}
          </div>

          <div className="topbar-menu topbar-menu--profile" ref={profileRef}>
            <button
              className={profileOpen ? `topbar-avatar topbar-avatar--${avatarColor} topbar-avatar--active` : `topbar-avatar topbar-avatar--${avatarColor}`}
              type="button"
              aria-label="Abrir menú de perfil"
              aria-expanded={profileOpen}
              onClick={() => {
                setProfileOpen((current) => !current);
                setNotificationsOpen(false);
              }}
            >
              <span className={avatarImage ? "topbar-avatar__photo topbar-avatar__photo--image" : "topbar-avatar__photo"}>{avatarImage ? <img src={avatarImage} alt="Foto de perfil" /> : initials(user?.nombreCompleto)}</span>
              <span className="topbar-avatar__meta">
                <strong>{user?.nombreCompleto || user?.username || 'Usuario autenticado'}</strong>
                <small>{displayRole}</small>
              </span>
              <ChevronDown size={17} />
            </button>

            {profileOpen ? (
              <section className="topbar-popover profile-panel" aria-label="Menú de perfil">
                <header className="profile-panel__hero">
                  <span className={avatarImage ? "profile-panel__avatar profile-panel__avatar--image" : "profile-panel__avatar"}>{avatarImage ? <img src={avatarImage} alt="Foto de perfil" /> : initials(user?.nombreCompleto)}</span>
                  <div>
                    <strong>{user?.nombreCompleto || 'Sesión activa'}</strong>
                    <small>{displayRole}</small>
                  </div>
                </header>

                <div className="profile-panel__details">
                  <p><Mail size={15} /> {user?.email || 'correo no registrado'}</p>
                  <p><UserRound size={15} /> {user?.username || 'usuario'}</p>
                  <p><ShieldCheck size={15} /> {displayRole}</p>
                  {user?.telefono ? <p><Clock3 size={15} /> {user.telefono}</p> : null}
                </div>

                <div className="profile-panel__actions">
                  <button type="button" className="ghost-button" onClick={() => { onOpenProfile?.(); setProfileOpen(false); }}>Editar perfil</button>
                  <button type="button" className="ghost-button" onClick={() => onNavigate?.('usuarios')}>Ver usuarios</button>
                  <button type="button" className="action-button action-button--soft" onClick={onLogout}>
                    <LogOut size={15} /> Cerrar sesión
                  </button>
                </div>
              </section>
            ) : null}
          </div>
        </div>
      </div>
    </header>
  );
}
