import { useMemo, useState } from 'react';
import { ClipboardCheck, Leaf, Loader2, PackageOpen, Save } from 'lucide-react';
import { FormMessage, FormPrerequisite, FormSection } from './FormLayout';
import type { ClasificacionFormPayload, JabaResponse, LoteTrazableResponse } from '../types/api';

interface ClasificacionFormProps {
  lotesTrazables: LoteTrazableResponse[];
  jabas: JabaResponse[];
  initialData?: ClasificacionFormPayload;
  submitLabel?: string;
  onSubmit: (payload: ClasificacionFormPayload) => Promise<void>;
  onCancel: () => void;
}
const today = () => new Date().toISOString().slice(0, 10);

export function ClasificacionForm({ lotesTrazables, jabas, initialData, submitLabel = 'Guardar clasificación', onSubmit, onCancel }: ClasificacionFormProps) {
  const first = lotesTrazables[0];
  const firstJaba = jabas.find((item) => item.cama?.id === first?.camaInicial?.id && item.estado?.toUpperCase() === 'ACTIVA');
  const [payload, setPayload] = useState<ClasificacionFormPayload>(initialData || {
    loteTrazableId: first?.id || 0, loteId: first?.loteFisico?.id || 0, camaId: first?.camaInicial?.id || 0, jabaId: firstJaba?.id || 0,
    fechaClasificacion: today(), estadoPlanta: 'APTA', tamano: 'MEDIANA', condicion: 'HÚMEDA Y VIGOROSA', cantidad: 1, observacion: '', estado: 'PENDIENTE'
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const trace = useMemo(() => lotesTrazables.find((item) => item.id === payload.loteTrazableId) || null, [lotesTrazables, payload.loteTrazableId]);
  const traceJabas = useMemo(() => jabas.filter((item) => item.cama?.id === trace?.camaInicial?.id && item.estado?.toUpperCase() === 'ACTIVA'), [jabas, trace?.camaInicial?.id]);

  function selectTrace(id: number) {
    const selected = lotesTrazables.find((item) => item.id === id);
    const nextJaba = jabas.find((item) => item.cama?.id === selected?.camaInicial?.id && item.estado?.toUpperCase() === 'ACTIVA');
    setPayload((current) => ({ ...current, loteTrazableId: id, loteId: selected?.loteFisico?.id || 0, camaId: selected?.camaInicial?.id || 0, jabaId: nextJaba?.id || 0 }));
  }

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!trace?.loteFisico?.id || !trace.camaInicial?.id || !payload.jabaId) { setError('Selecciona un lote trazable y la jaba revisada durante la clasificación.'); return; }
    try {
      setSaving(true); setError(null);
      await onSubmit({ ...payload, estadoPlanta: payload.estadoPlanta.trim(), tamano: payload.tamano.trim(), condicion: payload.condicion.trim(), observacion: payload.observacion?.trim() || undefined });
    } catch (exception) { setError(exception instanceof Error ? exception.message : 'No fue posible guardar la clasificación.'); }
    finally { setSaving(false); }
  }

  return (
    <form className="form-shell" onSubmit={submit}>
      {error ? <FormMessage>{error}</FormMessage> : null}
      {lotesTrazables.length === 0 ? <FormPrerequisite title="Se requiere un lote trazable" description="Crea un lote trazable y registra las etapas previas antes de clasificar plantas." /> : null}
      {lotesTrazables.length > 0 && traceJabas.length === 0 ? <FormPrerequisite title="Se requiere una jaba activa" description="Registra las jabas de la cama para identificar el origen de las plantas clasificadas." /> : null}
      <FormSection title="Origen de la revisión" description="La jaba permite conservar la ubicación real de las macetas evaluadas." icon={<Leaf size={18} />}>
        <div className="form-grid form-grid--two">
          <label className="form-grid__full"><span>Lote trazable</span><select value={payload.loteTrazableId} onChange={(event) => selectTrace(Number(event.target.value))}><option value={0} disabled>Selecciona un lote trazable</option>{lotesTrazables.map((item) => <option key={item.id} value={item.id}>{item.codigo} · {item.variedad}</option>)}</select></label>
          <label className="form-grid__full"><span>Jaba revisada</span><select value={payload.jabaId} onChange={(event) => setPayload((current) => ({ ...current, jabaId: Number(event.target.value) }))} disabled={traceJabas.length === 0}><option value={0} disabled>Selecciona una jaba</option>{traceJabas.map((item) => <option key={item.id} value={item.id}>{item.codigo} · {item.macetasOcupadas ?? 0} macetas ubicadas</option>)}</select></label>
        </div>
      </FormSection>
      <FormSection title="Evaluación de calidad" description="Las plantas aptas y húmedas podrán prepararse en empaque. Al marcar plantas secas, se abrirá automáticamente su seguimiento de recuperación por riego." icon={<ClipboardCheck size={18} />}>
        <div className="form-grid form-grid--two">
          <label><span>Fecha de clasificación</span><input type="date" value={payload.fechaClasificacion} onChange={(event) => setPayload({ ...payload, fechaClasificacion: event.target.value })} required /></label>
          <label><span>Resultado de planta</span><select value={payload.estadoPlanta} onChange={(event) => setPayload({ ...payload, estadoPlanta: event.target.value })}><option value="APTA">Apta</option><option value="RECUPERACION">Enviar a recuperación por riego</option><option value="NO_APTA">No apta / descarte</option></select></label>
          <label><span>Tamaño</span><select value={payload.tamano} onChange={(event) => setPayload({ ...payload, tamano: event.target.value })}><option value="GRANDE">Grande</option><option value="MEDIANA">Mediana</option><option value="PEQUENA">Pequeña</option></select></label>
          <label><span>Condición</span><select value={payload.condicion} onChange={(event) => setPayload({ ...payload, condicion: event.target.value })}><option value="HÚMEDA Y VIGOROSA">Húmeda y vigorosa</option><option value="SECA">Seca</option><option value="OBSERVADA">Observada</option></select></label>
          <label><span>Cantidad evaluada</span><input type="number" min={1} value={payload.cantidad} onChange={(event) => setPayload({ ...payload, cantidad: Number(event.target.value) })} required /></label>
          <label><span>Estado del registro</span><input value={initialData?.estado === 'VALIDADA' ? 'Validada' : initialData?.estado === 'OBSERVADA' ? 'Observada' : 'Pendiente de validación'} readOnly /></label>
          <label className="form-grid__full"><span>Observación</span><textarea value={payload.observacion || ''} onChange={(event) => setPayload({ ...payload, observacion: event.target.value })} maxLength={255} placeholder="Detalle de humedad, vigor o seguimiento requerido" /></label>
        </div>
      </FormSection>
      <footer className="form-actions form-actions--sticky"><button type="button" className="ghost-button" onClick={onCancel} disabled={saving}>Cancelar</button><button type="submit" className="action-button" disabled={saving || !trace || !payload.jabaId}>{saving ? <Loader2 className="spin" size={16} /> : <Save size={16} />}{saving ? 'Guardando...' : submitLabel}</button></footer>
    </form>
  );
}
