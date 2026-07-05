import { useMemo, useState } from 'react';
import { CalendarClock, CheckCircle2, Clock3, Droplets, Plus, Save, XCircle } from 'lucide-react';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { EmptyState } from '../components/EmptyState';
import { FormMessage, FormSection } from '../components/FormLayout';
import { Modal } from '../components/Modal';
import { ModuleHeader } from '../components/ModuleHeader';
import { StatusBadge } from '../components/StatusBadge';
import { blueberryApi } from '../lib/api';
import { dateShort } from '../lib/format';
import { emitToast } from '../lib/uiEvents';
import type { JabaResponse, LoteTrazableResponse, RiegoProgramadoFormPayload, RiegoProgramadoResponse, RiegoRealizadoPayload } from '../types/api';

interface RiegosPageProps {
  riegos: RiegoProgramadoResponse[];
  lotesTrazables: LoteTrazableResponse[];
  jabas: JabaResponse[];
  onChanged: (items: RiegoProgramadoResponse[]) => void;
}

const today = () => new Date().toISOString().slice(0, 10);
const nowTime = () => new Date().toTimeString().slice(0, 5);

export function RiegosPage({ riegos, lotesTrazables, jabas, onChanged }: RiegosPageProps) {
  const [createOpen, setCreateOpen] = useState(false);
  const [completeTarget, setCompleteTarget] = useState<RiegoProgramadoResponse | null>(null);
  const [cancelTarget, setCancelTarget] = useState<RiegoProgramadoResponse | null>(null);
  const [saving, setSaving] = useState(false);

  const programmed = riegos.filter((item) => item.estado === 'PROGRAMADO');
  const completed = riegos.filter((item) => item.estado === 'REALIZADO');

  async function create(payload: RiegoProgramadoFormPayload) {
    setSaving(true);
    try {
      const response = await blueberryApi.createRiegoProgramado(payload);
      onChanged(response.items);
      setCreateOpen(false);
      emitToast('success', 'Riego programado', 'El riego quedó registrado para la cama o jaba seleccionada.');
    } finally {
      setSaving(false);
    }
  }

  async function complete(payload: RiegoRealizadoPayload) {
    if (!completeTarget) return;
    setSaving(true);
    try {
      const response = await blueberryApi.completeRiegoProgramado(completeTarget.id, payload);
      onChanged(response.items);
      setCompleteTarget(null);
      emitToast('success', 'Riego realizado', 'La ejecución del riego quedó registrada.');
    } finally {
      setSaving(false);
    }
  }

  async function cancel() {
    if (!cancelTarget) return;
    setSaving(true);
    try {
      const response = await blueberryApi.cancelRiegoProgramado(cancelTarget.id);
      onChanged(response.items);
      setCancelTarget(null);
      emitToast('success', 'Riego cancelado', 'El programa de riego fue cancelado sin eliminar su historial.');
    } catch (exception) {
      emitToast('error', 'No se pudo cancelar', exception instanceof Error ? exception.message : 'Inténtalo nuevamente.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="content-grid">
      <ModuleHeader
        eyebrow="Manejo de agua"
        title="Riegos programados"
        description="Organiza y confirma los riegos de crecimiento o recuperación por cama y jaba."
        icon={<Droplets size={21} />}
        tone="blue"
        actions={<button type="button" className="action-button" onClick={() => setCreateOpen(true)}><Plus size={16} /> Programar riego</button>}
      />

      <section className="summary-strip summary-strip--three">
        <article className="summary-pill summary-pill--blue"><span className="summary-pill__icon"><CalendarClock size={18} /></span><span>Programados</span><strong>{programmed.length}</strong><small>riegos</small></article>
        <article className="summary-pill summary-pill--green"><span className="summary-pill__icon"><CheckCircle2 size={18} /></span><span>Realizados</span><strong>{completed.length}</strong><small>riegos</small></article>
        <article className="summary-pill summary-pill--orange"><span className="summary-pill__icon"><Clock3 size={18} /></span><span>Para recuperación</span><strong>{programmed.filter((item) => item.etapaAplicacion === 'RECUPERACION').length}</strong><small>programados</small></article>
      </section>

      <section className="panel-card">
        <div className="panel-card__header">
          <div><h2>Calendario de riegos</h2><p>El riego puede aplicarse a una cama completa o concentrarse en una jaba con plantas en recuperación.</p></div>
          <span className="panel-card__count">{riegos.length} registros</span>
        </div>
        {riegos.length > 0 ? (
          <div className="data-table-wrap">
            <table className="data-table">
              <thead><tr><th>Programación</th><th>Lote trazable</th><th>Ubicación</th><th>Aplicación</th><th>Estado</th><th /></tr></thead>
              <tbody>
                {riegos.map((item) => (
                  <tr key={item.id}>
                    <td><strong>{dateShort(item.fechaProgramada)}</strong><small>{item.horaProgramada?.slice(0, 5) || 'Sin hora'}</small></td>
                    <td>{item.loteTrazable?.codigo || 'Sin lote'}</td>
                    <td>{item.cama?.codigo || 'Sin cama'}<small>{item.jaba?.codigo ? `Jaba: ${item.jaba.codigo}` : 'Aplica a toda la cama'}</small></td>
                    <td>{item.etapaAplicacion === 'RECUPERACION' ? 'Recuperación por riego' : 'Crecimiento'}</td>
                    <td><StatusBadge value={item.estado} /></td>
                    <td>
                      {item.estado === 'PROGRAMADO' ? <div className="button-group"><button type="button" className="ghost-button ghost-button--small" onClick={() => setCompleteTarget(item)}>Registrar realizado</button><button type="button" className="ghost-button ghost-button--small" onClick={() => setCancelTarget(item)}>Cancelar</button></div> : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState icon={<Droplets size={28} />} title="No hay riegos programados" description="Programa el primer riego de crecimiento o recuperación para conservar el seguimiento operativo." action={<button type="button" className="action-button" onClick={() => setCreateOpen(true)}><Plus size={15} /> Programar riego</button>} />
        )}
      </section>

      <Modal open={createOpen} title="Programar riego" description="Selecciona el lote trazable, la cama y, de ser necesario, una jaba específica." onClose={() => setCreateOpen(false)}>
        <RiegoProgramadoForm traces={lotesTrazables} jabas={jabas} saving={saving} onSubmit={create} onCancel={() => setCreateOpen(false)} />
      </Modal>
      <Modal open={Boolean(completeTarget)} title="Registrar riego realizado" description="Indica la fecha y hora en que se completó el riego programado." onClose={() => setCompleteTarget(null)}>
        {completeTarget ? <RiegoRealizadoForm saving={saving} onSubmit={complete} onCancel={() => setCompleteTarget(null)} /> : null}
      </Modal>
      <ConfirmDialog open={Boolean(cancelTarget)} title="Cancelar riego programado" description="Se conservará el registro para consulta, pero no quedará pendiente de ejecución." confirmLabel="Cancelar riego" tone="danger" loading={saving} onCancel={() => setCancelTarget(null)} onConfirm={cancel} />
    </main>
  );
}

function RiegoProgramadoForm({ traces, jabas, saving, onSubmit, onCancel }: { traces: LoteTrazableResponse[]; jabas: JabaResponse[]; saving: boolean; onSubmit: (payload: RiegoProgramadoFormPayload) => Promise<void>; onCancel: () => void }) {
  const initialTrace = traces[0];
  const [payload, setPayload] = useState<RiegoProgramadoFormPayload>({
    loteTrazableId: initialTrace?.id || 0,
    camaId: initialTrace?.camaInicial?.id || 0,
    jabaId: undefined,
    fechaProgramada: today(),
    horaProgramada: nowTime(),
    etapaAplicacion: 'CRECIMIENTO',
    observacion: ''
  });
  const [error, setError] = useState<string | null>(null);
  const selectedTrace = traces.find((item) => item.id === payload.loteTrazableId);
  const availableJabas = useMemo(() => jabas.filter((item) => item.cama?.id === selectedTrace?.camaInicial?.id && item.estado === 'ACTIVA'), [jabas, selectedTrace?.camaInicial?.id]);

  function chooseTrace(id: number) {
    const trace = traces.find((item) => item.id === id);
    setPayload((current) => ({ ...current, loteTrazableId: id, camaId: trace?.camaInicial?.id || 0, jabaId: undefined }));
  }

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      setError(null);
      await onSubmit({ ...payload, jabaId: payload.jabaId || undefined, observacion: payload.observacion?.trim() || undefined });
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'No fue posible programar el riego.');
    }
  }

  const noPrerequisites = traces.length === 0;
  return <form className="form-shell" onSubmit={submit}>
    {error ? <FormMessage>{error}</FormMessage> : null}
    {noPrerequisites ? <FormMessage>Primero registra un lote trazable activo para programar riegos.</FormMessage> : null}
    <FormSection title="Ubicación y programación" description="El riego de crecimiento puede aplicarse a la cama; para recuperación puedes señalar una jaba específica." icon={<Droplets size={18} />}>
      <div className="form-grid form-grid--two">
        <label className="form-grid__full"><span>Lote trazable</span><select value={payload.loteTrazableId} onChange={(event) => chooseTrace(Number(event.target.value))} required><option value={0} disabled>Selecciona un lote trazable</option>{traces.map((item) => <option key={item.id} value={item.id}>{item.codigo} · {item.variedad}</option>)}</select></label>
        <label><span>Cama</span><input value={selectedTrace?.camaInicial?.codigo || 'Selecciona un lote trazable'} readOnly /></label>
        <label><span>Jaba específica</span><select value={payload.jabaId || 0} onChange={(event) => setPayload((current) => ({ ...current, jabaId: Number(event.target.value) || undefined }))}><option value={0}>Aplicar a toda la cama</option>{availableJabas.map((item) => <option key={item.id} value={item.id}>{item.codigo} · {item.capacidadMacetas} macetas</option>)}</select></label>
        <label><span>Fecha programada</span><input type="date" value={payload.fechaProgramada} onChange={(event) => setPayload({ ...payload, fechaProgramada: event.target.value })} required /></label>
        <label><span>Hora programada</span><input type="time" value={payload.horaProgramada} onChange={(event) => setPayload({ ...payload, horaProgramada: event.target.value })} required /></label>
        <label><span>Aplicación</span><select value={payload.etapaAplicacion} onChange={(event) => setPayload({ ...payload, etapaAplicacion: event.target.value })}><option value="CRECIMIENTO">Crecimiento</option><option value="RECUPERACION">Recuperación</option></select></label>
        <label className="form-grid__full"><span>Observación</span><textarea value={payload.observacion || ''} onChange={(event) => setPayload({ ...payload, observacion: event.target.value })} placeholder="Indica un detalle útil para la operación, si corresponde." /></label>
      </div>
    </FormSection>
    <footer className="form-actions form-actions--sticky"><button type="button" className="ghost-button" onClick={onCancel} disabled={saving}>Cancelar</button><button type="submit" className="action-button" disabled={saving || noPrerequisites || !payload.camaId}>{saving ? 'Guardando...' : <><Save size={16} /> Programar riego</>}</button></footer>
  </form>;
}

function RiegoRealizadoForm({ saving, onSubmit, onCancel }: { saving: boolean; onSubmit: (payload: RiegoRealizadoPayload) => Promise<void>; onCancel: () => void }) {
  const [payload, setPayload] = useState<RiegoRealizadoPayload>({ fechaEjecucion: today(), horaEjecucion: nowTime(), observacion: '' });
  const [error, setError] = useState<string | null>(null);
  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try { setError(null); await onSubmit({ ...payload, observacion: payload.observacion?.trim() || undefined }); }
    catch (exception) { setError(exception instanceof Error ? exception.message : 'No fue posible registrar el riego realizado.'); }
  }
  return <form className="form-shell" onSubmit={submit}>{error ? <FormMessage>{error}</FormMessage> : null}<FormSection title="Ejecución" description="Confirma cuándo se aplicó el riego para mantener el seguimiento de la cama o jaba." icon={<CheckCircle2 size={18} />}><div className="form-grid form-grid--two"><label><span>Fecha de ejecución</span><input type="date" value={payload.fechaEjecucion} onChange={(event) => setPayload({ ...payload, fechaEjecucion: event.target.value })} required /></label><label><span>Hora de ejecución</span><input type="time" value={payload.horaEjecucion} onChange={(event) => setPayload({ ...payload, horaEjecucion: event.target.value })} required /></label><label className="form-grid__full"><span>Observación</span><textarea value={payload.observacion || ''} onChange={(event) => setPayload({ ...payload, observacion: event.target.value })} placeholder="Registra una observación operativa si es necesaria." /></label></div></FormSection><footer className="form-actions form-actions--sticky"><button type="button" className="ghost-button" onClick={onCancel} disabled={saving}>Cancelar</button><button type="submit" className="action-button" disabled={saving}>{saving ? 'Guardando...' : <><Save size={16} /> Confirmar riego</>}</button></footer></form>;
}
