import { useEffect, useMemo, useState } from 'react';
import { Archive, CheckCircle2, Eye, GitBranch, Link2, Pencil, Plus, Route, XCircle } from 'lucide-react';
import { DetailDrawer } from '../components/DetailDrawer';
import { EmptyState } from '../components/EmptyState';
import { InfoGrid } from '../components/InfoGrid';
import { Modal } from '../components/Modal';
import { ModuleHeader } from '../components/ModuleHeader';
import { StatusBadge } from '../components/StatusBadge';
import { LoteTrazableForm } from '../components/LoteTrazableForm';
import { FormSection } from '../components/FormLayout';
import { blueberryApi } from '../lib/api';
import { dateShort, numberCompact } from '../lib/format';
import { emitToast } from '../lib/uiEvents';
import type { CamaResponse, LegacyMovementResponse, LoteTrazableDetailResponse, LoteTrazableFormPayload, LoteTrazableResponse, ReferenceResponse } from '../types/api';

interface LotesTrazablesPageProps {
  lotes: ReferenceResponse[];
  camas: CamaResponse[];
  canManage: boolean;
  isAdministrator: boolean;
  onChanged?: (items: LoteTrazableResponse[]) => void;
}

function toPayload(trace: LoteTrazableResponse): LoteTrazableFormPayload {
  return { codigo: trace.codigo, variedad: trace.variedad, procedencia: trace.procedencia, fechaIngreso: trace.fechaIngreso, estado: trace.estado, observacion: trace.observacion || '', loteFisicoId: trace.loteFisico?.id || 0, camaInicialId: trace.camaInicial?.id || 0 };
}

