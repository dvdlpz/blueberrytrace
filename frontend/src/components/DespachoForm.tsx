import { useMemo, useState } from 'react';
import { ClipboardCheck, Loader2, PackageCheck, Save, Truck } from 'lucide-react';
import { FormMessage, FormPrerequisite, FormSection } from './FormLayout';
import type { ClasificacionResponse, DespachoFormPayload, EmpaqueResponse, LoteTrazableResponse, PedidoResponse } from '../types/api';

interface DespachoFormProps {
  lotesTrazables: LoteTrazableResponse[];
  clasificaciones: ClasificacionResponse[];
  pedidos: PedidoResponse[];
  empaques: EmpaqueResponse[];
  validaciones: string[];
  initialData?: DespachoFormPayload;
  submitLabel?: string;
  onSubmit: (payload: DespachoFormPayload) => Promise<void>;
  onCancel: () => void;
}
const today = () => new Date().toISOString().slice(0, 10);
const pretty = (value?: string | null) => (value || '').replaceAll('_', ' ').toLowerCase().replace(/^./, (letter) => letter.toUpperCase());

export function DespachoForm({ lotesTrazables, clasificaciones, pedidos, empaques, validaciones, initialData, submitLabel = 'Registrar despacho', onSubmit, onCancel }: DespachoFormProps) {
  const firstTrace = lotesTrazables[0];
  const activeOrders = pedidos.filter((item) => ['CONFIRMADO', 'PARCIAL'].includes(item.estado?.toUpperCase()));
  const firstOrder = activeOrders[0];
  const firstDetail = firstOrder?.detalles[0];
  const firstPackage = empaques.find((item) => item.pedidoDetalle?.id === firstDetail?.id && item.unidadesPendientes > 0 && item.estado !== 'ANULADO');
  const [payload, setPayload] = useState<DespachoFormPayload>(initialData || {
    loteTrazableId: firstTrace?.id || 0,
    clasificacionId: firstPackage?.clasificacion?.id || 0,
    loteId: firstTrace?.loteFisico?.id || 0,
    pedidoId: firstOrder?.id || 0,
    pedidoDetalleId: firstDetail?.id || 0,
    empaqueId: firstPackage?.id || 0,
    unidadesEmpaque: 1,
    fechaDespacho: today(),
    vehiculo: '',
    guiaRemision: '',
    validacionCalidad: validaciones[0] || 'APTO',
    observacion: '',
    estado: 'REGISTRADO'
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const trace = useMemo(() => lotesTrazables.find((item) => item.id === payload.loteTrazableId) || null, [lotesTrazables, payload.loteTrazableId]);
  const order = useMemo(() => activeOrders.find((item) => item.id === payload.pedidoId) || null, [activeOrders, payload.pedidoId]);
  const selectedDetail = order?.detalles.find((item) => item.id === payload.pedidoDetalleId) || null;
  const availablePackages = useMemo(() => empaques.filter((item) => item.pedidoDetalle?.id === payload.pedidoDetalleId && item.unidadesPendientes > 0 && item.estado !== 'ANULADO'), [empaques, payload.pedidoDetalleId]);
  const selectedPackage = availablePackages.find((item) => item.id === payload.empaqueId) || null;
  const eligibleClassifications = useMemo(() => clasificaciones.filter((item) => item.loteTrazable?.id === payload.loteTrazableId && item.estado?.toUpperCase() === 'VALIDADA'), [clasificaciones, payload.loteTrazableId]);
  const canRegister = Boolean(trace && order && selectedDetail && selectedPackage && payload.clasificacionId);

  function selectTrace(id: number) {
    const next = lotesTrazables.find((item) => item.id === id);
    setPayload((current) => ({ ...current, loteTrazableId: id, loteId: next?.loteFisico?.id || 0, clasificacionId: 0, empaqueId: 0 }));
  }

  function selectOrder(id: number) {
    const next = activeOrders.find((item) => item.id === id);
    const detail = next?.detalles[0];
    const packageRow = empaques.find((item) => item.pedidoDetalle?.id === detail?.id && item.unidadesPendientes > 0 && item.estado !== 'ANULADO');
    setPayload((current) => ({ ...current, pedidoId: id, pedidoDetalleId: detail?.id || 0, empaqueId: packageRow?.id || 0, loteTrazableId: packageRow?.loteTrazable?.id || current.loteTrazableId, clasificacionId: packageRow?.clasificacion?.id || 0, loteId: lotesTrazables.find((traceRow) => traceRow.id === packageRow?.loteTrazable?.id)?.loteFisico?.id || current.loteId }));
  }

  function selectDetail(id: number) {
    const packageRow = empaques.find((item) => item.pedidoDetalle?.id === id && item.unidadesPendientes > 0 && item.estado !== 'ANULADO');
    setPayload((current) => ({ ...current, pedidoDetalleId: id, empaqueId: packageRow?.id || 0, loteTrazableId: packageRow?.loteTrazable?.id || current.loteTrazableId, clasificacionId: packageRow?.clasificacion?.id || 0, loteId: lotesTrazables.find((traceRow) => traceRow.id === packageRow?.loteTrazable?.id)?.loteFisico?.id || current.loteId }));
  }

  function selectPackage(id: number) {
    const packageRow = empaques.find((item) => item.id === id);
    const traceRow = lotesTrazables.find((item) => item.id === packageRow?.loteTrazable?.id);
    setPayload((current) => ({ ...current, empaqueId: id, loteTrazableId: packageRow?.loteTrazable?.id || current.loteTrazableId, clasificacionId: packageRow?.clasificacion?.id || current.clasificacionId, loteId: traceRow?.loteFisico?.id || current.loteId, unidadesEmpaque: Math.min(current.unidadesEmpaque, packageRow?.unidadesPendientes || 1) || 1 }));
  }

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!canRegister) { setError('Selecciona un pedido, una variedad y un empaque preparado antes de registrar el despacho.'); return; }
    if ((payload.unidadesEmpaque || 0) > (selectedPackage?.unidadesPendientes || 0)) { setError(`Solo hay ${selectedPackage?.unidadesPendientes || 0} unidades disponibles en el empaque seleccionado.`); return; }
    try {
      setSaving(true); setError(null);
      await onSubmit({ ...payload, vehiculo: payload.vehiculo?.trim() || undefined, guiaRemision: payload.guiaRemision?.trim() || undefined, observacion: payload.observacion?.trim() || undefined });
    } catch (exception) { setError(exception instanceof Error ? exception.message : 'No fue posible registrar el despacho.'); }
    finally { setSaving(false); }
  }

  const prerequisite = activeOrders.length === 0 ? { title: 'Se requiere un pedido confirmado', description: 'Confirma un pedido por variedad antes de preparar el despacho.' } : empaques.length === 0 ? { title: 'Se requiere un empaque preparado', description: 'Forma jabas cosecheras o bins de madera desde una clasificación validada.' } : null;

  return (
    <form className="form-shell" onSubmit={submit}>
      {error ? <FormMessage>{error}</FormMessage> : null}
      {prerequisite ? <FormPrerequisite title={prerequisite.title} description={prerequisite.description} /> : null}
      <FormSection title="Pedido y variedad" description="El despacho se registra según las cantidades solicitadas por variedad." icon={<Truck size={18} />}>
        <div className="form-grid form-grid--two">
          <label className="form-grid__full"><span>Pedido</span><select value={payload.pedidoId} onChange={(event) => selectOrder(Number(event.target.value))}><option value={0} disabled>Selecciona un pedido</option>{activeOrders.map((item) => <option key={item.id} value={item.id}>{item.codigo} · {item.cliente} · {pretty(item.estado)}</option>)}</select></label>
          <label><span>Variedad solicitada</span><select value={payload.pedidoDetalleId} onChange={(event) => selectDetail(Number(event.target.value))} disabled={!order}><option value={0} disabled>Selecciona una variedad</option>{order?.detalles.map((item) => <option key={item.id} value={item.id}>{item.variedad} · Pendiente: {item.cantidadPendiente}</option>)}</select></label>
          <label><span>Destino</span><input value={order?.destino || 'Pendiente de pedido'} readOnly /></label>
        </div>
      </FormSection>
      <FormSection title="Empaque y carga" description="La jaba cosechera siempre contiene 15 macetas. Para un bin, registra previamente su capacidad real superior a 100 plantas." icon={<PackageCheck size={18} />}>
        <div className="form-grid form-grid--two">
          <label className="form-grid__full"><span>Empaque preparado</span><select value={payload.empaqueId} onChange={(event) => selectPackage(Number(event.target.value))} disabled={availablePackages.length === 0}><option value={0} disabled>Selecciona un empaque disponible</option>{availablePackages.map((item) => <option key={item.id} value={item.id}>{item.tipo === 'JABA_COSECHERA' ? 'Jaba cosechera' : 'Bin de madera'} · {item.unidadesPendientes} unidades disponibles · {item.capacidadPorUnidad} plantas por unidad</option>)}</select></label>
          <label><span>Unidades para el tráiler</span><input type="number" min={1} max={selectedPackage?.unidadesPendientes || undefined} value={payload.unidadesEmpaque} onChange={(event) => setPayload({ ...payload, unidadesEmpaque: Number(event.target.value) })} disabled={!selectedPackage} required /></label>
          <label><span>Plantas calculadas</span><input value={selectedPackage ? `${payload.unidadesEmpaque * selectedPackage.capacidadPorUnidad} plantas` : 'Selecciona un empaque'} readOnly /></label>
          <label><span>Clasificación vinculada</span><input value={selectedPackage?.clasificacion?.codigo || 'Pendiente'} readOnly /></label>
          <label><span>Lote trazable</span><input value={trace?.codigo || selectedPackage?.loteTrazable?.codigo || 'Pendiente'} readOnly /></label>
        </div>
      </FormSection>
      <FormSection title="Salida de tráiler" description="El despacho inicia como registrado y se confirma cuando la carga ingresa al vehículo." icon={<ClipboardCheck size={18} />}>
        <div className="form-grid form-grid--two">
          <label><span>Fecha de despacho</span><input type="date" value={payload.fechaDespacho} onChange={(event) => setPayload({ ...payload, fechaDespacho: event.target.value })} required /></label>
          <label><span>Vehículo o tráiler</span><input value={payload.vehiculo || ''} onChange={(event) => setPayload({ ...payload, vehiculo: event.target.value })} maxLength={120} placeholder="Ejemplo: Tráiler de despacho 01" /></label>
          <label><span>Guía de remisión</span><input value={payload.guiaRemision || ''} onChange={(event) => setPayload({ ...payload, guiaRemision: event.target.value })} maxLength={80} placeholder="Número de guía" /></label>
          <label><span>Validación de calidad</span><select value={payload.validacionCalidad} onChange={(event) => setPayload({ ...payload, validacionCalidad: event.target.value })}>{validaciones.map((item) => <option key={item} value={item}>{pretty(item)}</option>)}</select></label>
          <label className="form-grid__full"><span>Observación</span><textarea value={payload.observacion || ''} onChange={(event) => setPayload({ ...payload, observacion: event.target.value })} maxLength={255} placeholder="Detalle del traslado o entrega" /></label>
        </div>
      </FormSection>
      <footer className="form-actions form-actions--sticky"><button type="button" className="ghost-button" onClick={onCancel} disabled={saving}>Cancelar</button><button type="submit" className="action-button" disabled={saving || !canRegister}>{saving ? <Loader2 className="spin" size={16} /> : <Save size={16} />}{saving ? 'Guardando...' : submitLabel}</button></footer>
    </form>
  );
}
