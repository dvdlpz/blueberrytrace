import { useMemo, useState } from 'react';
import { CheckCircle2, ClipboardCheck, Download, GitBranch, Leaf, Pencil, Plus, RotateCcw, Trash2 } from 'lucide-react';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { DataTable } from '../components/DataTable';
import { EmptyState } from '../components/EmptyState';
import { Modal } from '../components/Modal';
import { ModuleHeader } from '../components/ModuleHeader';
import { ProcesoForm } from '../components/ProcesoForm';
import { StatusBadge } from '../components/StatusBadge';
import { blueberryApi } from '../lib/api';
import { downloadCsv } from '../lib/export';
import { dateShort, numberCompact } from '../lib/format';
import { emitToast } from '../lib/uiEvents';
import type {
  CamaResponse,
  JabaResponse,
  FormalizacionFormPayload,
  FormalizacionResponse,
  ProcesoOperativoResponse,
  ReferenceResponse,
  SiembraResponse,
  LoteTrazableResponse,
  UniformizacionFormPayload,
  UniformizacionResponse
} from '../types/api';

interface ProcesosPageProps {
  mode: 'uniformizacion' | 'formalizacion';
  procesos: ProcesoOperativoResponse | null;
  lotes: ReferenceResponse[];
  camas: CamaResponse[];
  siembras: SiembraResponse[];
  lotesTrazables: LoteTrazableResponse[];
  jabas: JabaResponse[];
  onProcesosChange: (items: ProcesoOperativoResponse) => void;
}

