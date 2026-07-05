import { useEffect, useId, useRef } from 'react';
import { X } from 'lucide-react';
import { createPortal } from 'react-dom';
import type { ReactNode } from 'react';

interface ModalProps {
  open: boolean;
  title: string;
  description?: string;
  eyebrow?: string;
  icon?: ReactNode;
  children: ReactNode;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  closeOnBackdrop?: boolean;
  dismissible?: boolean;
  onClose: () => void;
}

export function Modal({
  open,
  title,
  description,
  eyebrow = 'Gestión operativa',
  icon,
  children,
  size = 'lg',
  closeOnBackdrop = true,
  dismissible = true,
  onClose
}: ModalProps) {
  const titleId = useId();
  const descriptionId = useId();
  const closeButtonRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!open) return;

    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const focusTimer = window.setTimeout(() => closeButtonRef.current?.focus(), 80);

    function onKeyDown(event: KeyboardEvent) {
      if (dismissible && event.key === 'Escape') onClose();
    }

    window.addEventListener('keydown', onKeyDown);
    return () => {
      window.clearTimeout(focusTimer);
      window.removeEventListener('keydown', onKeyDown);
      document.body.style.overflow = originalOverflow;
    };
  }, [open, dismissible, onClose]);

  if (!open) return null;

  return createPortal(
    <div className="modal-backdrop" role="presentation" onMouseDown={closeOnBackdrop && dismissible ? onClose : undefined}>
      <section
        className={`modal-card modal-card--${size}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={description ? descriptionId : undefined}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <header className="modal-card__header">
          <div className="modal-card__heading">
            {icon ? <span className="modal-card__icon" aria-hidden="true">{icon}</span> : null}
            <div>
              <span className="modal-card__eyebrow">{eyebrow}</span>
              <strong id={titleId}>{title}</strong>
              {description ? <span id={descriptionId}>{description}</span> : null}
            </div>
          </div>
          {dismissible ? (
            <button ref={closeButtonRef} type="button" className="icon-button modal-card__close" onClick={onClose} aria-label="Cerrar ventana">
              <X size={18} />
            </button>
          ) : null}
        </header>
        <div className="modal-card__body">{children}</div>
      </section>
    </div>,
    document.body
  );
}
