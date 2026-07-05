import { useMemo, useState } from 'react';
import { CheckCircle2, ClipboardList, Clock3, Eye, Factory, PackageCheck, Pencil, Plus, RefreshCcw, Trash2 } from 'lucide-react';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { DetailDrawer } from '../components/DetailDrawer';
import { EmptyState } from '../components/EmptyState';
import { FilterToolbar } from '../components/FilterToolbar';
import { InfoGrid } from '../components/InfoGrid';
import { LoteForm } from '../components/LoteForm';
import { Modal } from '../components/Modal';
import { ModuleHeader } from '../components/ModuleHeader';
import { StatusBadge } from '../components/StatusBadge';
import { blueberryApi } from '../lib/api';
import { dateShort, numberCompact } from '../lib/format';
import { emitToast } from '../lib/uiEvents';
import type { CamaResponse, LoteFormPayload, LoteResponse, SiembraResponse } from '../types/api';

interface LotesPageProps {
  lotes: LoteResponse[];
  camas: CamaResponse[];
  siembras: SiembraResponse[];
  onLotesChange: (items: LoteResponse[]) => void;
}

interface PendingAction {
  title: string;
  description: string;
  confirmLabel: string;
  tone?: 'warning' | 'danger' | 'success';
  run: () => Promise<void>;
}

const statusTabs = ['TODOS', 'ACTIVO', 'INACTIVO', 'MANTENIMIENTO', 'ARCHIVADO'] as const;

function toPayload(lote: LoteResponse): LoteFormPayload {
  return {
    codigo: lote.codigo || '',
    descripcion: lote.descripcion || '',
    cultivo: lote.cultivo || 'Arándano',
    variedad: lote.variedad || '',
    fechaRegistro: lote.fechaRegistro || new Date().toISOString().slice(0, 10),
    observacion: lote.observacion || '',
    estado: lote.estado || 'ACTIVO'
  };
}