export function ProcesosPage({ mode, procesos, lotes, camas, siembras, lotesTrazables, jabas, onProcesosChange }: ProcesosPageProps) {
  const [modal, setModal] = useState<'uniformizacion' | 'formalizacion' | null>(null);
  const [editingUniformizacion, setEditingUniformizacion] = useState<UniformizacionResponse | null>(null);
  const [editingFormalizacion, setEditingFormalizacion] = useState<FormalizacionResponse | null>(null);
  const [pendingStatus, setPendingStatus] = useState<{ type: 'uniformizacion' | 'formalizacion'; id: number; code: string } | null>(null);
  const [pendingDelete, setPendingDelete] = useState<{ type: 'uniformizacion' | 'formalizacion'; id: number; code: string } | null>(null);
  const [confirming, setConfirming] = useState(false);

  const uniformizaciones = procesos?.uniformizaciones.items || [];
  const formalizaciones = procesos?.formalizaciones.items || [];
  const isUniformizacion = mode === 'uniformizacion';

  const pageCopy = isUniformizacion
    ? {
      eyebrow: 'Proceso productivo',
      title: 'Uniformizaciones',
      description: 'Controla la nivelación y selección operativa de plantas por lote y cama.',
      icon: <Leaf size={21} />,
      tone: 'green' as const,
      addLabel: 'Nueva uniformización'
    }
    : {
      eyebrow: 'Proceso productivo',
      title: 'Formalizaciones',
      description: 'Gestiona jabas completas, plantas formalizadas y cierres del proceso productivo.',
      icon: <ClipboardCheck size={21} />,
      tone: 'orange' as const,
      addLabel: 'Nueva formalización'
    };

  const aggregated = useMemo(() => {
    return lotes.map((lote) => {
      const siembrasLote = siembras.filter((siembra) => siembra.lote?.id === lote.id);
      const planted = siembrasLote.reduce((total, siembra) => total + (siembra.cantidadRegistrada || 0), 0);
      const registrosUniformizacion = uniformizaciones.filter((registro) => registro.lote?.id === lote.id);
      const registrosFormalizacion = formalizaciones.filter((registro) => registro.lote?.id === lote.id);
      const processed = isUniformizacion
        ? registrosUniformizacion.reduce((total, registro) => total + (registro.cantidadUniformizada || 0), 0)
        : registrosFormalizacion.reduce((total, registro) => total + (registro.cantidadPlantas || 0), 0);
      const latest = isUniformizacion ? registrosUniformizacion[0] : registrosFormalizacion[0];
      const progress = planted === 0 ? 0 : Math.min(100, Math.round((processed / planted) * 100));
      return {
        lote,
        planted,
        processed,
        progress,
        latest,
        status: progress >= 100 ? 'COMPLETADO' : progress > 0 ? 'EN PROCESO' : 'PENDIENTE'
      };
    }).filter((item) => item.planted > 0 || item.processed > 0).slice(0, 6);
  }, [formalizaciones, isUniformizacion, lotes, siembras, uniformizaciones]);

  async function createUniformizacion(payload: UniformizacionFormPayload | FormalizacionFormPayload) {
    onProcesosChange(await blueberryApi.createUniformizacion(payload as UniformizacionFormPayload));
    setModal(null);
    emitToast('success', 'Uniformización registrada', 'El proceso fue guardado correctamente.');
  }

  async function createFormalizacion(payload: UniformizacionFormPayload | FormalizacionFormPayload) {
    onProcesosChange(await blueberryApi.createFormalizacion(payload as FormalizacionFormPayload));
    setModal(null);
    emitToast('success', 'Formalización registrada', 'El proceso fue guardado correctamente.');
  }

  async function updateUniformizacion(payload: UniformizacionFormPayload | FormalizacionFormPayload) {
    if (!editingUniformizacion) return;
    onProcesosChange(await blueberryApi.updateUniformizacion(editingUniformizacion.id, payload as UniformizacionFormPayload));
    setEditingUniformizacion(null);
    emitToast('success', 'Uniformización actualizada', 'Los cambios fueron guardados.');
  }

  async function updateFormalizacion(payload: UniformizacionFormPayload | FormalizacionFormPayload) {
    if (!editingFormalizacion) return;
    onProcesosChange(await blueberryApi.updateFormalizacion(editingFormalizacion.id, payload as FormalizacionFormPayload));
    setEditingFormalizacion(null);
    emitToast('success', 'Formalización actualizada', 'Los cambios fueron guardados.');
  }

  function exportCsv() {
    if (isUniformizacion) {
      downloadCsv('blueberrytrace-uniformizaciones.csv', [
        'Proceso', 'Lote', 'Cama', 'Fecha', 'Cantidad inicial', 'Cantidad uniformizada', 'Criterio', 'Estado', 'Responsable'
      ], uniformizaciones.map((item) => [
        'Uniformización',
        item.lote?.codigo || '',
        item.cama?.codigo || '',
        item.fechaUniformizacion || '',
        item.cantidadInicial || 0,
        item.cantidadUniformizada || 0,
        item.criterio || '',
        item.estado || '',
        item.usuarioRegistro?.nombreCompleto || ''
      ]));
      return;
    }

    downloadCsv('blueberrytrace-formalizaciones.csv', [
      'Proceso', 'Lote', 'Cama', 'Fecha', 'Jabas', 'Plantas formalizadas', 'Detalle', 'Estado', 'Responsable'
    ], formalizaciones.map((item) => [
      'Formalización',
      item.lote?.codigo || '',
      item.cama?.codigo || '',
      item.fechaFormalizacion || '',
      item.cantidadBandejas || 0,
      item.cantidadPlantas || 0,
      item.detalle || '',
      item.estado || '',
      item.usuarioRegistro?.nombreCompleto || ''
    ]));
  }

  async function confirmToggleStatus() {
    if (!pendingStatus) return;
    try {
      setConfirming(true);
      if (pendingStatus.type === 'uniformizacion') {
        onProcesosChange(await blueberryApi.toggleUniformizacionStatus(pendingStatus.id));
      } else {
        onProcesosChange(await blueberryApi.toggleFormalizacionStatus(pendingStatus.id));
      }
      emitToast('success', 'Estado actualizado', `El registro ${pendingStatus.code} fue actualizado.`);
      setPendingStatus(null);
    } catch (exception) {
      emitToast('error', 'No se pudo cambiar el estado', exception instanceof Error ? exception.message : 'Ocurrió un error inesperado.');
    } finally {
      setConfirming(false);
    }
  }

  async function confirmDeleteProcess() {
    if (!pendingDelete) return;
    try {
      setConfirming(true);
      if (pendingDelete.type === 'uniformizacion') {
        onProcesosChange(await blueberryApi.deleteUniformizacion(pendingDelete.id));
      } else {
        onProcesosChange(await blueberryApi.deleteFormalizacion(pendingDelete.id));
      }
      emitToast('success', 'Registro eliminado', `El proceso ${pendingDelete.code} fue eliminado correctamente.`);
      setPendingDelete(null);
    } catch (exception) {
      emitToast('error', 'No se pudo eliminar el proceso', exception instanceof Error ? exception.message : 'Ocurrió un error inesperado.');
    } finally {
      setConfirming(false);
    }
  }

  return (
    <main className="content-grid process-module-screen">
      <ModuleHeader
        eyebrow={pageCopy.eyebrow}
        title={pageCopy.title}
        description={pageCopy.description}
        icon={pageCopy.icon}
        tone={pageCopy.tone}
        actions={<button type="button" className="action-button" onClick={() => setModal(mode)}><Plus size={16} /> {pageCopy.addLabel}</button>}
      />

      <div className="module-utility-row module-utility-row--split">
        <div className="process-switch-note">
          <strong>{isUniformizacion ? 'Bloque de uniformización' : 'Bloque de formalización'}</strong>
          <span>{isUniformizacion ? 'Muestra solo registros de uniformización.' : 'Muestra solo registros de formalización.'}</span>
        </div>
        <button
          type="button"
          className="ghost-button"
          onClick={exportCsv}
          disabled={isUniformizacion ? uniformizaciones.length === 0 : formalizaciones.length === 0}
        >
          <Download size={15} /> Exportar CSV
        </button>
      </div>

      <section className="stacked-process-list stacked-process-list--modern">
        {aggregated.length === 0 ? (
          <EmptyState
            icon={isUniformizacion ? <Leaf size={26} /> : <ClipboardCheck size={26} />}
            title={isUniformizacion ? 'Sin uniformizaciones en seguimiento' : 'Sin formalizaciones en seguimiento'}
            description={isUniformizacion ? 'Registra siembras y uniformizaciones para visualizar el avance por lote.' : 'Registra formalizaciones para visualizar plantas y jabas organizadas por lote.'}
          />
        ) : null}
        {aggregated.map((item) => (
          <article className="process-card process-card--modern" key={item.lote.id}>
            <div className="process-card__header">
              <div className="process-card__identity">
                <span className="process-card__icon"><GitBranch size={18} /></span>
                <div>
                  <div className="process-card__title"><strong>{item.lote.codigo}</strong> <span>{item.lote.descripcion || 'Invernadero'}</span></div>
                  <small>{numberCompact(item.planted)} plantas · Actualizado: {dateShort(item.latest?.fechaActualizacion || item.latest?.fechaCreacion || null)}</small>
                </div>
              </div>
              <div className="process-card__meta">
                <StatusBadge value={item.status} />
                <strong>{item.progress}%</strong>
              </div>
            </div>
            <span className="process-card__caption">{isUniformizacion ? 'Progreso de uniformización' : 'Progreso de formalización'}</span>
            <div className="progress-track progress-track--wide"><span style={{ width: `${item.progress}%` }} /></div>
            <div className="process-card__footer">
              <span>{numberCompact(item.processed)} {isUniformizacion ? 'uniformizadas' : 'formalizadas'}</span>
              <span>{numberCompact(item.planted)} sembradas</span>
            </div>
          </article>
        ))}
      </section>

      <section className="tables-grid tables-grid--single">
        {isUniformizacion ? (
          <DataTable<UniformizacionResponse>
            title="Uniformizaciones"
            description="Movimientos de macetas entre jabas, con recuperación automática cuando se registran plantas secas."
            items={uniformizaciones}
            emptyTitle="Sin uniformizaciones registradas"
            emptyDescription="Los registros aparecerán cuando se complete el proceso por lote y cama."
            columns={[
              { key: 'lote', label: 'Lote', render: (item) => item.lote?.codigo || 'Sin lote' },
              { key: 'cama', label: 'Cama', render: (item) => item.cama?.codigo || 'Sin cama' },
              { key: 'fechaUniformizacion', label: 'Fecha', render: (item) => dateShort(item.fechaUniformizacion) },
              { key: 'criterio', label: 'Criterio', render: (item) => item.criterio || 'No definido' },
              { key: 'cantidadInicial', label: 'Inicial', render: (item) => numberCompact(item.cantidadInicial || 0) },
              { key: 'cantidadUniformizada', label: 'Uniformizada', render: (item) => numberCompact(item.cantidadUniformizada || 0) },
              { key: 'recuperacion', label: 'Recuperación', render: (item) => item.recuperacionRiego ? `Riego #${item.recuperacionRiego.id} · ${numberCompact(item.cantidadRecuperacion || 0)} plantas` : (item.cantidadRecuperacion || 0) > 0 ? `${numberCompact(item.cantidadRecuperacion || 0)} plantas por registrar` : 'Sin recuperación' },
              { key: 'estado', label: 'Estado', render: (item) => <StatusBadge value={item.estado} /> },
              { key: 'acciones', label: 'Acciones', render: (item) => (
                <div className="icon-actions">
                  <button type="button" className="icon-action" title="Editar uniformización" onClick={() => setEditingUniformizacion(item)}><Pencil size={15} /></button>
                  <button type="button" className="icon-action" title="Cambiar estado" onClick={() => setPendingStatus({ type: 'uniformizacion', id: item.id, code: item.lote?.codigo || `UNI-${item.id}` })}><RotateCcw size={15} /></button>
                  <button type="button" className="icon-action icon-action--danger" title="Eliminar uniformización" onClick={() => setPendingDelete({ type: 'uniformizacion', id: item.id, code: item.lote?.codigo || `UNI-${item.id}` })}><Trash2 size={15} /></button>
                </div>
              ) }
            ]}
          />
        ) : (
          <DataTable<FormalizacionResponse>
            title="Formalizaciones"
            description="Jabas completas y plantas formalizadas con edición directa."
            items={formalizaciones}
            emptyTitle="Sin formalizaciones registradas"
            emptyDescription="Los registros aparecerán cuando se organicen jabas y plantas."
            columns={[
              { key: 'lote', label: 'Lote', render: (item) => item.lote?.codigo || 'Sin lote' },
              { key: 'cama', label: 'Cama', render: (item) => item.cama?.codigo || 'Sin cama' },
              { key: 'fechaFormalizacion', label: 'Fecha', render: (item) => dateShort(item.fechaFormalizacion) },
              { key: 'detalle', label: 'Detalle', render: (item) => item.detalle || 'No definido' },
              { key: 'jabasMovidas', label: 'Jabas', render: (item) => item.jabasMovidas?.length ? item.jabasMovidas.map((jaba) => jaba.codigo).join(', ') : numberCompact(item.cantidadBandejas || 0) },
              { key: 'cantidadPlantas', label: 'Plantas', render: (item) => numberCompact(item.cantidadPlantas || 0) },
              { key: 'estado', label: 'Estado', render: (item) => <StatusBadge value={item.estado} /> },
              { key: 'acciones', label: 'Acciones', render: (item) => (
                <div className="icon-actions">
                  <button type="button" className="icon-action" title="Editar formalización" onClick={() => setEditingFormalizacion(item)}><Pencil size={15} /></button>
                  <button type="button" className="icon-action" title="Cambiar estado" onClick={() => setPendingStatus({ type: 'formalizacion', id: item.id, code: item.lote?.codigo || `FOR-${item.id}` })}><RotateCcw size={15} /></button>
                  <button type="button" className="icon-action icon-action--danger" title="Eliminar formalización" onClick={() => setPendingDelete({ type: 'formalizacion', id: item.id, code: item.lote?.codigo || `FOR-${item.id}` })}><Trash2 size={15} /></button>
                </div>
              ) }
            ]}
          />
        )}
      </section>

      <Modal open={modal === 'uniformizacion'} title="Nueva uniformización" description="Mueve macetas entre jabas, agrupa plantas por tamaño y deriva plantas secas a recuperación por riego." onClose={() => setModal(null)}>
        <ProcesoForm mode="uniformizacion" lotesTrazables={lotesTrazables} jabas={jabas} onSubmit={createUniformizacion} onCancel={() => setModal(null)} />
      </Modal>
      <Modal open={modal === 'formalizacion'} title="Nueva formalización" description="Registra las jabas completas organizadas por tamaño dentro de la cama." onClose={() => setModal(null)}>
        <ProcesoForm mode="formalizacion" lotesTrazables={lotesTrazables} jabas={jabas} onSubmit={createFormalizacion} onCancel={() => setModal(null)} />
      </Modal>

      <Modal open={Boolean(editingUniformizacion)} title="Editar uniformización" description="Actualiza el movimiento de macetas y el criterio de agrupación." onClose={() => setEditingUniformizacion(null)}>
        {editingUniformizacion ? <ProcesoForm mode="uniformizacion" lotesTrazables={lotesTrazables} jabas={jabas} initialData={editingUniformizacion} submitLabel="Guardar cambios" onSubmit={updateUniformizacion} onCancel={() => setEditingUniformizacion(null)} /> : null}
      </Modal>
      <Modal open={Boolean(editingFormalizacion)} title="Editar formalización" description="Actualiza las jabas completas organizadas y el orden aplicado." onClose={() => setEditingFormalizacion(null)}>
        {editingFormalizacion ? <ProcesoForm mode="formalizacion" lotesTrazables={lotesTrazables} jabas={jabas} initialData={editingFormalizacion} submitLabel="Guardar cambios" onSubmit={updateFormalizacion} onCancel={() => setEditingFormalizacion(null)} /> : null}
      </Modal>

      <ConfirmDialog
        open={Boolean(pendingStatus)}
        title="Cambiar estado del proceso"
        description="Se alternará el estado entre registrado y anulado."
        confirmLabel="Cambiar estado"
        loading={confirming}
        onCancel={() => setPendingStatus(null)}
        onConfirm={confirmToggleStatus}
      />

      <ConfirmDialog
        open={Boolean(pendingDelete)}
        tone="danger"
        title="Anular proceso operativo"
        description="El registro quedará anulado y se conservará en el historial para mantener la trazabilidad."
        confirmLabel="Anular registro"
        loading={confirming}
        onCancel={() => setPendingDelete(null)}
        onConfirm={confirmDeleteProcess}
      />
    </main>
  );
}
