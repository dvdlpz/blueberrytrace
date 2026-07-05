import { useState } from 'react';
import { Loader2, MinusCircle, Save } from 'lucide-react';
import { FormMessage, FormPrerequisite, FormSection } from './FormLayout';
import type { LoteTrazableResponse, MermaFormPayload } from '../types/api';

interface MermaFormProps {
  lotesTrazables: LoteTrazableResponse[];
  initialData?: MermaFormPayload;
  onSubmit: (payload: MermaFormPayload) => Promise<void>;
  onCancel: () => void;
}

const today = () => new Date().toISOString().slice(0, 10);
const stages = [
  { value: 'SIEMBRA', label: 'Siembra' },
  { value: 'UNIFORMIZACION', label: 'Uniformización' },
  { value: 'FORMALIZACION', label: 'Formalización' },
  { value: 'CLASIFICACION', label: 'Clasificación' },
  { value: 'DESPACHO', label: 'Despacho' }
];

export function MermaForm({ lotesTrazables, initialData, onSubmit, onCancel }: MermaFormProps) {
  const [payload, setPayload] = useState<MermaFormPayload>(initialData || {
    loteTrazableId: lotesTrazables[0]?.id || 0,
    etapaOrigen: stages[0].value,
    motivo: '',
    cantidad: 1,
    fechaMerma: today(),
    observacion: ''
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const canRegister = lotesTrazables.length > 0;

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!payload.loteTrazableId) {
      setError('Selecciona un lote trazable para registrar la merma.');
      return;
    }
    try {
      setSaving(true);
      setError(null);
      await onSubmit({ ...payload, motivo: payload.motivo.trim(), observacion: payload.observacion?.trim() || undefined });
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'No fue posible registrar la merma.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="form-shell" onSubmit={submit}>
      {error ? <FormMessage>{error}</FormMessage> : null}
      {!canRegister ? <FormPrerequisite title="Se requiere un lote trazable" description="Registra un lote trazable antes de ingresar una merma o ajuste de disponibilidad." /> : null}
      <FormSection title="Registro de merma" description="Las mermas reducen el saldo disponible y quedan registradas para el seguimiento operativo." icon={<MinusCircle size={18} />}>
        <div className="form-grid form-grid--two">
          <label className="form-grid__full">
            <span>Lote trazable</span>
            <select value={payload.loteTrazableId} onChange={(event) => setPayload({ ...payload, loteTrazableId: Number(event.target.value) })} required disabled={!canRegister}>
              <option value={0} disabled>Selecciona un lote trazable</option>
              {lotesTrazables.map((item) => <option key={item.id} value={item.id}>{item.codigo} · {item.variedad}</option>)}
            </select>
          </label>
          <label>
            <span>Etapa de origen</span>
            <select value={payload.etapaOrigen} onChange={(event) => setPayload({ ...payload, etapaOrigen: event.target.value })} disabled={!canRegister}>
              {stages.map((stage) => <option key={stage.value} value={stage.value}>{stage.label}</option>)}
            </select>
          </label>
          <label>
            <span>Cantidad</span>
            <input type="number" min={1} value={payload.cantidad} onChange={(event) => setPayload({ ...payload, cantidad: Number(event.target.value) })} required disabled={!canRegister} />
          </label>
          <label>
            <span>Fecha de registro</span>
            <input type="date" value={payload.fechaMerma} onChange={(event) => setPayload({ ...payload, fechaMerma: event.target.value })} required disabled={!canRegister} />
          </label>
          <label className="form-grid__full">
            <span>Motivo</span>
            <input value={payload.motivo} onChange={(event) => setPayload({ ...payload, motivo: event.target.value })} required maxLength={120} disabled={!canRegister} placeholder="Describe el motivo de la merma" />
          </label>
          <label className="form-grid__full">
            <span>Observación</span>
            <textarea value={payload.observacion || ''} onChange={(event) => setPayload({ ...payload, observacion: event.target.value })} maxLength={255} disabled={!canRegister} placeholder="Información adicional que respalde el registro" />
          </label>
        </div>
      </FormSection>
      <footer className="form-actions form-actions--sticky">
        <button type="button" className="ghost-button" onClick={onCancel} disabled={saving}>Cancelar</button>
        <button type="submit" className="action-button" disabled={saving || !canRegister}>
          {saving ? <Loader2 className="spin" size={16} /> : <Save size={16} />} {saving ? 'Guardando...' : 'Registrar merma'}
        </button>
      </footer>
    </form>
  );
}
