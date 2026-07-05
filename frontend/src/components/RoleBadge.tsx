import { ShieldCheck } from 'lucide-react';
import type { CSSProperties } from 'react';

interface RoleBadgeProps {
  label?: string | null;
  color?: string | null;
  compact?: boolean;
}

function visibleLabel(value?: string | null) {
  const normalized = String(value || 'Sin rol').replace(/_/g, ' ').trim();
  return normalized || 'Sin rol';
}

export function RoleBadge({ label, color = '#64748B', compact = false }: RoleBadgeProps) {
  const roleColor = /^#[0-9a-f]{6}$/i.test(String(color || '')) ? String(color) : '#64748B';

  return (
    <span
      className={compact ? 'role-badge role-badge--compact' : 'role-badge'}
      style={{ '--role-color': roleColor } as CSSProperties}
      title={visibleLabel(label)}
    >
      <i aria-hidden="true"><ShieldCheck size={compact ? 13 : 14} /></i>
      <span>{visibleLabel(label)}</span>
    </span>
  );
}