export function LotesTrazablesPage({ lotes, camas, canManage, isAdministrator, onChanged }: LotesTrazablesPageProps) {
  const [items, setItems] = useState<LoteTrazableResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState('');
  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState<LoteTrazableResponse | null>(null);
  const [selected, setSelected] = useState<LoteTrazableDetailResponse | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [pendingState, setPendingState] = useState<LoteTrazableResponse | null>(null);
  const [stateReason, setStateReason] = useState('');
  const [saving, setSaving] = useState(false);
  const [normalizing, setNormalizing] = useState<LegacyMovementResponse | null>(null);
  const [evidence, setEvidence] = useState('');

  async function reload() { try { setLoading(true); setError(null); const response = await blueberryApi.lotesTrazables(); setItems(response); onChanged?.(response); } catch (exception) { setError(exception instanceof Error ? exception.message : 'No se pudieron cargar los lotes trazables.'); } finally { setLoading(false); } }
  useEffect(() => { void reload(); }, []);
  const filtered = useMemo(() => { const term = query.trim().toLowerCase(); return items.filter((item) => !term || [item.codigo, item.variedad, item.procedencia, item.estado, item.loteFisico?.codigo, item.camaInicial?.codigo].some((value) => String(value || '').toLowerCase().includes(term))); }, [items, query]);
  async function select(item: LoteTrazableResponse) { try { setDetailLoading(true); setSelected(await blueberryApi.loteTrazable(item.id)); } catch (exception) { emitToast('error', 'No se pudo cargar el detalle', exception instanceof Error ? exception.message : 'Ocurrió un error inesperado.'); } finally { setDetailLoading(false); } }
  async function create(payload: LoteTrazableFormPayload) { const response = await blueberryApi.createLoteTrazable(payload); setCreating(false); await reload(); emitToast('success', 'Lote trazable creado', `${response.codigo} ya puede usarse en la cadena operativa.`); }
  async function update(payload: LoteTrazableFormPayload) { if (!editing) return; await blueberryApi.updateLoteTrazable(editing.id, payload); setEditing(null); await reload(); emitToast('success', 'Lote trazable actualizado', 'La información fue actualizada y auditada.'); }
  async function changeState() { if (!pendingState) return; try { setSaving(true); const target = pendingState.estado === 'ACTIVO' ? 'ARCHIVADO' : 'ACTIVO'; await blueberryApi.changeLoteTrazableState(pendingState.id, target, target === 'ACTIVO' ? undefined : stateReason.trim()); setPendingState(null); setStateReason(''); await reload(); emitToast('success', 'Estado actualizado', `El lote trazable ahora está ${target.toLowerCase()}.`); } catch (exception) { emitToast('error', 'No se pudo cambiar el estado', exception instanceof Error ? exception.message : 'Ocurrió un error inesperado.'); } finally { setSaving(false); } }
  async function normalizeLegacy() {
    if (!selected || !normalizing || !evidence.trim()) return;
    try {
      setSaving(true);
      const updated = await blueberryApi.normalizeLegacyMovement(selected.loteTrazable.id, { etapa: normalizing.etapa, registroId: normalizing.id, evidencia: evidence.trim() });
      setSelected(updated);
      setNormalizing(null);
      setEvidence('');
      await reload();
      emitToast('success', 'Legado normalizado', 'El movimiento fue vinculado con evidencia y quedó registrado en auditoría.');
    } catch (exception) {
      emitToast('error', 'No se pudo normalizar el movimiento', exception instanceof Error ? exception.message : 'Ocurrió un error inesperado.');
    } finally {
      setSaving(false);
    }
  }
  const active = items.filter((item) => item.estado === 'ACTIVO').length;

  return <main className="content-grid">
    <ModuleHeader eyebrow="Trazabilidad" title="Lotes trazables" description="Unidad operativa que conserva el origen, ubicación inicial y recorrido real de cada grupo de plantas." icon={<Route size={21} />} tone="green" actions={canManage ? <button type="button" className="action-button" onClick={() => setCreating(true)}><Plus size={16} /> Nuevo lote trazable</button> : undefined} />
    <section className="summary-strip summary-strip--three"><article className="summary-pill summary-pill--green"><span className="summary-pill__icon"><Route size={18} /></span><strong>{items.length}</strong><span>Lotes trazables</span><small>registrados</small></article><article className="summary-pill summary-pill--blue"><span className="summary-pill__icon"><CheckCircle2 size={18} /></span><strong>{active}</strong><span>Activos</span><small>disponibles para operar</small></article><article className="summary-pill summary-pill--purple"><span className="summary-pill__icon"><GitBranch size={18} /></span><strong>{items.filter((item) => item.legadoPendienteNormalizacion).length}</strong><span>Legado pendiente</span><small>requiere normalización</small></article></section>
    <section className="panel-card panel-card--interactive"><div className="module-toolbar-card module-toolbar-card--filters"><label className="filter-toolbar__search"><Route size={16}/><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Buscar código, variedad, procedencia o ubicación..." /></label></div>
      {error ? <EmptyState icon={<XCircle size={28}/>} title="No se pudieron cargar los lotes trazables" description={error} action={<button type="button" className="action-button" onClick={() => void reload()}>Reintentar</button>} /> : null}
      {!error && !loading && filtered.length === 0 ? <EmptyState icon={<Route size={28}/>} title="Aún no hay lotes trazables" description="Registra el primer lote trazable para iniciar el seguimiento de las plantas." action={canManage ? <button type="button" className="action-button" onClick={() => setCreating(true)}>Crear lote trazable</button> : undefined} /> : null}
      {filtered.length > 0 ? <div className="data-table-wrap"><table className="data-table"><thead><tr><th>Código</th><th>Variedad / origen</th><th>Ubicación inicial</th><th>Ingreso</th><th>Estado</th><th /></tr></thead><tbody>{filtered.map((item) => <tr key={item.id}><td><strong className="table-code">{item.codigo}</strong>{item.legadoPendienteNormalizacion ? <small className="table-subtext">Legado pendiente</small> : null}</td><td><strong>{item.variedad}</strong><small className="table-subtext">{item.procedencia}</small></td><td>{item.loteFisico?.codigo || 'Sin invernadero'}<small className="table-subtext">{item.camaInicial?.codigo || 'Sin cama'}</small></td><td>{dateShort(item.fechaIngreso)}</td><td><StatusBadge value={item.estado}/></td><td><div className="icon-actions"><button type="button" className="icon-action" title="Ver trazabilidad" onClick={() => void select(item)}><Eye size={15}/></button>{canManage ? <><button type="button" className="icon-action" title="Editar" onClick={() => setEditing(item)}><Pencil size={15}/></button><button type="button" className={item.estado === 'ACTIVO' ? 'icon-action icon-action--danger' : 'icon-action'} title={item.estado === 'ACTIVO' ? 'Archivar' : 'Activar'} onClick={() => setPendingState(item)}>{item.estado === 'ACTIVO' ? <Archive size={15}/> : <CheckCircle2 size={15}/>}</button></> : null}</div></td></tr>)}</tbody></table></div> : null}
    </section>
    <DetailDrawer open={Boolean(selected) || detailLoading} title={selected?.loteTrazable.codigo || 'Detalle trazable'} subtitle={selected?.loteTrazable.variedad || (detailLoading ? 'Cargando información...' : '')} onClose={() => setSelected(null)}>{selected ? <><InfoGrid items={[{label:'Estado',value:<StatusBadge value={selected.loteTrazable.estado}/>,tone:'green'},{label:'Saldo disponible',value:numberCompact(selected.balance.saldoDisponible),tone:'blue'},{label:'Plantas sembradas',value:numberCompact(selected.balance.sembradas)},{label:'Despachadas',value:numberCompact(selected.balance.despachadas),tone:'purple'},{label:'Mermas',value:numberCompact(selected.balance.mermas),tone:'orange'}]}/><section className="drawer-section"><h3>Línea de tiempo</h3>{selected.lineaTiempo.length === 0 ? <p>Sin movimientos asociados. Los registros históricos no se vinculan automáticamente.</p> : <div className="timeline-list">{selected.lineaTiempo.map((event,index)=><article className="timeline-event" key={`${event.referencia}-${index}`}><strong>{event.etapa} · {event.referencia}</strong><span><StatusBadge value={event.estado}/>&nbsp; {numberCompact(event.cantidad || 0)} plantas · {dateShort(event.fecha)}</span><small>{event.detalle || 'Sin observación'} · {event.responsable || 'Sin responsable'}</small></article>)}</div>}</section>{isAdministrator ? <section className="drawer-section"><h3>Movimientos históricos candidatos</h3><p>Solo se muestran registros sin vínculo trazable que coinciden por invernadero, cama y fecha. Despachos históricos no se vinculan automáticamente porque requieren una clasificación fuente verificable.</p>{selected.pendientesLegado.length === 0 ? <p>Sin movimientos históricos candidatos.</p> : <div className="data-table-wrap"><table className="data-table"><thead><tr><th>Etapa</th><th>Referencia</th><th>Fecha</th><th>Cantidad</th><th>Estado</th><th /></tr></thead><tbody>{selected.pendientesLegado.map((item) => <tr key={`${item.etapa}-${item.id}`}><td>{item.etapa}</td><td><strong>{item.referencia}</strong><small className="table-subtext">{item.detalle || 'Sin detalle'}</small></td><td>{dateShort(item.fecha)}</td><td>{numberCompact(item.cantidad || 0)}</td><td><StatusBadge value={item.estado}/></td><td><button type="button" className="icon-action" title="Vincular con evidencia" onClick={() => setNormalizing(item)}><Link2 size={15}/></button></td></tr>)}</tbody></table></div>}</section> : null}</> : null}</DetailDrawer>
    <Modal open={creating} title="Nuevo lote trazable" description="Registra el origen de plantas antes de iniciar la cadena operativa." onClose={() => setCreating(false)}>{creating ? <LoteTrazableForm lotes={lotes} camas={camas} onSubmit={create} onCancel={() => setCreating(false)} /> : null}</Modal>
    <Modal open={Boolean(editing)} title="Editar lote trazable" description="El código se conserva inmutable para proteger la trazabilidad." onClose={() => setEditing(null)}>{editing ? <LoteTrazableForm lotes={lotes} camas={camas} initialData={toPayload(editing)} editing onSubmit={update} onCancel={() => setEditing(null)} /> : null}</Modal>
    <Modal open={Boolean(pendingState)} title={pendingState?.estado === 'ACTIVO' ? 'Archivar lote trazable' : 'Activar lote trazable'} description={pendingState?.estado === 'ACTIVO' ? 'Indica el motivo. El lote no se elimina ni se desvinculan sus movimientos.' : 'El lote volverá a estar disponible para nuevos registros.'} size="sm" onClose={() => setPendingState(null)}>{pendingState?.estado === 'ACTIVO' ? <form className="form-shell" onSubmit={(event)=>{event.preventDefault();void changeState();}}><FormSection title="Motivo del archivado" description="Explica por qué el lote dejará de estar disponible para nuevos registros."><div className="form-grid"><label className="form-grid__full"><span>Motivo</span><textarea value={stateReason} onChange={(event)=>setStateReason(event.target.value)} required maxLength={255}/></label></div></FormSection><footer className="form-actions form-actions--sticky"><button type="button" className="ghost-button" onClick={()=>setPendingState(null)}>Cancelar</button><button type="submit" className="action-button" disabled={saving || !stateReason.trim()}>Archivar lote</button></footer></form> : <div className="form-actions"><button type="button" className="ghost-button" onClick={()=>setPendingState(null)}>Cancelar</button><button type="button" className="action-button" onClick={()=>void changeState()} disabled={saving}>Activar lote</button></div>}</Modal>
    <Modal open={Boolean(normalizing)} title="Normalizar movimiento histórico" description="Confirma la evidencia que prueba la procedencia. Esta acción no inventa vínculos y queda registrada en auditoría." size="md" onClose={() => { if (!saving) { setNormalizing(null); setEvidence(''); } }}>{normalizing ? <form className="form-shell" onSubmit={(event) => { event.preventDefault(); void normalizeLegacy(); }}><FormSection title="Evidencia de procedencia" description="Registra la referencia que confirma la relación del movimiento con este lote trazable."><div className="form-grid"><p className="form-grid__full form-readonly-note">{normalizing.referencia} · {dateShort(normalizing.fecha)} · {numberCompact(normalizing.cantidad || 0)} plantas</p><label className="form-grid__full"><span>Evidencia de normalización</span><textarea value={evidence} onChange={(event) => setEvidence(event.target.value)} required maxLength={255} placeholder="Ej.: Guía de operación o bitácora física verificada." /></label></div></FormSection><footer className="form-actions form-actions--sticky"><button type="button" className="ghost-button" onClick={() => { setNormalizing(null); setEvidence(''); }} disabled={saving}>Cancelar</button><button type="submit" className="action-button" disabled={saving || !evidence.trim()}>Vincular movimiento</button></footer></form> : null}</Modal>
  </main>;
}
