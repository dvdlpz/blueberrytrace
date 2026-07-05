import { useMemo, useState } from 'react';
import { Boxes, ClipboardPlus, Pencil, Plus, Power, Save } from 'lucide-react';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { EmptyState } from '../components/EmptyState';
import { FormMessage, FormSection } from '../components/FormLayout';
import { Modal } from '../components/Modal';
import { ModuleHeader } from '../components/ModuleHeader';
import { StatusBadge } from '../components/StatusBadge';
import { blueberryApi } from '../lib/api';
import { emitToast } from '../lib/uiEvents';
import type { CamaResponse, JabaFormPayload, JabaResponse } from '../types/api';

interface JabasPageProps {
  jabas: JabaResponse[];
  camas: CamaResponse[];
  onJabasChange: (items: JabaResponse[]) => void;
}

const emptyPayload = (camas: CamaResponse[]): JabaFormPayload => ({ codigo: '', camaId: camas.find((item) => item.estado?.toUpperCase() === 'ACTIVA')?.id || 0, capacidadMacetas: 1, ordenEnCama: 1, observacion: '', estado: 'ACTIVA' });
function toPayload(item: JabaResponse): JabaFormPayload { return { codigo: item.codigo, camaId: item.cama?.id || 0, capacidadMacetas: item.capacidadMacetas || 1, ordenEnCama: item.ordenEnCama || 1, observacion: item.observacion || '', estado: item.estado || 'ACTIVA' }; }

export function JabasPage({ jabas, camas, onJabasChange }: JabasPageProps) {
  const [editing, setEditing] = useState<JabaResponse | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [pendingState, setPendingState] = useState<JabaResponse | null>(null);
  const [saving, setSaving] = useState(false);
  const activeCamas = useMemo(() => camas.filter((item) => item.estado?.toUpperCase() === 'ACTIVA'), [camas]);

  async function save(payload: JabaFormPayload) {
    setSaving(true);
    try {
      const response = editing ? await blueberryApi.updateJaba(editing.id, payload) : await blueberryApi.createJaba(payload);
      onJabasChange(response.items); setEditing(null); setCreateOpen(false);
      emitToast('success', editing ? 'Jaba actualizada' : 'Jaba registrada', 'La jaba quedó disponible dentro de la cama seleccionada.');
    } finally { setSaving(false); }
  }

  async function toggle() {
    if (!pendingState) return;
    try {
      setSaving(true);
      const response = await blueberryApi.toggleJabaStatus(pendingState.id);
      onJabasChange(response.items); setPendingState(null);
      emitToast('success', 'Estado actualizado', 'La disponibilidad de la jaba fue actualizada.');
    } catch (error) { emitToast('error', 'No se pudo actualizar', error instanceof Error ? error.message : 'Inténtalo nuevamente.'); }
    finally { setSaving(false); }
  }

  return <main className="content-grid">
    <ModuleHeader eyebrow="Estructura productiva" title="Jabas de siembra" description="Registra las jabas ubicadas en cada cama y la capacidad de macetas que pueden contener." icon={<Boxes size={21} />} tone="green" actions={<button className="action-button" type="button" onClick={() => setCreateOpen(true)}><Plus size={16} /> Registrar jaba</button>} />
    <section className="summary-strip summary-strip--three">
      <article className="summary-pill summary-pill--green"><span className="summary-pill__icon"><Boxes size={18} /></span><span>Jabas registradas</span><strong>{jabas.length}</strong><small>unidades</small></article>
      <article className="summary-pill summary-pill--blue"><span className="summary-pill__icon"><ClipboardPlus size={18} /></span><span>Macetas ubicadas</span><strong>{jabas.reduce((total, item) => total + (item.macetasOcupadas || 0), 0)}</strong><small>en jabas activas</small></article>
      <article className="summary-pill summary-pill--purple"><span className="summary-pill__icon"><Power size={18} /></span><span>Espacio disponible</span><strong>{jabas.reduce((total, item) => total + (item.macetasDisponibles || 0), 0)}</strong><small>macetas</small></article>
    </section>
    <section className="panel-card">
      <div className="panel-card__header"><div><h2>Jabas por cama</h2><p>Cada jaba organiza macetas dentro de una cama productiva.</p></div><span className="panel-card__count">{jabas.length} registros</span></div>
      {jabas.length ? <div className="data-table-wrap"><table className="data-table"><thead><tr><th>Jaba</th><th>Cama</th><th>Macetas</th><th>Orden</th><th>Estado</th><th /></tr></thead><tbody>{jabas.map((item) => <tr key={item.id}><td><strong className="table-code">{item.codigo}</strong><small>{item.observacion || 'Sin observación'}</small></td><td>{item.cama?.codigo || 'Sin cama'}</td><td><strong>{item.macetasOcupadas || 0} / {item.capacidadMacetas}</strong><small>{item.macetasDisponibles || 0} espacios disponibles{item.macetasEnRecuperacion ? ` · ${item.macetasEnRecuperacion} en riego` : ''}</small></td><td>{item.ordenEnCama}</td><td><StatusBadge value={item.estado} /></td><td><div className="icon-actions"><button className="icon-action" type="button" aria-label="Editar jaba" onClick={() => setEditing(item)}><Pencil size={15} /></button><button className="icon-action" type="button" aria-label="Cambiar disponibilidad" onClick={() => setPendingState(item)}><Power size={15} /></button></div></td></tr>)}</tbody></table></div> : <EmptyState icon={<Boxes size={28} />} title="Aún no hay jabas registradas" description="Registra las jabas de cada cama antes de ubicar macetas y realizar la siembra." action={<button type="button" className="action-button" onClick={() => setCreateOpen(true)}><Plus size={15} /> Registrar primera jaba</button>} />}
    </section>
    <Modal open={createOpen || Boolean(editing)} title={editing ? 'Editar jaba de siembra' : 'Registrar jaba de siembra'} description="Indica la cama, capacidad de macetas y orden físico de la jaba." onClose={() => { setCreateOpen(false); setEditing(null); }}>
      <JabaForm camas={activeCamas} initialData={editing ? toPayload(editing) : emptyPayload(activeCamas)} saving={saving} onSubmit={save} onCancel={() => { setCreateOpen(false); setEditing(null); }} />
    </Modal>
    <ConfirmDialog open={Boolean(pendingState)} title="Actualizar disponibilidad" description={pendingState?.estado?.toUpperCase() === 'ACTIVA' ? 'La jaba dejará de estar disponible para nuevas operaciones.' : 'La jaba volverá a estar disponible para nuevas operaciones.'} confirmLabel="Confirmar" loading={saving} onCancel={() => setPendingState(null)} onConfirm={toggle} />
  </main>;
}

