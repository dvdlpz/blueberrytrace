import { useMemo, useState } from 'react';
import { Loader2, Save, Truck } from 'lucide-react';
import { FormMessage, FormPrerequisite, FormSection } from './FormLayout';
import type { CargaDespachoFormPayload, PedidoResponse } from '../types/api';

interface CargaDespachoFormProps {
  pedidos: PedidoResponse[];
  onSubmit: (payload: CargaDespachoFormPayload) => Promise<void>;
  onCancel: () => void;
}

const today = () => new Date().toISOString().slice(0, 10);
const initialCode = () => `CAR-${today().replaceAll('-', '')}-001`;

export function CargaDespachoForm({ pedidos, onSubmit, onCancel }: CargaDespachoFormProps) {
  const availableOrders = useMemo(
    () => pedidos.filter((item) => ['CONFIRMADO', 'PARCIAL'].includes((item.estado || '').toUpperCase())),
    [pedidos]
  );
  const firstOrder = availableOrders[0];
  const [payload, setPayload] = useState<CargaDespachoFormPayload>({
    codigo: initialCode(),
    pedidoId: firstOrder?.id || 0,
    fechaCarga: today(),
    vehiculo: '',
    guiaRemision: '',
    observacion: ''
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const selectedOrder = availableOrders.find((item) => item.id === payload.pedidoId) || null;
  const canCreate = Boolean(selectedOrder && selectedOrder.destino?.trim() && payload.codigo.trim() && payload.vehiculo.trim());

  function selectOrder(id: number) {
    setPayload((current) => ({ ...current, pedidoId: id }));
  }

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedOrder) {
      setError('Selecciona un pedido confirmado o parcialmente atendido para preparar la carga.');
      return;
    }
    if (!selectedOrder.destino?.trim()) {
      setError('El pedido seleccionado necesita un destino antes de preparar el tráiler.');
      return;
    }
    try {
      setSaving(true);
      setError(null);
      await onSubmit({
        ...payload,
        codigo: payload.codigo.trim().toUpperCase(),
        vehiculo: payload.vehiculo.trim(),
        guiaRemision: payload.guiaRemision?.trim() || undefined,
        observacion: payload.observacion?.trim() || undefined
      });
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'No fue posible preparar la carga de tráiler.');
    } finally {
      setSaving(false);
    }
  }

  const prerequisite = availableOrders.length === 0
    ? { title: 'Se requiere un pedido confirmado', description: 'Confirma un pedido por variedad antes de preparar una carga de tráiler.' }
    : null;

  return (
    <form className="form-shell" onSubmit={submit}>
      {error ? <FormMessage>{error}</FormMessage> : null}
      {prerequisite ? <FormPrerequisite title={prerequisite.title} description={prerequisite.description} /> : null}
      <FormSection title="Carga de tráiler" description="Una carga consolida líneas de distintas variedades del mismo pedido antes de su salida." icon={<Truck size={18} />}>
        <div className="form-grid form-grid--two">
          <label><span>Código de carga</span><input value={payload.codigo} onChange={(event) => setPayload({ ...payload, codigo: event.target.value })} maxLength={60} required /></label>
          <label><span>Fecha de carga</span><input type="date" value={payload.fechaCarga} onChange={(event) => setPayload({ ...payload, fechaCarga: event.target.value })} required /></label>
          <label className="form-grid__full"><span>Pedido</span><select value={payload.pedidoId} onChange={(event) => selectOrder(Number(event.target.value))} disabled={availableOrders.length === 0} required><option value={0} disabled>Selecciona un pedido</option>{availableOrders.map((item) => <option key={item.id} value={item.id}>{item.codigo} · {item.cliente} · Pendiente: {item.detalles.reduce((total, detail) => total + (detail.cantidadPendiente || 0), 0)}</option>)}</select></label>
          <label><span>Destino del pedido</span><input value={selectedOrder?.destino || 'Registra un destino en el pedido'} readOnly /></label>
          <label><span>Vehículo o tráiler</span><input value={payload.vehiculo} onChange={(event) => setPayload({ ...payload, vehiculo: event.target.value })} maxLength={120} required /></label>
          <label><span>Guía de remisión</span><input value={payload.guiaRemision || ''} onChange={(event) => setPayload({ ...payload, guiaRemision: event.target.value })} maxLength={80} /></label>
          <label className="form-grid__full"><span>Observación</span><textarea value={payload.observacion || ''} onChange={(event) => setPayload({ ...payload, observacion: event.target.value })} maxLength={255} placeholder="Indica alguna condición relevante de la carga" /></label>
        </div>
      </FormSection>
      <footer className="form-actions form-actions--sticky"><button type="button" className="ghost-button" onClick={onCancel} disabled={saving}>Cancelar</button><button type="submit" className="action-button" disabled={saving || !canCreate}>{saving ? <Loader2 className="spin" size={16} /> : <Save size={16} />}{saving ? 'Preparando...' : 'Preparar carga'}</button></footer>
    </form>
  );
}
