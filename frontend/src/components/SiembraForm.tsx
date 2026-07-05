import { useMemo, useState } from 'react';
import { CalendarDays, Loader2, MapPin, PackageOpen, Save, Sprout } from 'lucide-react';
import { FormMessage, FormPrerequisite, FormSection } from './FormLayout';
import type { JabaResponse, LoteTrazableResponse, SiembraFormPayload } from '../types/api';

interface SiembraFormProps {
  lotesTrazables: LoteTrazableResponse[];
  jabas: JabaResponse[];
  initialData?: SiembraFormPayload;
  submitLabel?: string;
  onSubmit: (payload: SiembraFormPayload) => Promise<void>;
  onCancel: () => void;
}

const today = () => new Date().toISOString().slice(0, 10);

function buildInitial(traces: LoteTrazableResponse[], jabas: JabaResponse[], data?: SiembraFormPayload): SiembraFormPayload {
  if (data) return data;
  const trace = traces[0];
  const firstJaba = jabas.find((item) => item.cama?.id === trace?.camaInicial?.id && item.estado?.toUpperCase() === 'ACTIVA');
  return {
    loteTrazableId: trace?.id || 0,
    loteId: trace?.loteFisico?.id || 0,
    camaId: trace?.camaInicial?.id || 0,
    jabaId: firstJaba?.id || 0,
    fechaSiembra: today(),
    cantidadRegistrada: 1,
    observacion: '',
    estado: 'REGISTRADA'
  };
}

export function SiembraForm({ lotesTrazables, jabas, initialData, submitLabel = 'Registrar siembra', onSubmit, onCancel }: SiembraFormProps) {
  const [payload, setPayload] = useState<SiembraFormPayload>(() => buildInitial(lotesTrazables, jabas, initialData));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const trace = useMemo(() => lotesTrazables.find((item) => item.id === payload.loteTrazableId) || null, [lotesTrazables, payload.loteTrazableId]);
  const availableJabas = useMemo(() => jabas.filter((item) => item.cama?.id === trace?.camaInicial?.id && item.estado?.toUpperCase() === 'ACTIVA'), [jabas, trace?.camaInicial?.id]);
  const selectedJaba = availableJabas.find((item) => item.id === payload.jabaId) || null;
  const availableSpace = selectedJaba?.macetasDisponibles ?? selectedJaba?.capacidadMacetas ?? 0;
  const canRegister = Boolean(trace && availableJabas.length > 0 && availableSpace > 0);

  function selectTrace(id: number) {
    const selected = lotesTrazables.find((item) => item.id === id);
    const firstJaba = jabas.find((item) => item.cama?.id === selected?.camaInicial?.id && item.estado?.toUpperCase() === 'ACTIVA');
    setPayload((current) => ({
      ...current,
      loteTrazableId: id,
      loteId: selected?.loteFisico?.id || 0,
      camaId: selected?.camaInicial?.id || 0,
      jabaId: firstJaba?.id || 0
    }));
  }

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!trace?.loteFisico?.id || !trace.camaInicial?.id || !selectedJaba) {
      setError('Selecciona un lote trazable y una jaba activa para ubicar las macetas sembradas.');
      return;
    }
    if (payload.cantidadRegistrada > availableSpace) {
      setError(`La cantidad supera el espacio disponible de la jaba seleccionada (${availableSpace} macetas).`);
      return;
    }
    try {
      setSaving(true);
      setError(null);
      await onSubmit({ ...payload, observacion: payload.observacion?.trim() || undefined });
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'No fue posible registrar la siembra.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="form-shell" onSubmit={submit}>
      {error ? <FormMessage>{error}</FormMessage> : null}
      {lotesTrazables.length === 0 ? <FormPrerequisite title="Se requiere un lote trazable" description="Antes de sembrar, registra la variedad y procedencia del grupo de plantas." /> : null}
      {lotesTrazables.length > 0 && availableJabas.length === 0 ? <FormPrerequisite title="Se requiere una jaba activa" description="Registra una jaba de siembra dentro de la cama inicial antes de ubicar macetas." /> : null}

      <FormSection title="Ubicación de la siembra" description="La cama contiene jabas y cada jaba organiza las macetas de este grupo de plantas." icon={<Sprout size={18} />}>
        <div className="form-grid form-grid--two">
          <label className="form-grid__full">
            <span>Lote trazable</span>
            <select value={payload.loteTrazableId} onChange={(event) => selectTrace(Number(event.target.value))} required>
              <option value={0} disabled>Selecciona un lote trazable</option>
              {lotesTrazables.map((item) => <option key={item.id} value={item.id}>{item.codigo} · {item.variedad} · {item.procedencia}</option>)}
            </select>
          </label>
          <label className="form-grid__full">
            <span>Jaba de siembra</span>
            <select value={payload.jabaId} onChange={(event) => setPayload((current) => ({ ...current, jabaId: Number(event.target.value) }))} required disabled={availableJabas.length === 0}>
              <option value={0} disabled>Selecciona una jaba activa</option>
              {availableJabas.map((item) => <option key={item.id} value={item.id}>{item.codigo} · {item.macetasDisponibles ?? item.capacidadMacetas} espacios disponibles</option>)}
            </select>
            <small className="field-hint">La cantidad se valida contra la capacidad de esta jaba.</small>
          </label>
        </div>
        <div className="operation-context-grid">
          <div><MapPin size={16} /><span>Invernadero</span><strong>{trace?.loteFisico?.codigo || 'Pendiente'}</strong></div>
          <div><MapPin size={16} /><span>Cama</span><strong>{trace?.camaInicial?.codigo || 'Pendiente'}</strong></div>
          <div><PackageOpen size={16} /><span>Capacidad de jaba</span><strong>{selectedJaba ? `${availableSpace} disponibles de ${selectedJaba.capacidadMacetas}` : 'Pendiente'}</strong></div>
        </div>
      </FormSection>

      <FormSection title="Registro de macetas" description="Registra las macetas preparadas con tierra fértil dentro de la jaba seleccionada." icon={<CalendarDays size={18} />}>
        <div className="form-grid form-grid--two">
          <label><span>Fecha de siembra</span><input type="date" value={payload.fechaSiembra} onChange={(event) => setPayload({ ...payload, fechaSiembra: event.target.value })} required /></label>
          <label><span>Cantidad de macetas</span><input type="number" min={1} max={availableSpace || undefined} value={payload.cantidadRegistrada} onChange={(event) => setPayload({ ...payload, cantidadRegistrada: Number(event.target.value) })} required /></label>
          <label className="form-grid__full"><span>Observación</span><textarea value={payload.observacion || ''} onChange={(event) => setPayload({ ...payload, observacion: event.target.value })} maxLength={255} placeholder="Ejemplo: tierra preparada y riego programado inicial" /></label>
        </div>
      </FormSection>

      <footer className="form-actions form-actions--sticky">
        <button type="button" className="ghost-button" onClick={onCancel} disabled={saving}>Cancelar</button>
        <button type="submit" className="action-button" disabled={saving || !canRegister}>{saving ? <Loader2 className="spin" size={16} /> : <Save size={16} />}{saving ? 'Guardando...' : submitLabel}</button>
      </footer>
    </form>
  );
}
