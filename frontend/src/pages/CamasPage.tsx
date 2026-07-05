import { useMemo, useState } from 'react';
import { CheckCircle2, Eye, Layers3, PackageCheck, Pencil, Plus, RotateCcw } from 'lucide-react';
import { CamaForm } from '../components/CamaForm';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { DataTable } from '../components/DataTable';
import { DetailDrawer } from '../components/DetailDrawer';
import { FilterToolbar } from '../components/FilterToolbar';
import { InfoGrid } from '../components/InfoGrid';
import { Modal } from '../components/Modal';
import { ModuleHeader } from '../components/ModuleHeader';
import { StatusBadge } from '../components/StatusBadge';
import { blueberryApi } from '../lib/api';
import { numberCompact } from '../lib/format';
import { emitToast } from '../lib/uiEvents';
import type { CamaFormPayload, CamaResponse, ReferenceResponse } from '../types/api';

interface CamasPageProps {
  camas: CamaResponse[];
  lotes: ReferenceResponse[];
  onCamasChange: (items: CamaResponse[]) => void;
}

function toPayload(cama: CamaResponse): CamaFormPayload {
  return {
    codigo: cama.codigo || '',
    descripcion: cama.descripcion || '',
    capacidadReferencial: cama.capacidadReferencial || 1,
    estado: cama.estado || 'ACTIVA',
    loteId: cama.lote?.id || 0
  };
}

