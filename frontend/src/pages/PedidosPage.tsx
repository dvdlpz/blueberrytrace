import { useMemo, useState, type FormEvent } from 'react';
import { ClipboardList, Pencil, Plus, Save, Trash2, Truck } from 'lucide-react';
import { EmptyState } from '../components/EmptyState';
import { FormMessage, FormSection } from '../components/FormLayout';
import { Modal } from '../components/Modal';
import { ModuleHeader } from '../components/ModuleHeader';
import { StatusBadge } from '../components/StatusBadge';
import { blueberryApi } from '../lib/api';
import { dateShort, numberCompact } from '../lib/format';
import { emitToast } from '../lib/uiEvents';
import type { PedidoDetalleFormPayload, PedidoFormPayload, PedidoResponse } from '../types/api';

interface PedidosPageProps {
  pedidos: PedidoResponse[];
  onChanged: (items: PedidoResponse[]) => void;
}

const today = () => new Date().toISOString().slice(0, 10);
const blankDetail = (): PedidoDetalleFormPayload => ({ variedad: '', cantidadSolicitada: 1, observacion: '' });
const blankOrder = (): PedidoFormPayload => ({
  codigo: '',
  cliente: '',
  destino: '',
  fechaCompromiso: today(),
  estado: 'BORRADOR',
  observacion: '',
  detalles: [blankDetail()]
});

const toPayload = (item: PedidoResponse): PedidoFormPayload => ({
  codigo: item.codigo,
  cliente: item.cliente,
  destino: item.destino || '',
  fechaCompromiso: item.fechaCompromiso,
  estado: item.estado,
  observacion: item.observacion || '',
  detalles: item.detalles.map((detail) => ({
    variedad: detail.variedad,
    cantidadSolicitada: detail.cantidadSolicitada,
    observacion: detail.observacion || ''
  }))
});

function normalizeVariety(value: string) {
  return value.trim().toLocaleUpperCase('es-PE');
}

