import { useMemo, useState } from 'react';
import { AlertTriangle, CheckCircle2, Clock3, Download, Eye, PackageCheck, Pencil, Plus, Send, Truck, Undo2, X } from 'lucide-react';
import { CargaDespachoForm } from '../components/CargaDespachoForm';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { DespachoForm } from '../components/DespachoForm';
import { DetailDrawer } from '../components/DetailDrawer';
import { EmptyState } from '../components/EmptyState';
import { FormMessage, FormPrerequisite, FormSection } from '../components/FormLayout';
import { InfoGrid } from '../components/InfoGrid';
import { Modal } from '../components/Modal';
import { ModuleHeader } from '../components/ModuleHeader';
import { StatusBadge } from '../components/StatusBadge';
import { blueberryApi } from '../lib/api';
import { downloadCsv } from '../lib/export';
import { dateShort, numberCompact } from '../lib/format';
import { emitToast } from '../lib/uiEvents';
import type { CargaDespachoFormPayload, CargaDespachoResponse, ClasificacionResponse, DespachoFormPayload, DespachoResponse, EmpaqueResponse, LoteTrazableResponse, PedidoResponse, ReferenceResponse } from '../types/api';

interface DespachoPageProps {
  despachos: DespachoResponse[];
  cargas: CargaDespachoResponse[];
  lotes: ReferenceResponse[];
  lotesTrazables: LoteTrazableResponse[];
  clasificaciones: ClasificacionResponse[];
  pedidos: PedidoResponse[];
  empaques: EmpaqueResponse[];
  validaciones: string[];
  onDespachosChange: (items: DespachoResponse[]) => void;
  onCargasChange: (items: CargaDespachoResponse[]) => void;
}

function toPayload(item: DespachoResponse): DespachoFormPayload {
  return {
    loteTrazableId: item.loteTrazable?.id || 0,
    clasificacionId: item.clasificacion?.id || 0,
    loteId: item.lote?.id || 0,
    pedidoId: item.pedido?.id || 0,
    pedidoDetalleId: item.pedidoDetalle?.id || 0,
    empaqueId: item.empaque?.id || 0,
    unidadesEmpaque: item.unidadesEmpaque || 1,
    fechaDespacho: item.fechaDespacho || new Date().toISOString().slice(0, 10),
    vehiculo: item.vehiculo || '',
    guiaRemision: item.guiaRemision || '',
    validacionCalidad: item.validacionCalidad || 'APTO',
    observacion: item.observacion || '',
    estado: item.estado || 'REGISTRADO'
  };
}

function upper(value?: string | null) {
  return (value || '').trim().toUpperCase();
}

function cargaStatusDescription(estado: string) {
  if (estado === 'CARGADA') return 'Las líneas permanecen reservadas y el tráiler ya está preparado.';
  if (estado === 'DESPACHADA') return 'La salida fue confirmada y las cantidades se descontaron del pedido y empaque.';
  if (estado === 'CANCELADA') return 'La carga fue anulada y las líneas preparadas quedaron liberadas.';
  return 'Aún puedes agregar, retirar o reordenar las líneas que viajarán en el tráiler.';
}

