import { useState } from 'react';
import { CalendarDays, Loader2, Save, Sprout } from 'lucide-react';
import { FormMessage, FormSection } from './FormLayout';
import type { LoteFormPayload } from '../types/api';

const today = new Date().toISOString().slice(0, 10);

const defaultPayload: LoteFormPayload = {
  codigo: '',
  descripcion: '',
  cultivo: 'Arándano',
  variedad: '',
  fechaRegistro: today,
  observacion: '',
  estado: 'ACTIVO'
};

interface LoteFormProps {
  initialData?: LoteFormPayload;
  submitLabel?: string;
  onSubmit: (payload: LoteFormPayload) => Promise<void>;
  onCancel: () => void;
}

export function LoteForm({ initialData, submitLabel = 'Guardar invernadero', onSubmit, onCancel }: LoteFormProps) {
  const [payload, setPayload] = useState<LoteFormPayload>(initialData || defaultPayload);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      setSaving(true);
      setError(null);
      await onSubmit({
        ...payload,
        codigo: payload.codigo.trim().toUpperCase(),
        descripcion: payload.descripcion.trim(),
        cultivo: payload.cultivo.trim(),
        variedad: payload.variedad.trim(),
        observacion: payload.observacion?.trim() || undefined
      });
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'No fue posible guardar la información del invernadero.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="form-shell" onSubmit={submit}>
      {error ? <FormMessage>{error}</FormMessage> : null}
      <FormSection title="Identificación del invernadero" description="Registra los datos que permiten reconocer esta ubicación dentro de la operación." icon={<Sprout size={18} />}>
        <div className="form-grid form-grid--two">
          <label>
            <span>Código de invernadero</span>
            <input value={payload.codigo} onChange={(event) => setPayload({ ...payload, codigo: event.target.value })} required maxLength={30} placeholder="Ej.: INV-001" autoComplete="off" />
            <small className="field-hint">Usa un código único y fácil de reconocer.</small>
          </label>
          <label>
            <span>Variedad</span>
            <input value={payload.variedad} onChange={(event) => setPayload({ ...payload, variedad: event.target.value })} required maxLength={120} placeholder="Ej.: Biloxi" />
          </label>
          <label className="form-grid__full">
            <span>Descripción</span>
            <input value={payload.descripcion} onChange={(event) => setPayload({ ...payload, descripcion: event.target.value })} required maxLength={150} placeholder="Ubicación o referencia del invernadero" />
          </label>
        </div>
      </FormSection>

      <FormSection title="Condición operativa" description="Define la vigencia del invernadero y agrega una observación solo cuando sea útil para la operación." icon={<CalendarDays size={18} />}>
        <div className="form-grid form-grid--two">
          <label>
            <span>Cultivo</span>
            <input value={payload.cultivo} onChange={(event) => setPayload({ ...payload, cultivo: event.target.value })} required maxLength={120} />
          </label>
          <label>
            <span>Fecha de registro</span>
            <input type="date" value={payload.fechaRegistro} onChange={(event) => setPayload({ ...payload, fechaRegistro: event.target.value })} required />
          </label>
          <label>
            <span>Estado</span>
            <select value={payload.estado} onChange={(event) => setPayload({ ...payload, estado: event.target.value })}>
              <option value="ACTIVO">Activo</option>
              <option value="INACTIVO">Inactivo</option>
              <option value="MANTENIMIENTO">En mantenimiento</option>
              <option value="ARCHIVADO">Archivado</option>
            </select>
          </label>
          <label className="form-grid__full">
            <span>Observación</span>
            <textarea value={payload.observacion || ''} onChange={(event) => setPayload({ ...payload, observacion: event.target.value })} maxLength={255} placeholder="Información adicional para el equipo operativo" />
          </label>
        </div>
      </FormSection>

      <footer className="form-actions form-actions--sticky">
        <button type="button" className="ghost-button" onClick={onCancel} disabled={saving}>Cancelar</button>
        <button type="submit" className="action-button" disabled={saving}>
          {saving ? <Loader2 className="spin" size={16} /> : <Save size={16} />} {saving ? 'Guardando...' : submitLabel}
        </button>
      </footer>
    </form>
  );
}
