import type { ReactNode } from 'react';

interface ModuleHeaderProps {
  eyebrow: string;
  title: string;
  description: string;
  icon?: ReactNode;
  tone?: 'green' | 'blue' | 'purple' | 'orange';
  actions?: ReactNode;
}

export function ModuleHeader({ eyebrow, title, description, icon, tone = 'green', actions }: ModuleHeaderProps) {
  return (
    <section className="module-header">
      <div className="module-header__identity">
        {icon ? <span className={`module-header__icon module-header__icon--${tone}`}>{icon}</span> : null}
        <div>
          <span className="hero-panel__tag">{eyebrow}</span>
          <h2>{title}</h2>
          <p>{description}</p>
        </div>
      </div>
      {actions ? <div className="module-header__actions">{actions}</div> : null}
    </section>
  );
}
