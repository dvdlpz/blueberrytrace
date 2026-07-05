import {
  AlertCircle,
  AlertTriangle,
  BadgeCheck,
  Boxes,
  CheckCircle2,
  CircleOff,
  ClipboardCheck,
  Clock3,
  Eye,
  Factory,
  FlaskConical,
  Leaf,
  Package,
  ShieldCheck,
  ShieldEllipsis,
  Sprout,
  Tag,
  Tractor,
  Truck,
  UserCog,
  UserRound,
  UsersRound,
  XCircle,
  type LucideIcon
} from 'lucide-react';

interface StatusBadgeProps {
  value?: string | null;
}

interface BadgeMeta {
  tone: 'success' | 'warning' | 'info' | 'danger' | 'neutral';
  variant?: string;
  icon: LucideIcon;
}

function normalize(value?: string | null) {
  return String(value || '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .trim()
    .toUpperCase();
}

function resolveBadgeMeta(value?: string | null): BadgeMeta {
  const normalized = normalize(value);

  if (/^ADMINISTRADOR$/.test(normalized)) {
    return { tone: 'info', variant: 'role-admin', icon: ShieldCheck };
  }
  if (/^SUPERVISOR$/.test(normalized)) {
    return { tone: 'info', variant: 'role-supervisor', icon: UsersRound };
  }
  if (/^OPERARIO$/.test(normalized)) {
    return { tone: 'warning', variant: 'role-operario', icon: Tractor };
  }
  if (/CONTROL\s+DE\s+CALIDAD/.test(normalized)) {
    return { tone: 'success', variant: 'role-quality', icon: FlaskConical };
  }
  if (/^CONSULTA$|^LECTURA$|^AUDITOR$/.test(normalized)) {
    return { tone: 'neutral', variant: 'role-reader', icon: Eye };
  }

  if (/PRIMERA\s+CALIDAD|^PRIMERA$/.test(normalized)) {
    return { tone: 'success', variant: 'quality-primary', icon: BadgeCheck };
  }
  if (/SEGUNDA\s+CALIDAD|^SEGUNDA$/.test(normalized)) {
    return { tone: 'info', variant: 'quality-secondary', icon: Tag };
  }
  if (/TERCERA\s+CALIDAD|^TERCERA$/.test(normalized)) {
    return { tone: 'warning', variant: 'quality-tertiary', icon: Boxes };
  }
  if (/DESCARTE/.test(normalized)) {
    return { tone: 'danger', variant: 'quality-discard', icon: AlertTriangle };
  }
  if (/SIN\s+CRITERIO|SIN\s+CLASIFICAR/.test(normalized)) {
    return { tone: 'neutral', variant: 'quality-empty', icon: ShieldEllipsis };
  }

  if (/^ACTIVO$|^ACTIVA$|^VALIDADA$|^APROBADO$|^APROBADA$|^CERRADO$|^CERRADA$|^REGISTRADO$|^REGISTRADA$|^COMPLETADO$|^COMPLETADA$|^DESPACHADO$|^DESPACHADA$/.test(normalized)) {
    return { tone: 'success', variant: 'status-active', icon: CheckCircle2 };
  }
  if (/^PREPARADO$|^PREPARADA$|PENDIENTE|OBSERVADO|OBSERVADA|MANTENIMIENTO|INICIADO/.test(normalized)) {
    return { tone: 'warning', variant: 'status-pending', icon: Clock3 };
  }
  if (/^CARGADO$|^CARGADA$|EN\s*PROCESO|TRANSITO|EN\s*TRANSITO|PROCESO/.test(normalized)) {
    return { tone: 'info', variant: 'status-progress', icon: Truck };
  }
  if (/INACTIVO|INACTIVA|RECHAZADO|RECHAZADA|ANULADO|ANULADA|CANCELADO|CANCELADA|ELIMINADO|DESCARTE/.test(normalized)) {
    return { tone: 'danger', variant: 'status-danger', icon: XCircle };
  }
  if (/COSECHA/.test(normalized)) {
    return { tone: 'info', variant: 'status-harvest', icon: Leaf };
  }

  if (/JABA|BIN|MADERA|CAJA|PALET/.test(normalized)) {
    return { tone: 'neutral', variant: 'dispatch-mode', icon: Package };
  }
  if (/EXPORTACION|LOCAL|NACIONAL/.test(normalized)) {
    return { tone: 'neutral', variant: 'dispatch-route', icon: Truck };
  }
  if (/LOTE|INVERNADERO/.test(normalized)) {
    return { tone: 'neutral', variant: 'trace-lot', icon: Factory };
  }
  if (/USUARIO|RESPONSABLE|SUPERVISION/.test(normalized)) {
    return { tone: 'neutral', variant: 'trace-user', icon: UserRound };
  }

  return { tone: 'neutral', variant: 'default', icon: UserCog };
}

export function StatusBadge({ value }: StatusBadgeProps) {
  const label = value || 'Sin estado';
  const meta = resolveBadgeMeta(label);
  const Icon = meta.icon;
  const classNames = ['status-badge', `status-badge--${meta.tone}`];
  if (meta.variant) {
    classNames.push(`status-badge--${meta.variant}`);
  }

  return (
    <span className={classNames.join(' ')}>
      <i className="status-badge__icon" aria-hidden="true">
        <Icon size={14} />
      </i>
      <span>{label}</span>
    </span>
  );
}