export function DespachoPage({ despachos, cargas, lotes, lotesTrazables, clasificaciones, pedidos, empaques, validaciones, onDespachosChange, onCargasChange }: DespachoPageProps) {
  const [tab, setTab] = useState<'historial' | 'cargas' | 'nuevo'>('historial');
  const [statusFilter, setStatusFilter] = useState('TODOS');
  const [selectedDespacho, setSelectedDespacho] = useState<DespachoResponse | null>(null);
  const [selectedCarga, setSelectedCarga] = useState<CargaDespachoResponse | null>(null);
  const [editingDespacho, setEditingDespacho] = useState<DespachoResponse | null>(null);
  const [cargaFormOpen, setCargaFormOpen] = useState(false);
  const [addingLineTo, setAddingLineTo] = useState<CargaDespachoResponse | null>(null);
  const [selectedLineId, setSelectedLineId] = useState(0);
  const [pendingCargaStatus, setPendingCargaStatus] = useState<{ item: CargaDespachoResponse; estado: string } | null>(null);
  const [confirming, setConfirming] = useState(false);
  const [lineSaving, setLineSaving] = useState(false);
  const [lineError, setLineError] = useState<string | null>(null);

  const availableStates = useMemo(() => Array.from(new Set(despachos.map((item) => item.estado).filter(Boolean))) as string[], [despachos]);
  const filteredDespachos = useMemo(() => despachos.filter((item) => statusFilter === 'TODOS' || item.estado === statusFilter), [despachos, statusFilter]);
  const availableLines = useMemo(() => {
    if (!addingLineTo) return [];
    return despachos.filter((item) => upper(item.estado) === 'REGISTRADO'
      && !item.cargaDespacho?.id
      && item.pedido?.id === addingLineTo.pedido?.id
      && item.fechaDespacho === addingLineTo.fechaCarga);
  }, [addingLineTo, despachos]);

  const confirmed = despachos.filter((item) => upper(item.estado) === 'DESPACHADO');
  const pending = despachos.filter((item) => upper(item.estado) === 'REGISTRADO');
  const plantas = confirmed.reduce((total, item) => total + (item.cantidadDespachada || 0), 0);
  const enSeguimiento = pending.reduce((total, item) => total + (item.cantidadDespachada || 0), 0);
  const confirmados = confirmed.length;
  const observados = despachos.filter((item) => /OBSERVADO|CANCELADO|RECHAZADO/i.test(`${item.estado || ''} ${item.validacionCalidad || ''}`)).length;
  const cargasActivas = cargas.filter((item) => ['PREPARADA', 'CARGADA'].includes(upper(item.estado))).length;

  async function refreshDespachos() {
    const latest = await blueberryApi.despachos();
    onDespachosChange(latest.items);
  }

  async function create(payload: DespachoFormPayload) {
    const response = await blueberryApi.createDespacho(payload);
    onDespachosChange(response.items);
    setTab('historial');
    emitToast('success', 'Línea preparada', 'Ahora agrégala a una carga de tráiler del mismo pedido.');
  }

  async function update(payload: DespachoFormPayload) {
    if (!editingDespacho) return;
    const response = await blueberryApi.updateDespacho(editingDespacho.id, payload);
    onDespachosChange(response.items);
    setEditingDespacho(null);
    setSelectedDespacho(null);
    emitToast('success', 'Línea actualizada', 'Los datos de la línea de despacho fueron guardados.');
  }

  async function createCarga(payload: CargaDespachoFormPayload) {
    const response = await blueberryApi.createCargaDespacho(payload);
    onCargasChange(response.items);
    setCargaFormOpen(false);
    setTab('cargas');
    emitToast('success', 'Carga preparada', 'Agrega las líneas por variedad que viajarán en el tráiler.');
  }

  async function addLine(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!addingLineTo || !selectedLineId) {
      setLineError('Selecciona una línea registrada para agregarla a la carga.');
      return;
    }
    try {
      setLineSaving(true);
      setLineError(null);
      const response = await blueberryApi.addCargaDespachoLinea(addingLineTo.id, selectedLineId);
      onCargasChange(response.items);
      await refreshDespachos();
      setSelectedCarga(response.items.find((item) => item.id === addingLineTo.id) || null);
      setAddingLineTo(null);
      setSelectedLineId(0);
      emitToast('success', 'Línea agregada', 'La variedad quedó reservada dentro de la carga de tráiler.');
    } catch (exception) {
      setLineError(exception instanceof Error ? exception.message : 'No fue posible agregar la línea a la carga.');
    } finally {
      setLineSaving(false);
    }
  }

  async function removeLine(carga: CargaDespachoResponse, despacho: DespachoResponse) {
    try {
      setLineSaving(true);
      const response = await blueberryApi.removeCargaDespachoLinea(carga.id, despacho.id);
      onCargasChange(response.items);
      await refreshDespachos();
      setSelectedCarga(response.items.find((item) => item.id === carga.id) || null);
      emitToast('success', 'Línea retirada', 'La línea volvió a quedar preparada para otra carga.');
    } catch (exception) {
      emitToast('error', 'No se pudo retirar', exception instanceof Error ? exception.message : 'Ocurrió un error inesperado.');
    } finally {
      setLineSaving(false);
    }
  }

  async function confirmCargaStatus() {
    if (!pendingCargaStatus) return;
    try {
      setConfirming(true);
      const response = await blueberryApi.changeCargaDespachoStatus(pendingCargaStatus.item.id, pendingCargaStatus.estado);
      onCargasChange(response.items);
      await refreshDespachos();
      const updated = response.items.find((item) => item.id === pendingCargaStatus.item.id) || null;
      setSelectedCarga(updated);
      emitToast('success', 'Estado de carga actualizado', `La carga ${pendingCargaStatus.item.codigo} quedó ${pendingCargaStatus.estado}.`);
      setPendingCargaStatus(null);
    } catch (exception) {
      emitToast('error', 'No se pudo actualizar la carga', exception instanceof Error ? exception.message : 'Ocurrió un error inesperado.');
    } finally {
      setConfirming(false);
    }
  }

  function exportCsv() {
    downloadCsv('blueberrytrace-despachos.csv', [
      'Código', 'Lote', 'Carga', 'Pedido', 'Variedad', 'Fecha', 'Cantidad', 'Modalidad', 'Destino', 'Guía de remisión', 'Validación', 'Estado', 'Responsable'
    ], filteredDespachos.map((item) => [
      `D-${String(item.id).padStart(4, '0')}`,
      item.loteTrazable?.codigo || item.lote?.codigo || '',
      item.cargaDespacho?.codigo || '',
      item.pedido?.codigo || '',
      item.pedidoDetalle?.codigo || '',
      item.fechaDespacho || '',
      item.cantidadDespachada || 0,
      item.modalidad || '',
      item.destino || '',
      item.guiaRemision || '',
      item.validacionCalidad || '',
      item.estado || '',
      item.usuarioRegistro?.nombreCompleto || ''
    ]));
  }

  const summary = [
    { label: 'Despachado confirmado', value: plantas, suffix: 'plantas', tone: 'green', icon: <Truck size={18} /> },
    { label: 'Preparado para tráiler', value: enSeguimiento, suffix: 'plantas', tone: 'blue', icon: <Clock3 size={18} /> },
    { label: 'Cargas activas', value: cargasActivas, suffix: 'tráilers', tone: 'purple', icon: <PackageCheck size={18} /> },
    { label: 'Con observación', value: observados, suffix: 'registros', tone: 'orange', icon: <AlertTriangle size={18} /> }
  ];

  return (
    <main className="content-grid dispatch-screen">
      <ModuleHeader
        eyebrow="Salida"
        title="Despacho por carga de tráiler"
        description="Prepara líneas por variedad, consolídalas por pedido y confirma la salida física del tráiler."
        icon={<Truck size={21} />}
        tone="blue"
      />

      <div className="tab-switcher">
        <button type="button" className={tab === 'historial' ? 'tab-switcher__item tab-switcher__item--active' : 'tab-switcher__item'} onClick={() => setTab('historial')}><PackageCheck size={15} /> Líneas preparadas</button>
        <button type="button" className={tab === 'cargas' ? 'tab-switcher__item tab-switcher__item--active' : 'tab-switcher__item'} onClick={() => setTab('cargas')}><Truck size={15} /> Cargas de tráiler</button>
        <button type="button" className={tab === 'nuevo' ? 'tab-switcher__item tab-switcher__item--active' : 'tab-switcher__item'} onClick={() => setTab('nuevo')}><Plus size={15} /> Preparar línea</button>
      </div>

      <section className="summary-strip summary-strip--four">
        {summary.map((card) => (
          <article key={card.label} className={`summary-pill summary-pill--${card.tone}`}>
            <span className="summary-pill__icon">{card.icon}</span>
            <span>{card.label}</span>
            <strong>{numberCompact(card.value)}</strong>
            <small>{card.suffix}</small>
          </article>
        ))}
      </section>

      {tab === 'historial' ? (
        <section className="panel-card">
          <div className="panel-card__header">
            <div>
              <h2>Líneas de despacho</h2>
              <p>Cada línea representa empaques de una variedad. Las líneas registradas se consolidan después en el tráiler.</p>
            </div>
            <div className="button-group">
              <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)} className="toolbar-select">
                <option value="TODOS">Todos los estados</option>
                {availableStates.map((estado) => <option key={estado} value={estado}>{estado}</option>)}
              </select>
              <button type="button" className="ghost-button" onClick={exportCsv} disabled={filteredDespachos.length === 0}><Download size={15} /> Exportar CSV</button>
            </div>
          </div>

          {filteredDespachos.length > 0 ? (
            <div className="data-table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Línea</th>
                    <th>Pedido / variedad</th>
                    <th>Fecha</th>
                    <th>Cantidad</th>
                    <th>Empaque</th>
                    <th>Carga de tráiler</th>
                    <th>Estado</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {filteredDespachos.map((item) => (
                    <tr key={item.id}>
                      <td><strong className="table-code">D-{String(item.id).padStart(4, '0')}</strong><small className="table-secondary">{item.loteTrazable?.codigo || item.lote?.codigo || 'Sin lote trazable'}</small></td>
                      <td><strong>{item.pedido?.codigo || 'Sin pedido'}</strong><small className="table-secondary">{item.pedidoDetalle?.codigo || 'Variedad no indicada'}</small></td>
                      <td>{dateShort(item.fechaDespacho)}</td>
                      <td>{numberCompact(item.cantidadDespachada || 0)}<small className="table-secondary">{item.unidadesEmpaque || 0} unidades</small></td>
                      <td><StatusBadge value={item.modalidad} /></td>
                      <td>{item.cargaDespacho ? <StatusBadge value={item.cargaDespacho.codigo} /> : <span className="table-muted">Pendiente de consolidar</span>}</td>
                      <td><StatusBadge value={item.estado} /></td>
                      <td>
                        <div className="icon-actions">
                          <button type="button" className="icon-action" aria-label="Ver detalle" onClick={() => setSelectedDespacho(item)}><Eye size={15} /></button>
                          {item.loteTrazable?.id && item.clasificacion?.id && !item.cargaDespacho?.id && upper(item.estado) === 'REGISTRADO' ? <button type="button" className="icon-action" aria-label="Editar línea" onClick={() => setEditingDespacho(item)}><Pencil size={15} /></button> : null}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState
              icon={<Truck size={26} />}
              title="Sin líneas preparadas"
              description="Prepara una línea desde un empaque validado y luego inclúyela en una carga de tráiler."
              compact
              action={<button type="button" className="action-button" onClick={() => setTab('nuevo')}><Plus size={15} /> Preparar línea</button>}
            />
          )}
          <div className="table-footer-note">{filteredDespachos.length} de {despachos.length} líneas registradas</div>
        </section>
      ) : null}

      {tab === 'cargas' ? (
        <section className="panel-card">
          <div className="panel-card__header">
            <div>
              <h2>Cargas de tráiler</h2>
              <p>Una carga reúne varias líneas del mismo pedido y conserva la trazabilidad por empaque y variedad.</p>
            </div>
            <button type="button" className="action-button" onClick={() => setCargaFormOpen(true)}><Plus size={15} /> Preparar carga</button>
          </div>
          {cargas.length > 0 ? (
            <div className="data-table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Carga</th>
                    <th>Pedido</th>
                    <th>Fecha</th>
                    <th>Tráiler</th>
                    <th>Contenido</th>
                    <th>Estado</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {cargas.map((item) => {
                    const editable = upper(item.estado) === 'PREPARADA';
                    const loaded = upper(item.estado) === 'CARGADA';
                    return (
                      <tr key={item.id}>
                        <td><strong className="table-code">{item.codigo}</strong><small className="table-secondary">{item.destino || 'Sin destino'}</small></td>
                        <td>{item.pedido?.codigo || 'Sin pedido'}</td>
                        <td>{dateShort(item.fechaCarga)}</td>
                        <td>{item.vehiculo}</td>
                        <td><strong>{numberCompact(item.totalPlantas || 0)} plantas</strong><small className="table-secondary">{item.totalLineas || 0} líneas · {item.totalUnidades || 0} unidades</small></td>
                        <td><StatusBadge value={item.estado} /></td>
                        <td>
                          <div className="icon-actions">
                            <button type="button" className="icon-action" aria-label="Ver carga" onClick={() => setSelectedCarga(item)}><Eye size={15} /></button>
                            {editable ? <button type="button" className="icon-action" aria-label="Agregar línea" onClick={() => { setAddingLineTo(item); setSelectedLineId(0); setLineError(null); }}><Plus size={15} /></button> : null}
                            {editable && item.totalLineas > 0 ? <button type="button" className="icon-action" aria-label="Marcar tráiler cargado" onClick={() => setPendingCargaStatus({ item, estado: 'CARGADA' })}><PackageCheck size={15} /></button> : null}
                            {loaded ? <button type="button" className="icon-action" aria-label="Confirmar salida" onClick={() => setPendingCargaStatus({ item, estado: 'DESPACHADA' })}><Send size={15} /></button> : null}
                            {loaded ? <button type="button" className="icon-action" aria-label="Reabrir carga" onClick={() => setPendingCargaStatus({ item, estado: 'PREPARADA' })}><Undo2 size={15} /></button> : null}
                            {(editable || loaded) ? <button type="button" className="icon-action" aria-label="Cancelar carga" onClick={() => setPendingCargaStatus({ item, estado: 'CANCELADA' })}><X size={15} /></button> : null}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState icon={<Truck size={26} />} title="Aún no hay cargas de tráiler" description="Prepara una carga para un pedido y agrégale las líneas de cada variedad que viajarán juntas." compact action={<button type="button" className="action-button" onClick={() => setCargaFormOpen(true)}><Plus size={15} /> Preparar carga</button>} />
          )}
        </section>
      ) : null}

      {tab === 'nuevo' ? (
        <section className="panel-card panel-card--form-only dispatch-form-card">
          <div className="panel-card__header"><div><h2>Preparar línea de despacho</h2><p>Selecciona el empaque de una variedad validada. La salida final se confirma desde la carga de tráiler.</p></div></div>
          <DespachoForm lotesTrazables={lotesTrazables} clasificaciones={clasificaciones} pedidos={pedidos} empaques={empaques} validaciones={validaciones} onSubmit={create} onCancel={() => setTab('historial')} />
        </section>
      ) : null}

      <DetailDrawer
        open={Boolean(selectedDespacho)}
        title={selectedDespacho ? `D-${String(selectedDespacho.id).padStart(4, '0')}` : 'Detalle de línea'}
        subtitle={selectedDespacho?.pedidoDetalle?.codigo || selectedDespacho?.destino || 'Línea de despacho'}
        onClose={() => setSelectedDespacho(null)}
        actions={selectedDespacho && !selectedDespacho.cargaDespacho?.id && upper(selectedDespacho.estado) === 'REGISTRADO' ? <button type="button" className="action-button" onClick={() => setEditingDespacho(selectedDespacho)}><Pencil size={15} /> Editar línea</button> : null}
      >
        {selectedDespacho ? (
          <>
            <InfoGrid
              items={[
                { label: 'Pedido', value: selectedDespacho.pedido?.codigo || 'Sin pedido', tone: 'green' },
                { label: 'Variedad', value: selectedDespacho.pedidoDetalle?.codigo || 'Sin variedad' },
                { label: 'Empaque', value: <StatusBadge value={selectedDespacho.modalidad} />, tone: 'purple' },
                { label: 'Cantidad', value: `${numberCompact(selectedDespacho.cantidadDespachada || 0)} plantas`, tone: 'blue' },
                { label: 'Carga', value: selectedDespacho.cargaDespacho?.codigo || 'Pendiente de consolidar', tone: 'orange' },
                { label: 'Estado', value: <StatusBadge value={selectedDespacho.estado} /> }
              ]}
            />
            <section className="drawer-section">
              <h3>Destino y guía</h3>
              <p><strong>Destino:</strong> {selectedDespacho.destino || 'Sin destino registrado'}</p>
              <p><strong>Guía:</strong> {selectedDespacho.guiaRemision || 'Sin guía registrada'}</p>
              <p>{selectedDespacho.observacion || 'No se registraron observaciones.'}</p>
            </section>
          </>
        ) : null}
      </DetailDrawer>

      <DetailDrawer
        open={Boolean(selectedCarga)}
        title={selectedCarga?.codigo || 'Detalle de carga'}
        subtitle={selectedCarga ? cargaStatusDescription(upper(selectedCarga.estado)) : 'Carga de tráiler'}
        onClose={() => setSelectedCarga(null)}
        actions={selectedCarga && upper(selectedCarga.estado) === 'PREPARADA' ? <button type="button" className="action-button" onClick={() => { setAddingLineTo(selectedCarga); setSelectedLineId(0); setLineError(null); }}><Plus size={15} /> Agregar línea</button> : null}
      >
        {selectedCarga ? (
          <>
            <InfoGrid items={[
              { label: 'Pedido', value: selectedCarga.pedido?.codigo || 'Sin pedido', tone: 'green' },
              { label: 'Fecha de carga', value: dateShort(selectedCarga.fechaCarga) },
              { label: 'Tráiler', value: selectedCarga.vehiculo, tone: 'blue' },
              { label: 'Destino', value: selectedCarga.destino || 'Sin destino' },
              { label: 'Contenido', value: `${numberCompact(selectedCarga.totalPlantas || 0)} plantas · ${selectedCarga.totalUnidades || 0} unidades`, tone: 'purple' },
              { label: 'Estado', value: <StatusBadge value={selectedCarga.estado} />, tone: 'orange' }
            ]} />
            <section className="drawer-section">
              <h3>Líneas cargadas</h3>
              {selectedCarga.lineas.length > 0 ? (
                <div className="data-table-wrap">
                  <table className="data-table data-table--compact">
                    <thead><tr><th>Variedad</th><th>Empaque</th><th>Unidades</th><th>Plantas</th><th /></tr></thead>
                    <tbody>{selectedCarga.lineas.map((line) => <tr key={line.id}><td>{line.pedidoDetalle?.codigo || `D-${line.id}`}</td><td><StatusBadge value={line.modalidad} /></td><td>{line.unidadesEmpaque || 0}</td><td>{numberCompact(line.cantidadDespachada || 0)}</td><td>{upper(selectedCarga.estado) === 'PREPARADA' ? <button type="button" className="icon-action" aria-label="Retirar línea" disabled={lineSaving} onClick={() => void removeLine(selectedCarga, line)}><X size={14} /></button> : null}</td></tr>)}</tbody>
                  </table>
                </div>
              ) : <p className="table-muted">Todavía no se agregaron líneas a esta carga.</p>}
            </section>
            <section className="drawer-section">
              <h3>Guía y observación</h3>
              <p><strong>Guía:</strong> {selectedCarga.guiaRemision || 'Sin guía registrada'}</p>
              <p>{selectedCarga.observacion || 'No se registraron observaciones.'}</p>
            </section>
          </>
        ) : null}
      </DetailDrawer>

      <Modal open={Boolean(editingDespacho)} title="Editar línea de despacho" description="Actualiza el empaque, la fecha o la validación antes de incluirla en el tráiler." onClose={() => setEditingDespacho(null)}>
        {editingDespacho ? <DespachoForm lotesTrazables={lotesTrazables} clasificaciones={clasificaciones} pedidos={pedidos} empaques={empaques} validaciones={validaciones} initialData={toPayload(editingDespacho)} submitLabel="Guardar cambios" onSubmit={update} onCancel={() => setEditingDespacho(null)} /> : null}
      </Modal>

      <Modal open={cargaFormOpen} title="Preparar carga de tráiler" description="Crea el manifiesto físico antes de agregar las líneas por variedad." onClose={() => setCargaFormOpen(false)}>
        <CargaDespachoForm pedidos={pedidos} onSubmit={createCarga} onCancel={() => setCargaFormOpen(false)} />
      </Modal>

      <Modal open={Boolean(addingLineTo)} title={addingLineTo ? `Agregar línea a ${addingLineTo.codigo}` : 'Agregar línea'} description="Solo se muestran líneas registradas del mismo pedido y de la misma fecha de carga." onClose={() => { if (!lineSaving) setAddingLineTo(null); }}>
        <form className="form-shell" onSubmit={addLine}>
          {lineError ? <FormMessage>{lineError}</FormMessage> : null}
          {availableLines.length === 0 ? <FormPrerequisite title="No hay líneas compatibles" description="Prepara una línea registrada del mismo pedido y fecha, sin otra carga asignada." /> : null}
          <FormSection title="Línea preparada" description="La línea conserva el empaque y la variedad solicitada dentro del mismo pedido." icon={<PackageCheck size={18} />}>
            <div className="form-grid">
              <label><span>Línea disponible</span><select value={selectedLineId} onChange={(event) => setSelectedLineId(Number(event.target.value))} disabled={availableLines.length === 0} required><option value={0} disabled>Selecciona una línea</option>{availableLines.map((line) => <option key={line.id} value={line.id}>D-{String(line.id).padStart(4, '0')} · {line.pedidoDetalle?.codigo || 'Variedad'} · {line.unidadesEmpaque || 0} unidades · {line.cantidadDespachada || 0} plantas</option>)}</select></label>
            </div>
          </FormSection>
          <footer className="form-actions form-actions--sticky"><button type="button" className="ghost-button" onClick={() => setAddingLineTo(null)} disabled={lineSaving}>Cancelar</button><button type="submit" className="action-button" disabled={lineSaving || !selectedLineId}>{lineSaving ? 'Agregando...' : 'Agregar a la carga'}</button></footer>
        </form>
      </Modal>

      <ConfirmDialog
        open={Boolean(pendingCargaStatus)}
        title={pendingCargaStatus?.estado === 'DESPACHADA' ? 'Confirmar salida del tráiler' : pendingCargaStatus?.estado === 'CARGADA' ? 'Marcar tráiler como cargado' : pendingCargaStatus?.estado === 'PREPARADA' ? 'Reabrir carga preparada' : 'Cancelar carga de tráiler'}
        description={pendingCargaStatus?.estado === 'DESPACHADA'
          ? 'Se confirmarán todas las líneas de la carga, descontando sus cantidades del pedido y de cada empaque.'
          : pendingCargaStatus?.estado === 'CARGADA'
            ? 'La carga quedará lista físicamente y ya no permitirá agregar ni retirar líneas.'
            : pendingCargaStatus?.estado === 'PREPARADA'
              ? 'La carga volverá a permitir cambios antes de la salida.'
              : 'Las líneas se retirarán de la carga y volverán a quedar disponibles como despachos preparados.'}
        confirmLabel={pendingCargaStatus?.estado === 'DESPACHADA' ? 'Confirmar salida' : pendingCargaStatus?.estado === 'CARGADA' ? 'Confirmar carga' : pendingCargaStatus?.estado === 'PREPARADA' ? 'Reabrir carga' : 'Cancelar carga'}
        tone={pendingCargaStatus?.estado === 'CANCELADA' ? 'danger' : pendingCargaStatus?.estado === 'DESPACHADA' ? 'success' : 'warning'}
        loading={confirming}
        onCancel={() => setPendingCargaStatus(null)}
        onConfirm={confirmCargaStatus}
      />
    </main>
  );
}