function JabaForm({ camas, initialData, saving, onSubmit, onCancel }: { camas: CamaResponse[]; initialData: JabaFormPayload; saving: boolean; onSubmit: (payload: JabaFormPayload) => Promise<void>; onCancel: () => void }) {
  const [payload, setPayload] = useState(initialData); const [error, setError] = useState<string | null>(null);
  async function submit(event: React.FormEvent<HTMLFormElement>) { event.preventDefault(); if (!payload.camaId) { setError('Selecciona una cama activa.'); return; } try { setError(null); await onSubmit({ ...payload, codigo: payload.codigo.trim(), observacion: payload.observacion?.trim() || undefined }); } catch (exception) { setError(exception instanceof Error ? exception.message : 'No fue posible guardar la jaba.'); } }
  return <form className="form-shell" onSubmit={submit}>{error ? <FormMessage>{error}</FormMessage> : null}<FormSection title="Ubicación y capacidad" description="La capacidad indica cuántas macetas pueden colocarse dentro de la jaba." icon={<Boxes size={18} />}><div className="form-grid form-grid--two"><label><span>Código de jaba</span><input value={payload.codigo} onChange={(event) => setPayload({ ...payload, codigo: event.target.value })} maxLength={50} placeholder="Ejemplo: JAB-01" required /></label><label><span>Cama</span><select value={payload.camaId} onChange={(event) => setPayload({ ...payload, camaId: Number(event.target.value) })}><option value={0} disabled>Selecciona una cama</option>{camas.map((item) => <option key={item.id} value={item.id}>{item.codigo} · {item.lote?.codigo}</option>)}</select></label><label><span>Capacidad de macetas</span><input type="number" min={1} value={payload.capacidadMacetas} onChange={(event) => setPayload({ ...payload, capacidadMacetas: Number(event.target.value) })} required /></label><label><span>Orden dentro de la cama</span><input type="number" min={1} value={payload.ordenEnCama} onChange={(event) => setPayload({ ...payload, ordenEnCama: Number(event.target.value) })} required /></label><label className="form-grid__full"><span>Observación</span><textarea value={payload.observacion || ''} onChange={(event) => setPayload({ ...payload, observacion: event.target.value })} maxLength={255} placeholder="Ubicación o condición de la jaba" /></label></div></FormSection><footer className="form-actions form-actions--sticky"><button className="ghost-button" type="button" onClick={onCancel} disabled={saving}>Cancelar</button><button className="action-button" type="submit" disabled={saving}>{saving ? 'Guardando...' : <><Save size={16} /> Guardar jaba</>}</button></footer></form>;
}
