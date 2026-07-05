import type { ReactNode } from 'react';

interface InfoGridItem {
  label: string;
  value: ReactNode;
  tone?: 'green' | 'blue' | 'purple' | 'orange' | 'red' | 'neutral';
}

interface InfoGridProps {
  items: InfoGridItem[];
}

export function InfoGrid({ items }: InfoGridProps) {
  return (
    <dl className="info-grid">
      {items.map((item) => (
        <div key={item.label} className={`info-grid__item info-grid__item--${item.tone || 'neutral'}`}>
          <dt>{item.label}</dt>
          <dd>{item.value}</dd>
        </div>
      ))}
    </dl>
  );
}