export function CamasPage({ camas, lotes, onCamasChange }: CamasPageProps) {
  const [query, setQuery] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedCama, setSelectedCama] = useState<CamaResponse | null>(null);
  const [editingCama, setEditingCama] = useState<CamaResponse | null>(null);
  const [pendingStatus, setPendingStatus] = useState<CamaResponse | null>(null);
  const [confirming, setConfirming] = useState(false);

  const filtered = useMemo(() => {
    const term = query.trim().toLowerCase();
    if (!term) return camas;
    return camas.filter((cama) => [cama.codigo, cama.descripcion, cama.estado, cama.lote?.codigo]
      .some((value) => String(value || '').toLowerCase().includes(term)));
  }, [camas, query]);

  const activas = camas.filter((cama) => cama.estado === 'ACTIVA').length;
  const capacidad = camas.reduce((total, cama) => total + (cama.capacidadReferencial || 0), 0);

  async function createCama(payload: CamaFormPayload) {
    const response = await blueberryApi.createCama(payload);
    onCamasChange(response.items);
    setModalOpen(false);
    emitToast('success', 'Cama creada', `La cama ${payload.codigo} fue registrada correctamente.`);
  }

  async function updateCama(payload: CamaFormPayload) {
    if (!editingCama) return;
    const response = await blueberryApi.updateCama(editingCama.id, payload);
    onCamasChange(response.items);
    setEditingCama(null);
    setSelectedCama(null);
    emitToast('success', 'Cama actualizada', `Los datos de ${payload.codigo} fueron guardados.`);
  }

  async function confirmToggleStatus() {
    if (!pendingStatus) return;
    try {
      setConfirming(true);
      const response = await blueberryApi.toggleCamaStatus(pendingStatus.id);
      onCamasChange(response.items);
      emitToast('success', 'Estado actualizado', `El estado de ${pendingStatus.codigo} fue modificado.`);
      setPendingStatus(null);
    } catch (exception) {
      emitToast('error', 'No se pudo cambiar el estado', exception instanceof Error ? exception.message : 'Ocurrió un error inesperado.');
    } finally {
      setConfirming(false);
    }
  }

  return (
    <main className="content-grid">
      <ModuleHeader
        eyebrow="Infraestructura"
        title="Camas productivas"
        description="Seguimiento de camas por invernadero, capacidad referencial y estado operativo."
        icon={<Layers3 size={21} />}
        tone="blue"
        actions={<button className="action-button" type="button" onClick={() => setModalOpen(true)}><Plus size={17} /> Nueva cama</button>}
      />

      <section className="summary-strip summary-strip--three">
        <article className="summary-pill summary-pill--green"><span className="summary-pill__icon"><Layers3 size={18} /></span><strong>{camas.length}</strong><span>Camas registradas</span><small>en invernaderos</small></article>
        <article className="summary-pill summary-pill--blue"><span className="summary-pill__icon"><CheckCircle2 size={18} /></span><strong>{activas}</strong><span>Camas activas</span><small>{numberCompact(camas.length - activas)} inactivas</small></article>
        <article className="summary-pill summary-pill--orange"><span className="summary-pill__icon"><PackageCheck size={18} /></span><strong>{numberCompact(capacidad)}</strong><span>Capacidad total</span><small>referencial de plantas</small></article>
      </section>

      <section className="panel-card">
        <FilterToolbar value={query} onChange={setQuery} placeholder="Buscar cama, lote o estado" />
        <DataTable<CamaResponse>
          title="Listado de camas"
          description="Capacidad operativa disponible por cama e invernadero."
          items={filtered}
          emptyTitle={camas.length === 0 ? 'Aún no hay camas registradas' : 'No se encontraron camas'}
          emptyDescription={camas.length === 0 ? 'Registra una cama cuando el invernadero ya esté disponible.' : 'Ajusta la búsqueda para volver a ver las camas registradas.'}
          emptyAction={camas.length === 0 ? <button type="button" className="action-button" onClick={() => setModalOpen(true)}><Plus size={16} /> Registrar cama</button> : undefined}
          countLabel="camas"
          columns={[
            { key: 'codigo', label: 'Código' },
            { key: 'lote', label: 'Lote', render: (item) => item.lote?.codigo || 'Sin lote' },
            { key: 'descripcion', label: 'Descripción' },
            { key: 'capacidadReferencial', label: 'Capacidad', render: (item) => numberCompact(item.capacidadReferencial || 0) },
            { key: 'estado', label: 'Estado', render: (item) => <StatusBadge value={item.estado} /> },
            { key: 'acciones', label: 'Acciones', render: (item) => (
              <div className="icon-actions">
                <button type="button" className="icon-action" title="Ver detalle" onClick={() => setSelectedCama(item)}><Eye size={15} /></button>
                <button type="button" className="icon-action" title="Editar" onClick={() => setEditingCama(item)}><Pencil size={15} /></button>
                <button type="button" className="mini-button" onClick={() => setPendingStatus(item)}><RotateCcw size={14} /> Estado</button>
              </div>
            ) }
          ]}
        />
      </section>

      <DetailDrawer open={Boolean(selectedCama)} title={selectedCama?.codigo || 'Detalle de cama'} subtitle={selectedCama?.descripcion || 'Información de cama'} onClose={() => setSelectedCama(null)} actions={selectedCama ? <button type="button" className="action-button" onClick={() => setEditingCama(selectedCama)}><Pencil size={15} /> Editar cama</button> : null}>
        {selectedCama ? (
          <InfoGrid
            items={[
              { label: 'Código', value: selectedCama.codigo, tone: 'green' },
              { label: 'Lote', value: selectedCama.lote?.codigo || 'Sin lote', tone: 'purple' },
              { label: 'Capacidad', value: numberCompact(selectedCama.capacidadReferencial || 0), tone: 'blue' },
              { label: 'Estado', value: <StatusBadge value={selectedCama.estado} />, tone: 'orange' },
              { label: 'Responsable', value: selectedCama.usuarioRegistro?.nombreCompleto || 'Sin asignar' }
            ]}
          />
        ) : null}
      </DetailDrawer>

      <Modal open={modalOpen} title="Nueva cama" description="Asocia una cama productiva a un invernadero existente." onClose={() => setModalOpen(false)}>
        <CamaForm lotes={lotes} onSubmit={createCama} onCancel={() => setModalOpen(false)} />
      </Modal>

      <Modal open={Boolean(editingCama)} title="Editar cama" description="Actualiza capacidad, estado y lote asociado." onClose={() => setEditingCama(null)}>
        {editingCama ? <CamaForm lotes={lotes} initialData={toPayload(editingCama)} submitLabel="Guardar cambios" onSubmit={updateCama} onCancel={() => setEditingCama(null)} /> : null}
      </Modal>

      <ConfirmDialog
        open={Boolean(pendingStatus)}
        title="Cambiar estado de cama"
        description={`Se alternará el estado operativo de ${pendingStatus?.codigo || 'la cama seleccionada'}.`}
        confirmLabel="Cambiar estado"
        loading={confirming}
        onCancel={() => setPendingStatus(null)}
        onConfirm={confirmToggleStatus}
      />
    </main>
  );
}
