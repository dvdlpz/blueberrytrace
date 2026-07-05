import { useEffect, useId, useRef } from 'react';
import { AlertTriangle, CheckCircle2, Loader2, X } from 'lucide-react';
import { createPortal } from 'react-dom';

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  description: string;
  confirmLabel?: string;
  cancelLabel?: string;
  tone?: 'success' | 'warning' | 'danger';
  loading?: boolean;
  onCancel: () => void;
  onConfirm: () => Promise<void> | void;
}

export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = 'Confirmar',
  cancelLabel = 'Cancelar',
  tone = 'warning',
  loading = false,
  onCancel,
  onConfirm
}: ConfirmDialogProps) {
  const titleId = useId();
  const descriptionId = useId();
  const cancelRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!open) {
      return;
    }

    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const focusTimer = window.setTimeout(() => cancelRef.current?.focus(), 80);

    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !loading) {
        onCancel();
      }
    }

    window.addEventListener('keydown', onKeyDown);
    return () => {
      window.clearTimeout(focusTimer);
      window.removeEventListener('keydown', onKeyDown);
      document.body.style.overflow = originalOverflow;
    };
  }, [open, loading, onCancel]);

  if (!open) {
    return null;
  }

  const Icon = tone === 'success' ? CheckCircle2 : AlertTriangle;

  return createPortal(
    <div className="confirm-backdrop" role="presentation" onMouseDown={loading ? undefined : onCancel}>
      <section
        className={`confirm-dialog confirm-dialog--${tone}`}
        role="alertdialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={descriptionId}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <button type="button" className="confirm-dialog__close" aria-label="Cerrar confirmación" onClick={onCancel} disabled={loading}>
          <X size={16} />
        </button>
        <span className="confirm-dialog__icon"><Icon size={22} /></span>
        <div className="confirm-dialog__content">
          <h2 id={titleId}>{title}</h2>
          <p id={descriptionId}>{description}</p>
        </div>
        <footer className="confirm-dialog__actions">
          <button ref={cancelRef} type="button" className="ghost-button" onClick={onCancel} disabled={loading}>{cancelLabel}</button>
          <button type="button" className={tone === 'danger' ? 'action-button action-button--danger' : 'action-button'} onClick={onConfirm} disabled={loading}>
            {loading ? <Loader2 className="spin" size={16} /> : null}
            {loading ? 'Procesando...' : confirmLabel}
          </button>
        </footer>
      </section>
    </div>,
    document.body
  );
}
