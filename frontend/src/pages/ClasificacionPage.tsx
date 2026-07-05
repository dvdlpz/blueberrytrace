import { useMemo, useState } from 'react';
import { AlertTriangle, CheckCircle2, Download, Eye, Medal, Pencil, Plus, RefreshCcw, Tags } from 'lucide-react';
import { ClasificacionForm } from '../components/ClasificacionForm';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { DetailDrawer } from '../components/DetailDrawer';
import { EmptyState } from '../components/EmptyState';
import { FilterToolbar } from '../components/FilterToolbar';
import { InfoGrid } from '../components/InfoGrid';
import { Modal } from '../components/Modal';
import { ModuleHeader } from '../components/ModuleHeader';
import { StatusBadge } from '../components/StatusBadge';
import { blueberryApi } from '../lib/api';
import { downloadCsv } from '../lib/export';
import { dateShort, numberCompact } from '../lib/format';
import { emitToast } from '../lib/uiEvents';
import type { CamaResponse, ClasificacionFormPayload, ClasificacionResponse, JabaResponse, LoteTrazableResponse, ReferenceResponse } from '../types/api';

interface ClasificacionPageProps {
  clasificaciones: ClasificacionResponse[];
  lotes: ReferenceResponse[];
  camas: CamaResponse[];
  lotesTrazables: LoteTrazableResponse[];
  jabas: JabaResponse[];
  onClasificacionesChange: (items: ClasificacionResponse[]) => void;
}

type QualityBucket = 'Primera calidad' | 'Segunda calidad' | 'Tercera calidad' | 'Descarte' | 'Sin criterio';

function bucketOf(item: ClasificacionResponse): QualityBucket {
  const combined = `${item.condicion || ''} ${item.estadoPlanta || ''} ${item.tamano || ''}`.toLowerCase();
  if (/descarte|rechaz|observ|no apt|dañ|enfer/.test(combined)) return 'Descarte';
  if (/primera|export|apta|excelente|grande|óptim|optim/.test(combined)) return 'Primera calidad';
  if (/segunda|buena|mediana|medio/.test(combined)) return 'Segunda calidad';
  if (/tercera|regular|pequeñ|pequen|bajo/.test(combined)) return 'Tercera calidad';
  return 'Sin criterio';
}

function toPayload(item: ClasificacionResponse): ClasificacionFormPayload {
  return {
    loteTrazableId: item.loteTrazable?.id || 0,
    loteId: item.lote?.id || 0,
    camaId: item.cama?.id || 0,
    jabaId: item.jaba?.id || 0,
    fechaClasificacion: item.fechaClasificacion || new Date().toISOString().slice(0, 10),
    estadoPlanta: item.estadoPlanta || 'Apta',
    tamano: item.tamano || 'Mediana',
    condicion: item.condicion || 'Exportación',
    cantidad: item.cantidad || 1,
    observacion: item.observacion || '',
    estado: item.estado || 'PENDIENTE'
  };
}

