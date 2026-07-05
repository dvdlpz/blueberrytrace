import type { ReactNode } from 'react';
import { AlertCircle, CircleAlert, Info } from 'lucide-react';

interface FormSectionProps {
  title: string;
  description?: string;
  icon?: ReactNode;
  children: ReactNode;
  className?: string;
}

export function FormSection({ title, description, icon, children, className = '' }: FormSectionProps) {
  return (
    <section className={`form-section ${className}`.trim()}>
      <header className="form-section__header">
        {icon ? <span className="form-section__icon" aria-hidden="true">{icon}</span> : null}
        <div>
          <h3>{title}</h3>
          {description ? <p>{description}</p> : null}
        </div>
      </header>
      <div className="form-section__content">{children}</div>
    </section>
  );
}

interface FormMessageProps {
  children: ReactNode;
  tone?: 'error' | 'info' | 'warning';
}

export function FormMessage({ children, tone = 'error' }: FormMessageProps) {
  const Icon = tone === 'error' ? AlertCircle : tone === 'warning' ? CircleAlert : Info;
  return (
    <div className={`form-message form-message--${tone}`} role={tone === 'error' ? 'alert' : 'status'}>
      <Icon size={18} aria-hidden="true" />
      <span>{children}</span>
    </div>
  );
}

interface FormPrerequisiteProps {
  title: string;
  description: string;
  action?: ReactNode;
}

export function FormPrerequisite({ title, description, action }: FormPrerequisiteProps) {
  return (
    <section className="form-prerequisite" role="status">
      <span className="form-prerequisite__icon"><CircleAlert size={22} /></span>
      <div>
        <strong>{title}</strong>
        <p>{description}</p>
        {action ? <div className="form-prerequisite__action">{action}</div> : null}
      </div>
    </section>
  );
}