export function LotesPage({ lotes, camas, siembras, onLotesChange }: LotesPageProps) {
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState<(typeof statusTabs)[number]>('TODOS');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingLote, setEditingLote] = useState<LoteResponse | null>(null);
  const [selectedLote, setSelectedLote] = useState<LoteResponse | null>(null);
  const [pendingAction, setPendingAction] = useState<PendingAction | null>(null);
  const [confirming, setConfirming] = useState(false);

  const filtered = useMemo(() => {
    const term = query.trim().toLowerCase();
    return lotes.filter((lote) => {
      const matchesQuery = !term || [lote.codigo, lote.descripcion, lote.cultivo, lote.variedad, lote.estado]
        .some((value) => String(value || '').toLowerCase().includes(term));
      const matchesStatus = status === 'TODOS' || String(lote.estado || '').toUpperCase() === status;
      return matchesQuery && matchesStatus;
    });
  }, [lotes, query, status]);

  async function createLote(payload: LoteFormPayload) {
    const response = await blueberryApi.createLote(payload);
    onLotesChange(response.items);
    setModalOpen(false);
    emitToast('success', 'Lote creado', `El lote ${payload.codigo} fue registrado correctamente.`);
  }

  async function updateLote(payload: LoteFormPayload) {
    if (!editingLote) return;
    const response = await blueberryApi.updateLote(editingLote.id, payload);
    onLotesChange(response.items);
    setEditingLote(null);
    setSelectedLote(null);
    emitToast('success', 'Lote actualizado', `Los datos de ${payload.codigo} fueron guardados.`);
  }

  function confirmToggleStatus(lote: LoteResponse) {
    setPendingAction({
      title: 'Cambiar estado del lote',
      description: `Se actualizará el estado operativo de ${lote.codigo}. Esta acción quedará reflejada en la trazabilidad.`,
      confirmLabel: 'Cambiar estado',
      tone: 'warning',
      run: async () => {
        const response = await blueberryApi.toggleLoteStatus(lote.id);
        onLotesChange(response.items);
        emitToast('success', 'Estado actualizado', `El estado de ${lote.codigo} fue modificado.`);
      }
    });
  }

  function confirmDelete(lote: LoteResponse) {
    setPendingAction({
      title: 'Archivar lote',
      description: `El lote ${lote.codigo} será archivado de forma lógica. La trazabilidad histórica se conserva y el lote dejará de operar como activo.`,
      confirmLabel: 'Archivar lote',
      tone: 'danger',
      run: async () => {
        const response = await blueberryApi.deleteLote(lote.id);
        onLotesChange(response.items);
        setSelectedLote(null);
        emitToast('warning', 'Lote archivado', `${lote.codigo} fue archivado sin eliminación física.`);
      }
    });
  }

  async function runPendingAction() {
    if (!pendingAction) return;
    try {
      setConfirming(true);
      await pendingAction.run();
      setPendingAction(null);
    } catch (exception) {
      emitToast('error', 'No se pudo completar la acción', exception instanceof Error ? exception.message : 'Ocurrió un error inesperado.');
    } finally {
      setConfirming(false);
    }
  }

  const cards = [
    { label: 'Total Lotes', value: lotes.length, tone: 'default', icon: <Factory size={18} /> },
    { label: 'Activos', value: lotes.filter((item) => (item.estado || '').toUpperCase() === 'ACTIVO').length, tone: 'green', icon: <CheckCircle2 size={18} /> },
    { label: 'Mantenimiento', value: lotes.filter((item) => (item.estado || '').toUpperCase() === 'MANTENIMIENTO').length, tone: 'blue', icon: <ClipboardList size={18} /> },
    { label: 'Inactivos', value: lotes.filter((item) => (item.estado || '').toUpperCase() === 'INACTIVO').length, tone: 'purple', icon: <PackageCheck size={18} /> },
    { label: 'Archivados', value: lotes.filter((item) => (item.estado || '').toUpperCase() === 'ARCHIVADO').length, tone: 'orange', icon: <Clock3 size={18} /> }
  ];

  return (
    <main className="content-grid">
      <ModuleHeader
        eyebrow="Producción"
        title="Gestión de Lotes e Invernaderos"
        description="Control de invernaderos, camas y jabas de arándano."
        icon={<Factory size={21} />}
        tone="green"
        actions={<button className="action-button" type="button" onClick={() => setModalOpen(true)}><Plus size={16} /> Nuevo lote</button>}
      />

      <section className="summary-strip summary-strip--five">
        {cards.map((card) => (
          <article key={card.label} className={`summary-pill summary-pill--${card.tone}`}>
            <span className="summary-pill__icon">{card.icon}</span>
            <strong>{card.value}</strong>
            <span>{card.label}</span>
          </article>
        ))}
      </section>

      <section className="panel-card">
        <div className="module-toolbar-card">
          <FilterToolbar value={query} onChange={setQuery} placeholder="Buscar lote o invernadero..." />
          <div className="segmented-tabs">
            {statusTabs.map((tab) => (
              <button key={tab} type="button" className={tab === status ? 'segmented-tabs__item segmented-tabs__item--active' : 'segmented-tabs__item'} onClick={() => setStatus(tab)}>
                {tab === 'TODOS' ? 'Todos' : tab === 'ACTIVO' ? 'Activo' : tab === 'INACTIVO' ? 'Inactivo' : tab === 'MANTENIMIENTO' ? 'Mantenimiento' : 'Archivado'}
              </button>
            ))}
          </div>
        </div>

        {filtered.length > 0 ? (
          <>
            <div className="data-table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Lote</th>
                    <th>Invernadero</th>
                    <th>Variedad</th>
                    <th>Camas</th>
                    <th>Plantas</th>
                    <th>Inicio</th>
                    <th>Supervisor</th>
                    <th>Estado</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((lote) => {
                    const camasCount = camas.filter((cama) => cama.lote?.id === lote.id).length;
                    const plantas = siembras.filter((siembra) => siembra.lote?.id === lote.id).reduce((total, siembra) => total + (siembra.cantidadRegistrada || 0), 0);
                    return (
                      <tr key={lote.id}>
                        <td><strong className="table-code">{lote.codigo}</strong></td>
                        <td>{lote.descripcion || 'Sin descripción'}</td>
                        <td>{lote.variedad || 'No definida'}</td>
                        <td>{camasCount}</td>
                        <td>{plantas ? numberCompact(plantas) : '—'}</td>
                        <td>{dateShort(lote.fechaRegistro)}</td>
                        <td>{lote.usuarioRegistro?.nombreCompleto || 'Sin asignar'}</td>
                        <td><StatusBadge value={lote.estado} /></td>
                        <td>
                          <div className="icon-actions">
                            <button type="button" className="icon-action" title="Ver información" onClick={() => setSelectedLote(lote)}><Eye size={15} /></button>
                            <button type="button" className="icon-action" title="Editar" onClick={() => setEditingLote(lote)}><Pencil size={15} /></button>
                            <button type="button" className="icon-action" title="Cambiar estado" onClick={() => confirmToggleStatus(lote)}><RefreshCcw size={15} /></button>
                            <button type="button" className="icon-action icon-action--danger" title="Archivar" onClick={() => confirmDelete(lote)}><Trash2 size={15} /></button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            <div className="table-footer-note">Mostrando {filtered.length} de {lotes.length} lotes registrados</div>
          </>
        ) : (
          <EmptyState
            icon={<Factory size={28} />}
            title={lotes.length === 0 ? 'Aún no hay invernaderos registrados' : 'No se encontraron invernaderos'}
            description={lotes.length === 0 ? 'Registra el primer invernadero para organizar camas y siembras.' : 'Ajusta la búsqueda o el estado para volver a ver los registros.'}
            action={lotes.length === 0 ? <button type="button" className="action-button" onClick={() => setModalOpen(true)}><Plus size={16} /> Registrar invernadero</button> : undefined}
          />
        )}
      </section>

      <DetailDrawer
        open={Boolean(selectedLote)}
        title={selectedLote?.codigo || 'Detalle del lote'}
        subtitle={selectedLote?.descripcion || 'Información operativa del lote'}
        onClose={() => setSelectedLote(null)}
        actions={selectedLote ? (
          <div className="button-group">
            <button type="button" className="ghost-button" onClick={() => setEditingLote(selectedLote)}><Pencil size={15} /> Editar</button>
            <button type="button" className="action-button" onClick={() => confirmToggleStatus(selectedLote)}><RefreshCcw size={15} /> Cambiar estado</button>
          </div>
        ) : null}
      >
        {selectedLote ? (
          <>
            <InfoGrid
              items={[
                { label: 'Código', value: selectedLote.codigo, tone: 'green' },
                { label: 'Cultivo', value: selectedLote.cultivo || 'No definido' },
                { label: 'Variedad', value: selectedLote.variedad || 'No definida', tone: 'purple' },
                { label: 'Estado', value: <StatusBadge value={selectedLote.estado} />, tone: 'blue' },
                { label: 'Fecha de registro', value: dateShort(selectedLote.fechaRegistro) },
                { label: 'Supervisor', value: selectedLote.usuarioRegistro?.nombreCompleto || 'Sin asignar' }
              ]}
            />
            <section className="drawer-section">
              <h3>Observación</h3>
              <p>{selectedLote.observacion || 'No se registraron observaciones para este lote.'}</p>
            </section>
          </>
        ) : null}
      </DetailDrawer>

      <Modal open={modalOpen} title="Nuevo lote" description="Registra un lote productivo o invernadero." onClose={() => setModalOpen(false)}>
        <LoteForm onSubmit={createLote} onCancel={() => setModalOpen(false)} />
      </Modal>

      <Modal open={Boolean(editingLote)} title="Editar lote" description="Actualiza datos operativos del invernadero." onClose={() => setEditingLote(null)}>
        {editingLote ? <LoteForm initialData={toPayload(editingLote)} submitLabel="Guardar cambios" onSubmit={updateLote} onCancel={() => setEditingLote(null)} /> : null}
      </Modal>

      <ConfirmDialog
        open={Boolean(pendingAction)}
        title={pendingAction?.title || ''}
        description={pendingAction?.description || ''}
        confirmLabel={pendingAction?.confirmLabel}
        tone={pendingAction?.tone}
        loading={confirming}
        onCancel={() => setPendingAction(null)}
        onConfirm={runPendingAction}
      />
    </main>
  );
}