export function PedidosPage({ pedidos, onChanged }: PedidosPageProps) {
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<PedidoResponse | null>(null);
  const [saving, setSaving] = useState(false);

  async function save(payload: PedidoFormPayload) {
    setSaving(true);
    try {
      const response = editing
        ? await blueberryApi.updatePedido(editing.id, payload)
        : await blueberryApi.createPedido(payload);
      onChanged(response.items);
      setEditing(null);
      setOpen(false);
      emitToast(
        'success',
        editing ? 'Pedido actualizado' : 'Pedido registrado',
        'Las cantidades por variedad quedan disponibles para preparar empaques.'
      );
    } finally {
      setSaving(false);
    }
  }

  function closeForm() {
    setOpen(false);
    setEditing(null);
  }

  return (
    <main className="content-grid">
      <ModuleHeader
        eyebrow="Planificación de salida"
        title="Pedidos por variedad"
        description="Registra el pedido del cliente antes de preparar jabas cosecheras o bins para el tráiler."
        icon={<ClipboardList size={21} />}
        tone="purple"
        actions={<button type="button" className="action-button" onClick={() => setOpen(true)}><Plus size={16} /> Registrar pedido</button>}
      />

      <section className="panel-card">
        <div className="panel-card__header">
          <div>
            <h2>Pedidos programados</h2>
            <p>Cada pedido controla cantidades pendientes por variedad.</p>
          </div>
          <span className="panel-card__count">{pedidos.length} pedidos</span>
        </div>
        {pedidos.length ? (
          <div className="data-table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Pedido</th>
                  <th>Cliente y destino</th>
                  <th>Fecha compromiso</th>
                  <th>Variedades</th>
                  <th>Estado</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {pedidos.map((item) => (
                  <tr key={item.id}>
                    <td><strong className="table-code">{item.codigo}</strong><small>{item.observacion || 'Sin observación'}</small></td>
                    <td><strong>{item.cliente}</strong><small>{item.destino || 'Sin destino registrado'}</small></td>
                    <td>{dateShort(item.fechaCompromiso)}</td>
                    <td>
                      <div className="table-tag-list">
                        {item.detalles.map((detail) => <span key={detail.id} className="table-tag">{detail.variedad}: {numberCompact(detail.cantidadPendiente)} pendientes</span>)}
                      </div>
                    </td>
                    <td><StatusBadge value={item.estado} /></td>
                    <td><button className="icon-action" type="button" aria-label="Editar pedido" onClick={() => setEditing(item)}><Pencil size={15} /></button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState
            icon={<ClipboardList size={28} />}
            title="Aún no hay pedidos registrados"
            description="Registra el pedido del cliente con cantidades por variedad antes de preparar empaques."
            action={<button type="button" className="action-button" onClick={() => setOpen(true)}><Plus size={15} /> Registrar pedido</button>}
          />
        )}
      </section>

      <Modal
        open={open || Boolean(editing)}
        title={editing ? 'Editar pedido' : 'Registrar pedido'}
        description="Agrega una o más variedades con la cantidad requerida por el cliente."
        onClose={closeForm}
      >
        <PedidoForm
          initialData={editing ? toPayload(editing) : blankOrder()}
          saving={saving}
          editing={Boolean(editing)}
          onSubmit={save}
          onCancel={closeForm}
        />
      </Modal>
    </main>
  );
}

interface PedidoFormProps {
  initialData: PedidoFormPayload;
  saving: boolean;
  editing: boolean;
  onSubmit: (payload: PedidoFormPayload) => Promise<void>;
  onCancel: () => void;
}

function PedidoForm({ initialData, saving, editing, onSubmit, onCancel }: PedidoFormProps) {
  const [payload, setPayload] = useState(initialData);
  const [error, setError] = useState<string | null>(null);

  const duplicateVarietyIndexes = useMemo(() => {
    const counts = new Map<string, number>();
    payload.detalles.forEach((detail) => {
      const normalized = normalizeVariety(detail.variedad);
      if (normalized) counts.set(normalized, (counts.get(normalized) || 0) + 1);
    });

    return new Set(
      payload.detalles.flatMap((detail, index) => {
        const normalized = normalizeVariety(detail.variedad);
        return normalized && (counts.get(normalized) || 0) > 1 ? [index] : [];
      })
    );
  }, [payload.detalles]);

  function updateDetail(index: number, patch: Partial<PedidoDetalleFormPayload>) {
    setPayload((current) => ({
      ...current,
      detalles: current.detalles.map((item, itemIndex) => itemIndex === index ? { ...item, ...patch } : item)
    }));
    setError(null);
  }

  function addDetail() {
    setPayload((current) => ({ ...current, detalles: [...current.detalles, blankDetail()] }));
    setError(null);
  }

  function removeDetail(index: number) {
    setPayload((current) => ({ ...current, detalles: current.detalles.filter((_, itemIndex) => itemIndex !== index) }));
    setError(null);
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const hasIncompleteDetail = !payload.detalles.length || payload.detalles.some((item) => !item.variedad.trim() || item.cantidadSolicitada < 1);
    if (hasIncompleteDetail) {
      setError('Registra al menos una variedad con una cantidad solicitada válida.');
      return;
    }
    if (duplicateVarietyIndexes.size > 0) {
      setError('Cada variedad debe aparecer una sola vez dentro del pedido. Corrige las filas resaltadas.');
      return;
    }

    try {
      setError(null);
      await onSubmit({
        ...payload,
        codigo: payload.codigo.trim(),
        cliente: payload.cliente.trim(),
        destino: payload.destino?.trim() || undefined,
        observacion: payload.observacion?.trim() || undefined,
        detalles: payload.detalles.map((item) => ({
          ...item,
          variedad: item.variedad.trim(),
          observacion: item.observacion?.trim() || undefined
        }))
      });
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'No fue posible guardar el pedido.');
    }
  }

  return (
    <form className="form-shell" onSubmit={submit}>
      {error ? <FormMessage>{error}</FormMessage> : null}

      <FormSection
        title="Datos del pedido"
        description="El pedido define la variedad, destino y cantidades que deben prepararse para el despacho."
        icon={<Truck size={18} />}
      >
        <div className="form-grid form-grid--two">
          <label>
            <span>Código de pedido</span>
            <input value={payload.codigo} onChange={(event) => setPayload({ ...payload, codigo: event.target.value })} maxLength={50} required readOnly={editing} placeholder="Ejemplo: PED-001" />
          </label>
          <label>
            <span>Cliente</span>
            <input value={payload.cliente} onChange={(event) => setPayload({ ...payload, cliente: event.target.value })} maxLength={150} required />
          </label>
          <label>
            <span>Destino</span>
            <input value={payload.destino || ''} onChange={(event) => setPayload({ ...payload, destino: event.target.value })} maxLength={160} placeholder="Lugar de entrega" />
          </label>
          <label>
            <span>Fecha compromiso</span>
            <input type="date" value={payload.fechaCompromiso} onChange={(event) => setPayload({ ...payload, fechaCompromiso: event.target.value })} required />
          </label>
          <label>
            <span>Estado inicial</span>
            <select value={payload.estado} onChange={(event) => setPayload({ ...payload, estado: event.target.value })}>
              <option value="BORRADOR">Borrador</option>
              <option value="CONFIRMADO">Confirmado</option>
            </select>
          </label>
          <label className="form-grid__full">
            <span>Observación</span>
            <textarea value={payload.observacion || ''} onChange={(event) => setPayload({ ...payload, observacion: event.target.value })} maxLength={255} />
          </label>
        </div>
      </FormSection>

      <FormSection
        title="Cantidades por variedad"
        description="Cada variedad aparece una vez y se controla durante empaque y despacho."
        icon={<ClipboardList size={18} />}
      >
        <div className="detail-editor">
          <p className="detail-editor__help" id="pedido-variedades-ayuda">
            Registra cada variedad una sola vez. La comparación ignora espacios al inicio o final y mayúsculas o minúsculas.
          </p>
          {payload.detalles.map((detail, index) => {
            const duplicate = duplicateVarietyIndexes.has(index);
            const varietyErrorId = `pedido-variedad-error-${index}`;

            return (
              <section className={`detail-editor__row${duplicate ? ' detail-editor__row--invalid' : ''}`} key={index} aria-labelledby={`pedido-variedad-${index}`}>
                <header className="detail-editor__row-header">
                  <span className="detail-editor__index" id={`pedido-variedad-${index}`}>Variedad {String(index + 1).padStart(2, '0')}</span>
                  {duplicate ? <span className="detail-editor__status" role="status">Duplicada</span> : null}
                </header>
                <label className={`detail-editor__field detail-editor__field--variedad${duplicate ? ' form-field--invalid' : ''}`}>
                  <span>Variedad</span>
                  <input
                    value={detail.variedad}
                    onChange={(event) => updateDetail(index, { variedad: event.target.value })}
                    maxLength={120}
                    required
                    aria-invalid={duplicate}
                    aria-describedby={duplicate ? varietyErrorId : 'pedido-variedades-ayuda'}
                  />
                  {duplicate ? <small className="field-error" id={varietyErrorId}>Esta variedad ya está registrada en otra fila.</small> : null}
                </label>
                <label className="detail-editor__field detail-editor__field--cantidad">
                  <span>Cantidad solicitada</span>
                  <input
                    type="number"
                    min={1}
                    value={detail.cantidadSolicitada}
                    onChange={(event) => updateDetail(index, { cantidadSolicitada: Number(event.target.value) })}
                    required
                  />
                </label>
                <label className="detail-editor__field detail-editor__field--observacion">
                  <span>Observación</span>
                  <input value={detail.observacion || ''} onChange={(event) => updateDetail(index, { observacion: event.target.value })} maxLength={255} />
                </label>
                <div className="detail-editor__actions">
                  <button type="button" className="ghost-button ghost-button--small" onClick={() => removeDetail(index)} disabled={payload.detalles.length === 1}>
                    <Trash2 size={14} /> Quitar
                  </button>
                </div>
              </section>
            );
          })}
          <button type="button" className="ghost-button detail-editor__add" onClick={addDetail}><Plus size={15} /> Agregar variedad</button>
        </div>
      </FormSection>

      <footer className="form-actions form-actions--sticky">
        <button className="ghost-button" type="button" onClick={onCancel} disabled={saving}>Cancelar</button>
        <button className="action-button" type="submit" disabled={saving}>{saving ? 'Guardando...' : <><Save size={16} /> Guardar pedido</>}</button>
      </footer>
    </form>
  );
}
