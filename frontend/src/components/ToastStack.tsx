import { CheckCircle2, Info, TriangleAlert, X, XCircle } from 'lucide-react';

export type ToastTone = 'success' | 'info' | 'warning' | 'error';

export interface ToastItem {
  id: number;
  tone: ToastTone;
  title: string;
  description?: string;
}

interface ToastStackProps {
  toasts: ToastItem[];
  onDismiss: (id: number) => void;
}

const icons = {
  success: CheckCircle2,
  info: Info,
  warning: TriangleAlert,
  error: XCircle
};

export function ToastStack({ toasts, onDismiss }: ToastStackProps) {
  if (toasts.length === 0) {
    return null;
  }

  return (
    <div className="toast-stack" aria-live="polite" aria-relevant="additions removals">
      {toasts.map((toast) => {
        const Icon = icons[toast.tone];
        return (
          <article key={toast.id} className={`toast-card toast-card--${toast.tone}`}>
            <span className="toast-card__icon"><Icon size={18} /></span>
            <div>
              <strong>{toast.title}</strong>
              {toast.description ? <small>{toast.description}</small> : null}
            </div>
            <button type="button" aria-label="Cerrar notificación" onClick={() => onDismiss(toast.id)}>
              <X size={15} />
            </button>
          </article>
        );
      })}
    </div>
  );
}
