import type { ReactNode } from 'react';
import { ArrowUpRight } from 'lucide-react';
import { numberCompact } from '../lib/format';

interface MetricCardProps {
  label: string;
  value: number | string;
  detail: string;
  icon: ReactNode;
  tone?: 'green' | 'blue' | 'purple' | 'orange';
}

export function MetricCard({ label, value, detail, icon, tone = 'green' }: MetricCardProps) {
  return (
    <article className={`metric-card metric-card--${tone}`}>
      <div className="metric-card__header">
        <span className="metric-card__icon">{icon}</span>
        <span className="metric-card__trend"><ArrowUpRight size={15} /> Operativo</span>
      </div>
      <p>{label}</p>
      <strong>{typeof value === 'number' ? numberCompact(value) : value}</strong>
      <small>{detail}</small>
    </article>
  );
}
