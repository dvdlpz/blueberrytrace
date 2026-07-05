import { useMemo, useState } from 'react';
import { ArrowDown, ArrowLeftRight, ArrowUp, ClipboardCheck, Leaf, Loader2, PackageOpen, Save } from 'lucide-react';
import { FormMessage, FormPrerequisite, FormSection } from './FormLayout';
import type { FormalizacionFormPayload, FormalizacionResponse, JabaResponse, LoteTrazableResponse, UniformizacionFormPayload, UniformizacionResponse } from '../types/api';

type ProcessMode = 'uniformizacion' | 'formalizacion';

interface ProcesoFormProps {
  mode: ProcessMode;
  lotesTrazables: LoteTrazableResponse[];
  jabas: JabaResponse[];
  initialData?: UniformizacionResponse | FormalizacionResponse;
  submitLabel?: string;
  onSubmit: (payload: UniformizacionFormPayload | FormalizacionFormPayload) => Promise<void>;
  onCancel: () => void;
}

const today = () => new Date().toISOString().slice(0, 10);
function isUniform(data?: UniformizacionResponse | FormalizacionResponse): data is UniformizacionResponse { return Boolean(data && 'fechaUniformizacion' in data); }

export function ProcesoForm({ mode, lotesTrazables, jabas, initialData, submitLabel = 'Guardar registro', onSubmit, onCancel }: ProcesoFormProps) {
  const uniform = mode === 'uniformizacion';
  const [traceId, setTraceId] = useState(initialData?.loteTrazable?.id || lotesTrazables[0]?.id || 0);
  const [fecha, setFecha] = useState(isUniform(initialData) ? initialData.fechaUniformizacion || today() : initialData && 'fechaFormalizacion' in initialData ? initialData.fechaFormalizacion || today() : today());
  const [criterio, setCriterio] = useState(isUniform(initialData) ? initialData.criterio || 'TAMAÑO_SIMILAR' : '');
  const [detalle, setDetalle] = useState(initialData && 'detalle' in initialData ? initialData.detalle || '' : '');
  const [cantidadInicial, setCantidadInicial] = useState(isUniform(initialData) ? initialData.cantidadInicial || 1 : 1);
  const [cantidadUniformizada, setCantidadUniformizada] = useState(isUniform(initialData) ? initialData.cantidadUniformizada || 1 : 1);
  const [cantidadBandejas, setCantidadBandejas] = useState(initialData && 'cantidadBandejas' in initialData ? initialData.cantidadBandejas || 1 : 1);
  const [jabaIds, setJabaIds] = useState<number[]>(!isUniform(initialData) && initialData && 'jabasMovidas' in initialData ? initialData.jabasMovidas.map((item) => item.id) : []);
  const [cantidadPlantas, setCantidadPlantas] = useState(initialData && 'cantidadPlantas' in initialData ? initialData.cantidadPlantas || 1 : 1);
  const [jabaOrigenId, setJabaOrigenId] = useState(isUniform(initialData) ? initialData.jabaOrigen?.id || 0 : 0);
  const [jabaDestinoId, setJabaDestinoId] = useState(isUniform(initialData) ? initialData.jabaDestino?.id || 0 : 0);
  const [origenOperativo, setOrigenOperativo] = useState(isUniform(initialData) ? initialData.origenOperativo || 'SIEMBRA' : 'SIEMBRA');
  const [cantidadRecuperacion, setCantidadRecuperacion] = useState(isUniform(initialData) ? initialData.cantidadRecuperacion || 0 : 0);
  const [malezasRetiradas, setMalezasRetiradas] = useState(isUniform(initialData) ? Boolean(initialData.malezasRetiradas) : false);
  const [ordenamiento, setOrdenamiento] = useState(initialData && 'ordenamientoJabas' in initialData ? initialData.ordenamientoJabas || 'MAYOR_A_MENOR' : 'MAYOR_A_MENOR');
  const [observacion, setObservacion] = useState(initialData?.observacion || '');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const trace = useMemo(() => lotesTrazables.find((item) => item.id === traceId) || null, [lotesTrazables, traceId]);
  const traceJabas = useMemo(() => jabas
    .filter((item) => item.cama?.id === trace?.camaInicial?.id && item.estado?.toUpperCase() === 'ACTIVA')
    .sort((left, right) => (left.ordenEnCama || 0) - (right.ordenEnCama || 0)), [jabas, trace?.camaInicial?.id]);
  const orderedFormalizationJabas = useMemo(() => jabaIds
    .map((id) => traceJabas.find((item) => item.id === id))
    .filter((item): item is JabaResponse => Boolean(item)), [jabaIds, traceJabas]);

  function moveFormalizationJaba(index: number, direction: -1 | 1) {
    setJabaIds((current) => {
      const target = index + direction;
      if (target < 0 || target >= current.length) return current;
      const next = [...current];
      [next[index], next[target]] = [next[target], next[index]];
      return next;
    });
  }

  function switchTrace(id: number) {
    const next = lotesTrazables.find((item) => item.id === id);
    const first = jabas.find((item) => item.cama?.id === next?.camaInicial?.id && item.estado?.toUpperCase() === 'ACTIVA');
    setTraceId(id);
    setJabaOrigenId(first?.id || 0);
    setJabaDestinoId(first?.id || 0);
    setJabaIds([]);
    if (!uniform) setCantidadBandejas(1);
  }

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!trace?.loteFisico?.id || !trace.camaInicial?.id) { setError('Selecciona un lote trazable con cama inicial asignada.'); return; }
    if (uniform && (!jabaOrigenId || !jabaDestinoId)) { setError('Selecciona la jaba de origen y la jaba de destino para mover las macetas.'); return; }
    if (uniform && jabaOrigenId === jabaDestinoId) { setError('Selecciona una jaba de destino distinta para completar el movimiento de macetas.'); return; }
    if (!uniform && jabaIds.length === 0) { setError('Selecciona las jabas completas que fueron organizadas.'); return; }
    if (!uniform && cantidadBandejas !== jabaIds.length) { setError('La cantidad de jabas organizadas debe coincidir con las jabas seleccionadas.'); return; }
    try {
      setSaving(true); setError(null);
      if (uniform) {
        await onSubmit({ loteTrazableId: traceId, loteId: trace.loteFisico.id, camaId: trace.camaInicial.id, jabaOrigenId, jabaDestinoId, fechaUniformizacion: fecha, criterio: criterio.trim(), cantidadInicial, cantidadUniformizada, origenOperativo, cantidadRecuperacion, malezasRetiradas, observacion: observacion.trim() || undefined, estado: initialData?.estado || 'REGISTRADA' });
      } else {
        await onSubmit({ loteTrazableId: traceId, loteId: trace.loteFisico.id, camaId: trace.camaInicial.id, fechaFormalizacion: fecha, detalle: detalle.trim(), cantidadBandejas, jabaIds, cantidadPlantas, ordenamientoJabas: ordenamiento, observacion: observacion.trim() || undefined, estado: initialData?.estado || 'REGISTRADA' });
      }
    } catch (exception) { setError(exception instanceof Error ? exception.message : 'No fue posible guardar el proceso.'); }
    finally { setSaving(false); }
  }

  return (
    <form className="form-shell" onSubmit={submit}>
      {error ? <FormMessage>{error}</FormMessage> : null}
      {lotesTrazables.length === 0 ? <FormPrerequisite title="Se requiere un lote trazable" description="Registra el grupo de plantas antes de realizar movimientos operativos." /> : null}
      {uniform && lotesTrazables.length > 0 && traceJabas.length === 0 ? <FormPrerequisite title="Se requieren jabas activas" description="Registra jabas dentro de la cama para mover macetas por tamaño." /> : null}
      <FormSection title="Origen operativo" description="Selecciona el lote trazable y la cama donde se encuentra el grupo de plantas." icon={uniform ? <Leaf size={18} /> : <ClipboardCheck size={18} />}>
        <div className="form-grid form-grid--two">
          <label className="form-grid__full"><span>Lote trazable</span><select value={traceId} onChange={(event) => switchTrace(Number(event.target.value))}><option value={0} disabled>Selecciona un lote trazable</option>{lotesTrazables.map((item) => <option key={item.id} value={item.id}>{item.codigo} · {item.variedad}</option>)}</select></label>
          <label><span>Fecha</span><input type="date" value={fecha} onChange={(event) => setFecha(event.target.value)} required /></label>
          {uniform ? <label><span>Origen del movimiento</span><select value={origenOperativo} onChange={(event) => setOrigenOperativo(event.target.value)}><option value="SIEMBRA">Siembra o crecimiento</option><option value="FORMALIZACION">Retorno desde formalización</option><option value="RECUPERACION">Retorno desde recuperación por riego</option></select></label> : null}
        </div>
      </FormSection>
      {uniform ? (
        <FormSection title="Uniformización de macetas" description="Mueve macetas individuales para agrupar plantas de tamaño similar y registrar recuperación o limpieza." icon={<ArrowLeftRight size={18} />}>
          <div className="form-grid form-grid--two">
            <label><span>Jaba de origen</span><select value={jabaOrigenId} onChange={(event) => setJabaOrigenId(Number(event.target.value))} required><option value={0} disabled>Selecciona una jaba</option>{traceJabas.map((item) => <option key={item.id} value={item.id}>{item.codigo} · {item.macetasOcupadas ?? 0} ubicadas · {item.macetasDisponibles ?? item.capacidadMacetas} libres</option>)}</select></label>
            <label><span>Jaba de destino</span><select value={jabaDestinoId} onChange={(event) => setJabaDestinoId(Number(event.target.value))} required><option value={0} disabled>Selecciona una jaba</option>{traceJabas.map((item) => <option key={item.id} value={item.id}>{item.codigo} · {item.macetasOcupadas ?? 0} ubicadas · {item.macetasDisponibles ?? item.capacidadMacetas} libres</option>)}</select></label>
            <label><span>Criterio de agrupación</span><select value={criterio} onChange={(event) => setCriterio(event.target.value)}><option value="TAMAÑO_SIMILAR">Plantas de tamaño similar</option><option value="VIGOR">Vigor de planta</option><option value="ESTADO_HIDRICO">Estado hídrico</option></select></label>
            <label><span>Macetas revisadas</span><input type="number" min={1} value={cantidadInicial} onChange={(event) => setCantidadInicial(Number(event.target.value))} required /></label>
            <label><span>Macetas uniformizadas</span><input type="number" min={1} value={cantidadUniformizada} onChange={(event) => setCantidadUniformizada(Number(event.target.value))} required /></label>
            <label><span>Plantas para recuperación por riego</span><input type="number" min={0} value={cantidadRecuperacion} onChange={(event) => setCantidadRecuperacion(Number(event.target.value))} required /></label>
            <label className="form-grid__full checkbox-field"><input type="checkbox" checked={malezasRetiradas} onChange={(event) => setMalezasRetiradas(event.target.checked)} /><span>Se realizó retiro de malezas durante la revisión</span></label>
          </div>
        </FormSection>
      ) : (
        <FormSection title="Formalización de jabas" description="Organiza jabas completas según el tamaño de las plantas; puede retornarse a uniformización si se requiere reordenar." icon={<PackageOpen size={18} />}>
          <div className="form-grid form-grid--two">
            <label><span>Ordenamiento de jabas</span><select value={ordenamiento} onChange={(event) => setOrdenamiento(event.target.value)}><option value="MAYOR_A_MENOR">Grandes → medianas → pequeñas</option><option value="MENOR_A_MAYOR">Pequeñas → medianas → grandes</option></select></label>
            <label><span>Jabas organizadas</span><input type="number" min={1} value={cantidadBandejas} readOnly aria-describedby="jabas-organizadas-ayuda" required /><small id="jabas-organizadas-ayuda">Se actualiza según las jabas seleccionadas.</small></label>
            <label><span>Plantas incluidas</span><input type="number" min={1} value={cantidadPlantas} onChange={(event) => setCantidadPlantas(Number(event.target.value))} required /></label>
            <fieldset className="form-grid__full checkbox-group"><legend>Jabas completas organizadas</legend><p>Selecciona las jabas que se movieron como unidades completas durante la formalización.</p><div className="checkbox-group__options">{traceJabas.map((item) => <label key={item.id} className="checkbox-field"><input type="checkbox" checked={jabaIds.includes(item.id)} onChange={(event) => setJabaIds((current) => { const next = event.target.checked ? [...current, item.id] : current.filter((id) => id !== item.id); setCantidadBandejas(next.length || 1); return next; })} /><span>Posición {item.ordenEnCama ?? '—'} · {item.codigo} · {item.macetasOcupadas ?? 0} macetas ubicadas</span></label>)}</div></fieldset>
            {orderedFormalizationJabas.length > 0 ? <section className="form-grid__full ordered-jabas" aria-label="Orden final de las jabas formalizadas"><div><strong>Orden final dentro de la cama</strong><p>La primera jaba quedará antes que las demás seleccionadas. Usa las flechas para reflejar el orden físico real, por ejemplo grandes → medianas → pequeñas.</p></div><ol className="ordered-jabas__list">{orderedFormalizationJabas.map((item, index) => <li key={item.id}><span><b>{index + 1}</b>{item.codigo}<small>Posición actual {item.ordenEnCama ?? '—'} · {item.macetasOcupadas ?? 0} macetas</small></span><div className="ordered-jabas__actions"><button type="button" className="icon-action" aria-label={`Subir ${item.codigo}`} disabled={index === 0} onClick={() => moveFormalizationJaba(index, -1)}><ArrowUp size={15} /></button><button type="button" className="icon-action" aria-label={`Bajar ${item.codigo}`} disabled={index === orderedFormalizationJabas.length - 1} onClick={() => moveFormalizationJaba(index, 1)}><ArrowDown size={15} /></button></div></li>)}</ol></section> : null}
            <label className="form-grid__full"><span>Detalle de organización</span><input value={detalle} onChange={(event) => setDetalle(event.target.value)} placeholder="Ejemplo: jabas grandes al inicio de la cama" required /></label>
          </div>
        </FormSection>
      )}
      <FormSection title="Observación" description="Registra información relevante para el siguiente responsable." icon={<ClipboardCheck size={18} />}><div className="form-grid"><label><span>Observación</span><textarea value={observacion} onChange={(event) => setObservacion(event.target.value)} maxLength={255} placeholder="Detalle del movimiento realizado" /></label></div></FormSection>
      <footer className="form-actions form-actions--sticky"><button type="button" className="ghost-button" onClick={onCancel} disabled={saving}>Cancelar</button><button type="submit" className="action-button" disabled={saving || !trace || (uniform && traceJabas.length === 0)}>{saving ? <Loader2 className="spin" size={16} /> : <Save size={16} />}{saving ? 'Guardando...' : submitLabel}</button></footer>
    </form>
  );
}
