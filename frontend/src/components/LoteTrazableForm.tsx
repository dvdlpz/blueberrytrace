import { useMemo, useState } from 'react';
import { CalendarDays, GitBranch, Loader2, MapPin, Save } from 'lucide-react';
import { FormMessage, FormPrerequisite, FormSection } from './FormLayout';
import type { CamaResponse, LoteTrazableFormPayload, ReferenceResponse } from '../types/api';

const today = () => new Date().toISOString().slice(0, 10);
export const blankLoteTrazablePayload: LoteTrazableFormPayload = {
  codigo: '',
  variedad: '',
  procedencia: '',
  fechaIngreso: today(),
  estado: 'ACTIVO',
  observacion: '',
  loteFisicoId: 0,
  camaInicialId: 0
};

interface LoteTrazableFormProps {
  lotes: ReferenceResponse[];
  camas: CamaResponse[];
  initialData?: LoteTrazableFormPayload;
  editing?: boolean;
  onSubmit: (payload: LoteTrazableFormPayload) => Promise<void>;
  onCancel: () => void;
}

export function LoteTrazableForm({ lotes, camas, initialData, editing = false, onSubmit, onCancel }: LoteTrazableFormProps) {
  const [payload, setPayload] = useState<LoteTrazableFormPayload>(initialData || blankLoteTrazablePayload);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const availableCamas = useMemo(() => camas.filter((cama) => cama.lote?.id === payload.loteFisicoId && String(cama.estado || '').toUpperCase() === 'ACTIVA'), [camas, payload.loteFisicoId]);
  const canRegister = lotes.length > 0 && camas.length > 0;

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!payload.loteFisicoId || !payload.camaInicialId) {
      setError('Selecciona un invernadero y una cama activa para continuar.');
      return;
    }
    try {
      setSaving(true);
      setError(null);
      await onSubmit({
        ...payload,
        codigo: payload.codigo.trim().toUpperCase(),
        variedad: payload.variedad.trim(),
        procedencia: payload.procedencia.trim(),
        observacion: payload.observacion?.trim() || undefined
      });
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'No fue posible guardar el lote trazable.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="form-shell" onSubmit={submit}>
      {error ? <FormMessage>{error}</FormMessage> : null}
      {!canRegister ? <FormPrerequisite title="Se requiere una ubicación operativa" description="Antes de crear un lote trazable, registra un invernadero y una cama activa donde se iniciará la operación." /> : null}
      <FormSection title="Identidad y procedencia" description="El código trazable identifica las plantas durante todas las etapas del proceso." icon={<GitBranch size={18} />}>
        <div className="form-grid form-grid--two">
          <label>
            <span>Código trazable</span>
            <input value={payload.codigo} onChange={(event) => setPayload({ ...payload, codigo: event.target.value })} required maxLength={50} disabled={editing || !canRegister} placeholder="Ej.: LT-2026-001" />
            <small className="field-hint">{editing ? 'El código se conserva para mantener la trazabilidad histórica.' : 'Se mantendrá sin cambios después del registro.'}</small>
          </label>
          <label>
            <span>Variedad</span>
            <input value={payload.variedad} onChange={(event) => setPayload({ ...payload, variedad: event.target.value })} required maxLength={120} disabled={!canRegister} placeholder="Ej.: Biloxi" />
          </label>
          <label className="form-grid__full">
            <span>Procedencia u origen</span>
            <input value={payload.procedencia} onChange={(event) => setPayload({ ...payload, procedencia: event.target.value })} required maxLength={180} disabled={!canRegister} placeholder="Describe el origen de las plantas" />
          </label>
        </div>
      </FormSection>

      <FormSection title="Ubicación inicial" description="Asocia el lote trazable al invernadero y cama donde comenzará su recorrido operativo." icon={<MapPin size={18} />}>
        <div className="form-grid form-grid--two">
          <label>
            <span>Invernadero</span>
            <select value={payload.loteFisicoId} onChange={(event) => setPayload({ ...payload, loteFisicoId: Number(event.target.value), camaInicialId: 0 })} required disabled={!canRegister}>
              <option value={0} disabled>Selecciona un invernadero</option>
              {lotes.map((lote) => <option key={lote.id} value={lote.id}>{lote.codigo}</option>)}
            </select>
          </label>
          <label>
            <span>Cama inicial</span>
            <select value={payload.camaInicialId} onChange={(event) => setPayload({ ...payload, camaInicialId: Number(event.target.value) })} required disabled={!payload.loteFisicoId || availableCamas.length === 0}>
              <option value={0} disabled>{payload.loteFisicoId ? 'Selecciona una cama activa' : 'Selecciona primero un invernadero'}</option>
              {availableCamas.map((cama) => <option key={cama.id} value={cama.id}>{cama.codigo}</option>)}
            </select>
            {payload.loteFisicoId && availableCamas.length === 0 ? <small className="field-hint field-hint--warning">Este invernadero no tiene camas activas disponibles.</small> : null}
          </label>
        </div>
      </FormSection>

      <FormSection title="Estado inicial" description="Define cuándo ingresó el lote y agrega una observación útil para su seguimiento." icon={<CalendarDays size={18} />}>
        <div className="form-grid form-grid--two">
          <label>
            <span>Fecha de ingreso</span>
            <input type="date" value={payload.fechaIngreso} onChange={(event) => setPayload({ ...payload, fechaIngreso: event.target.value })} required disabled={!canRegister} />
          </label>
          <label>
            <span>Estado</span>
            <select value={payload.estado} onChange={(event) => setPayload({ ...payload, estado: event.target.value })} disabled={!canRegister}>
              <option value="ACTIVO">Activo</option>
              <option value="CERRADO">Cerrado</option>
              <option value="ARCHIVADO">Archivado</option>
              <option value="ANULADO">Anulado</option>
            </select>
          </label>
          <label className="form-grid__full">
            <span>Observación</span>
            <textarea value={payload.observacion || ''} onChange={(event) => setPayload({ ...payload, observacion: event.target.value })} maxLength={255} disabled={!canRegister} placeholder="Información adicional para el seguimiento del lote" />
          </label>
        </div>
      </FormSection>

      <footer className="form-actions form-actions--sticky">
        <button type="button" className="ghost-button" onClick={onCancel} disabled={saving}>Cancelar</button>
        <button type="submit" className="action-button" disabled={saving || !canRegister || !payload.loteFisicoId || !payload.camaInicialId}>
          {saving ? <Loader2 className="spin" size={16} /> : <Save size={16} />} {saving ? 'Guardando...' : editing ? 'Guardar cambios' : 'Crear lote trazable'}
        </button>
      </footer>
    </form>
  );
}
