import type { ReactNode } from 'react';
import { CircleCheck, PackageOpen } from 'lucide-react';

interface EmptyStateProps {
  title: string;
  description: string;
  icon?: ReactNode;
  compact?: boolean;
  inline?: boolean;
  action?: ReactNode;
  tone?: 'default' | 'success' | 'info';
}

export function EmptyState({ title, description, icon, compact = false, inline = false, action, tone = 'default' }: EmptyStateProps) {
  const className = [
    'empty-state',
    compact ? 'empty-state--compact' : '',
    inline ? 'empty-state--inline' : '',
    `empty-state--${tone}`
  ].filter(Boolean).join(' ');

  return (
    <div className={className}>
      <span className="empty-state__icon">{icon || (tone === 'success' ? <CircleCheck size={26} /> : <PackageOpen size={26} />)}</span>
      <strong>{title}</strong>
      <small>{description}</small>
      {action ? <div className="empty-state__action">{action}</div> : null}
    </div>
  );
}
