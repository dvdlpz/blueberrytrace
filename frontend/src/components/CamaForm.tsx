import { useState } from 'react';
import { Layers3, Loader2, Save, SlidersHorizontal } from 'lucide-react';
import { FormMessage, FormPrerequisite, FormSection } from './FormLayout';
import type { CamaFormPayload, ReferenceResponse } from '../types/api';

interface CamaFormProps {
  lotes: ReferenceResponse[];
  initialData?: CamaFormPayload;
  submitLabel?: string;
  onSubmit: (payload: CamaFormPayload) => Promise<void>;
  onCancel: () => void;
}

export function CamaForm({ lotes, initialData, submitLabel = 'Guardar cama', onSubmit, onCancel }: CamaFormProps) {
  const [payload, setPayload] = useState<CamaFormPayload>(initialData || {
    codigo: '',
    descripcion: '',
    capacidadReferencial: 1,
    estado: 'ACTIVA',
    loteId: lotes[0]?.id || 0
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const canRegister = lotes.length > 0;

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!canRegister) {
      setError('Primero registra un invernadero activo para asociar la cama.');
      return;
    }
    try {
      setSaving(true);
      setError(null);
      await onSubmit({ ...payload, codigo: payload.codigo.trim().toUpperCase(), descripcion: payload.descripcion.trim() });
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'No fue posible guardar la cama.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="form-shell" onSubmit={submit}>
      {error ? <FormMessage>{error}</FormMessage> : null}
      {!canRegister ? <FormPrerequisite title="Se requiere un invernadero" description="Registra primero un invernadero activo. Luego podrás organizar sus camas productivas." /> : null}
      <FormSection title="Ubicación de la cama" description="Identifica la cama y vincúlala al invernadero donde operará." icon={<Layers3 size={18} />}>
        <div className="form-grid form-grid--two">
          <label>
            <span>Código de cama</span>
            <input value={payload.codigo} onChange={(event) => setPayload({ ...payload, codigo: event.target.value })} required maxLength={30} placeholder="Ej.: CMA-001" disabled={!canRegister} />
            <small className="field-hint">Debe ser único dentro de la operación.</small>
          </label>
          <label>
            <span>Invernadero</span>
            <select value={payload.loteId} onChange={(event) => setPayload({ ...payload, loteId: Number(event.target.value) })} required disabled={!canRegister}>
              <option value={0} disabled>Selecciona un invernadero</option>
              {lotes.map((lote) => <option key={lote.id} value={lote.id}>{lote.codigo}</option>)}
            </select>
          </label>
          <label className="form-grid__full">
            <span>Descripción</span>
            <input value={payload.descripcion} onChange={(event) => setPayload({ ...payload, descripcion: event.target.value })} required maxLength={150} placeholder="Referencia de ubicación dentro del invernadero" disabled={!canRegister} />
          </label>
        </div>
      </FormSection>

      <FormSection title="Capacidad y disponibilidad" description="La capacidad referencial ayuda a controlar la ocupación durante la siembra." icon={<SlidersHorizontal size={18} />}>
        <div className="form-grid form-grid--two">
          <label>
            <span>Capacidad referencial</span>
            <input type="number" min={1} value={payload.capacidadReferencial} onChange={(event) => setPayload({ ...payload, capacidadReferencial: Number(event.target.value) })} required disabled={!canRegister} />
            <small className="field-hint">Indica la cantidad máxima estimada de plantas.</small>
          </label>
          <label>
            <span>Estado</span>
            <select value={payload.estado} onChange={(event) => setPayload({ ...payload, estado: event.target.value })} disabled={!canRegister}>
              <option value="ACTIVA">Activa</option>
              <option value="INACTIVA">Inactiva</option>
              <option value="MANTENIMIENTO">En mantenimiento</option>
              <option value="ARCHIVADA">Archivada</option>
            </select>
          </label>
        </div>
      </FormSection>

      <footer className="form-actions form-actions--sticky">
        <button type="button" className="ghost-button" onClick={onCancel} disabled={saving}>Cancelar</button>
        <button type="submit" className="action-button" disabled={saving || !canRegister || payload.loteId === 0}>
          {saving ? <Loader2 className="spin" size={16} /> : <Save size={16} />} {saving ? 'Guardando...' : submitLabel}
        </button>
      </footer>
    </form>
  );
}