export function ClasificacionPage({ clasificaciones, lotes, camas, lotesTrazables, jabas, onClasificacionesChange }: ClasificacionPageProps) {
  const [query, setQuery] = useState('');
  const [loteFilter, setLoteFilter] = useState('TODOS');
  const [statusFilter, setStatusFilter] = useState('TODOS');
  const [creating, setCreating] = useState(false);
  const [selectedClasificacion, setSelectedClasificacion] = useState<ClasificacionResponse | null>(null);
  const [editingClasificacion, setEditingClasificacion] = useState<ClasificacionResponse | null>(null);
  const [pendingStatus, setPendingStatus] = useState<{ item: ClasificacionResponse; estado: string } | null>(null);
  const [confirming, setConfirming] = useState(false);

  const availableStates = useMemo(() => Array.from(new Set(clasificaciones.map((item) => item.estado).filter(Boolean))) as string[], [clasificaciones]);

  const filtered = useMemo(() => {
    const term = query.trim().toLowerCase();
    return clasificaciones.filter((item) => {
      const matchesTerm = !term || [item.lote?.codigo, item.cama?.codigo, item.estadoPlanta, item.tamano, item.condicion, item.estado, item.usuarioRegistro?.nombreCompleto]
        .some((value) => String(value || '').toLowerCase().includes(term));
      const matchesLot = loteFilter === 'TODOS' || String(item.lote?.id || '') === loteFilter;
      const matchesStatus = statusFilter === 'TODOS' || item.estado === statusFilter;
      return matchesTerm && matchesLot && matchesStatus;
    });
  }, [clasificaciones, loteFilter, query, statusFilter]);

  const grouped = useMemo(() => {
    const initial: Record<QualityBucket, number> = {
      'Primera calidad': 0,
      'Segunda calidad': 0,
      'Tercera calidad': 0,
      Descarte: 0,
      'Sin criterio': 0
    };
    return clasificaciones.reduce((acc, item) => {
      acc[bucketOf(item)] += item.cantidad || 0;
      return acc;
    }, initial);
  }, [clasificaciones]);

  async function create(payload: ClasificacionFormPayload) {
    const response = await blueberryApi.createClasificacion(payload);
    onClasificacionesChange(response.items);
    setCreating(false);
    emitToast('success', 'Clasificación registrada', 'El control de calidad fue guardado correctamente.');
  }

  async function update(payload: ClasificacionFormPayload) {
    if (!editingClasificacion) return;
    const response = await blueberryApi.updateClasificacion(editingClasificacion.id, payload);
    onClasificacionesChange(response.items);
    setEditingClasificacion(null);
    setSelectedClasificacion(null);
    emitToast('success', 'Clasificación actualizada', 'Los datos de calidad fueron guardados.');
  }

  async function confirmChangeStatus() {
    if (!pendingStatus) return;
    try {
      setConfirming(true);
      const response = await blueberryApi.changeClasificacionStatus(pendingStatus.item.id, pendingStatus.estado);
      onClasificacionesChange(response.items);
      emitToast('success', 'Estado actualizado', `La clasificación cambió a ${pendingStatus.estado}.`);
      setPendingStatus(null);
    } catch (exception) {
      emitToast('error', 'No se pudo actualizar', exception instanceof Error ? exception.message : 'Ocurrió un error inesperado.');
    } finally {
      setConfirming(false);
    }
  }

  function exportCsv() {
    downloadCsv('blueberrytrace-clasificaciones.csv', [
      'Código', 'Lote', 'Cama', 'Jaba', 'Fecha', 'Criterio', 'Estado planta', 'Tamaño', 'Condición', 'Cantidad', 'Recuperación por riego', 'Responsable', 'Estado'
    ], filtered.map((item) => [
      `CL-${String(item.id).padStart(4, '0')}`,
      item.lote?.codigo || '',
      item.cama?.codigo || '',
      item.jaba?.codigo || '',
      item.fechaClasificacion || '',
      bucketOf(item),
      item.estadoPlanta || '',
      item.tamano || '',
      item.condicion || '',
      item.cantidad || 0,
      item.recuperacionRiego?.codigo || '',
      item.usuarioRegistro?.nombreCompleto || '',
      item.estado || ''
    ]));
  }

  const cards = [
    { label: 'Primera calidad', value: grouped['Primera calidad'], tone: 'green', icon: <Medal size={18} /> },
    { label: 'Segunda calidad', value: grouped['Segunda calidad'], tone: 'blue', icon: <CheckCircle2 size={18} /> },
    { label: 'Tercera calidad', value: grouped['Tercera calidad'], tone: 'orange', icon: <Tags size={18} /> },
    { label: 'Descarte', value: grouped.Descarte, tone: 'red', icon: <AlertTriangle size={18} /> }
  ];
  const qualityTotal = cards.reduce((sum, item) => sum + item.value, 0);
  const filteredTotal = filtered.reduce((sum, item) => sum + (item.cantidad || 0), 0);

  return (
    <main className="content-grid classification-screen">
      <ModuleHeader
        eyebrow="Calidad"
        title="Control de clasificación"
        description="Clasificación por tamaño, estado y condición de plantas registradas."
        icon={<Tags size={21} />}
        tone="purple"
        actions={<button type="button" className="action-button" onClick={() => setCreating(true)}><Plus size={16} /> Nueva clasificación</button>}
      />

      <section className="quality-card-grid quality-card-grid--real">
        {cards.map((card) => {
          const percent = qualityTotal === 0 ? 0 : Math.round((card.value / qualityTotal) * 100);
          return (
            <article key={card.label} className={`quality-card quality-card--${card.tone}`}>
              <div className="quality-card__header">
                <span className="quality-card__label"><i>{card.icon}</i>{card.label}</span>
                <strong>{percent}%</strong>
              </div>
              <h3>{numberCompact(card.value)}</h3>
              <div className="progress-track"><span style={{ width: `${percent}%` }} /></div>
            </article>
          );
        })}
      </section>

      <section className="panel-card">
        <div className="module-toolbar-card module-toolbar-card--filters">
          <FilterToolbar value={query} onChange={setQuery} placeholder="Buscar lote, cama, condición o responsable..." />
          <select value={loteFilter} onChange={(event) => setLoteFilter(event.target.value)}>
            <option value="TODOS">Todos los lotes</option>
            {lotes.map((lote) => <option key={lote.id} value={lote.id}>{lote.codigo}</option>)}
          </select>
          <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
            <option value="TODOS">Todos los estados</option>
            {availableStates.map((estado) => <option key={estado} value={estado}>{estado}</option>)}
          </select>
          <button type="button" className="ghost-button" onClick={exportCsv} disabled={filtered.length === 0}><Download size={15} /> Exportar CSV</button>
        </div>

        {filtered.length > 0 ? (
          <div className="data-table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Lote</th>
                  <th>Cama</th>
                  <th>Jaba</th>
                  <th>Fecha</th>
                  <th>Criterio</th>
                  <th>Tamaño</th>
                  <th>Condición</th>
                  <th>Cantidad</th>
                  <th>Recuperación</th>
                  <th>Responsable</th>
                  <th>Estado</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {filtered.map((item) => (
                  <tr key={item.id}>
                    <td><strong className="table-code">CL-{String(item.id).padStart(4, '0')}</strong></td>
                    <td>{item.lote?.codigo || 'Sin lote'}</td>
                    <td>{item.cama?.codigo || 'Sin cama'}</td>
                    <td>{item.jaba?.codigo || 'Sin jaba'}</td>
                    <td>{dateShort(item.fechaClasificacion)}</td>
                    <td><StatusBadge value={bucketOf(item)} /></td>
                    <td>{item.tamano || 'Sin tamaño'}</td>
                    <td>{item.condicion || 'Sin condición'}</td>
                    <td><strong>{numberCompact(item.cantidad || 0)}</strong></td>
                    <td>{item.recuperacionRiego ? <span className="table-subtle">Riego #{item.recuperacionRiego.id}</span> : '—'}</td>
                    <td>{item.usuarioRegistro?.nombreCompleto || 'Sin responsable'}</td>
                    <td><StatusBadge value={item.estado} /></td>
                    <td>
                      <div className="icon-actions">
                        <button type="button" className="icon-action" onClick={() => setSelectedClasificacion(item)}><Eye size={15} /></button>
                        <button type="button" className="icon-action" onClick={() => setEditingClasificacion(item)}><Pencil size={15} /></button>
                        <button type="button" className="icon-action" onClick={() => setPendingStatus({ item, estado: 'VALIDADA' })}><RefreshCcw size={15} /></button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState
            icon={<Tags size={26} />}
            title="Sin clasificaciones para mostrar"
            description="Registra controles de calidad o ajusta los filtros para visualizar resultados."
            compact
          />
        )}
        <div className="table-footer-note">{filtered.length} registros · {numberCompact(filteredTotal)} plantas clasificadas</div>
      </section>

      <DetailDrawer
        open={Boolean(selectedClasificacion)}
        title={selectedClasificacion ? `CL-${String(selectedClasificacion.id).padStart(4, '0')}` : 'Detalle de clasificación'}
        subtitle={selectedClasificacion?.lote?.codigo || 'Control de calidad'}
        onClose={() => setSelectedClasificacion(null)}
        actions={selectedClasificacion ? <button type="button" className="action-button" onClick={() => setEditingClasificacion(selectedClasificacion)}><Pencil size={15} /> Editar</button> : null}
      >
        {selectedClasificacion ? (
          <>
            <InfoGrid
              items={[
                { label: 'Lote', value: selectedClasificacion.lote?.codigo || 'Sin lote', tone: 'green' },
                { label: 'Cama', value: selectedClasificacion.cama?.codigo || 'Sin cama' },
                { label: 'Jaba', value: selectedClasificacion.jaba?.codigo || 'Sin jaba' },
                { label: 'Cantidad', value: numberCompact(selectedClasificacion.cantidad || 0), tone: 'blue' },
                { label: 'Criterio', value: <StatusBadge value={bucketOf(selectedClasificacion)} /> },
                { label: 'Tamaño', value: selectedClasificacion.tamano || 'No definido', tone: 'purple' },
                { label: 'Estado', value: <StatusBadge value={selectedClasificacion.estado} />, tone: 'orange' },
                { label: 'Recuperación', value: selectedClasificacion.recuperacionRiego ? `Riego #${selectedClasificacion.recuperacionRiego.id}` : 'No requerida' }
              ]}
            />
            <section className="drawer-section">
              <h3>Condición y observación</h3>
              <p><strong>Condición:</strong> {selectedClasificacion.condicion || 'Sin condición registrada'}</p>
              <p>{selectedClasificacion.observacion || 'No se registraron observaciones.'}</p>
            </section>
          </>
        ) : null}
      </DetailDrawer>

      <Modal open={creating} title="Nueva clasificación" description="Registra el resultado de control de calidad por lote, cama y jaba." onClose={() => setCreating(false)}>
        <ClasificacionForm lotesTrazables={lotesTrazables} jabas={jabas} onSubmit={create} onCancel={() => setCreating(false)} />
      </Modal>

      <Modal open={Boolean(editingClasificacion)} title="Editar clasificación" description="Actualiza datos de control de calidad." onClose={() => setEditingClasificacion(null)}>
        {editingClasificacion ? <ClasificacionForm lotesTrazables={lotesTrazables} jabas={jabas} initialData={toPayload(editingClasificacion)} submitLabel="Guardar cambios" onSubmit={update} onCancel={() => setEditingClasificacion(null)} /> : null}
      </Modal>

      <ConfirmDialog
        open={Boolean(pendingStatus)}
        title="Validar clasificación"
        description="Se actualizará el estado de la clasificación seleccionada."
        confirmLabel="Validar"
        tone="success"
        loading={confirming}
        onCancel={() => setPendingStatus(null)}
        onConfirm={confirmChangeStatus}
      />
    </main>
  );
}
