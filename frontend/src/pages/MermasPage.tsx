import { useEffect, useMemo, useState } from 'react';
import { AlertTriangle, ArchiveX, MinusCircle, Plus, Search } from 'lucide-react';
import { EmptyState } from '../components/EmptyState';
import { FormSection } from '../components/FormLayout';
import { MermaForm } from '../components/MermaForm';
import { Modal } from '../components/Modal';
import { ModuleHeader } from '../components/ModuleHeader';
import { StatusBadge } from '../components/StatusBadge';
import { blueberryApi } from '../lib/api';
import { dateShort, numberCompact } from '../lib/format';
import { emitToast } from '../lib/uiEvents';
import type { LoteTrazableResponse, MermaFormPayload, MermaResponse } from '../types/api';

interface MermasPageProps {
  lotesTrazables: LoteTrazableResponse[];
}

export function MermasPage({ lotesTrazables }: MermasPageProps) {
  const [items, setItems] = useState<MermaResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [query, setQuery] = useState('');
  const [pending, setPending] = useState<MermaResponse | null>(null);
  const [reason, setReason] = useState('');
  const [saving, setSaving] = useState(false);

  async function reload() {
    try {
      setLoading(true);
      setError(null);
      setItems(await blueberryApi.mermas());
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'No fue posible cargar las mermas.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { void reload(); }, []);

  const filtered = useMemo(() => {
    const term = query.trim().toLowerCase();
    return items.filter((item) => !term || [item.loteTrazable?.codigo, item.etapaOrigen, item.motivo, item.estado, item.usuarioRegistro?.nombreCompleto]
      .some((value) => String(value || '').toLowerCase().includes(term)));
  }, [items, query]);
  const active = items.filter((item) => String(item.estado || '').toUpperCase() !== 'ANULADA');
  const total = active.reduce((sum, item) => sum + (item.cantidad || 0), 0);

  async function create(payload: MermaFormPayload) {
    await blueberryApi.createMerma(payload);
    setCreating(false);
    await reload();
    emitToast('success', 'Merma registrada', 'La cantidad fue descontada del saldo disponible y quedó registrada en el historial.');
  }

  async function annul() {
    if (!pending) return;
    try {
      setSaving(true);
      await blueberryApi.annulMerma(pending.id, reason.trim());
      setPending(null);
      setReason('');
      await reload();
      emitToast('success', 'Merma anulada', 'La cantidad vuelve a estar disponible según las reglas operativas.');
    } catch (exception) {
      emitToast('error', 'No se pudo anular la merma', exception instanceof Error ? exception.message : 'Intenta nuevamente.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="content-grid">
      <ModuleHeader
        eyebrow="Control de disponibilidad"
        title="Mermas y ajustes"
        description="Registra pérdidas o ajustes justificados sin eliminar la evidencia de la operación."
        icon={<MinusCircle size={21} />}
        tone="orange"
        actions={<button type="button" className="action-button" onClick={() => setCreating(true)} disabled={lotesTrazables.length === 0}><Plus size={16} /> Registrar merma</button>}
      />

      <section className="summary-strip summary-strip--three">
        <article className="summary-pill summary-pill--orange"><span className="summary-pill__icon"><AlertTriangle size={18} /></span><strong>{numberCompact(total)}</strong><span>Plantas en merma</span><small>afectan disponibilidad</small></article>
        <article className="summary-pill summary-pill--blue"><span className="summary-pill__icon"><MinusCircle size={18} /></span><strong>{active.length}</strong><span>Mermas activas</span><small>con motivo registrado</small></article>
        <article className="summary-pill summary-pill--purple"><span className="summary-pill__icon"><ArchiveX size={18} /></span><strong>{items.length - active.length}</strong><span>Anuladas</span><small>no afectan el saldo</small></article>
      </section>

      <section className="panel-card panel-card--interactive">
        <div className="module-toolbar-card module-toolbar-card--filters">
          <label className="filter-toolbar__search"><Search size={16} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Buscar por lote, motivo, etapa o responsable" /></label>
        </div>
        {error ? <EmptyState icon={<AlertTriangle size={28} />} title="No se pudieron cargar las mermas" description={error} action={<button type="button" className="action-button" onClick={() => void reload()}>Reintentar</button>} /> : null}
        {!error && !loading && filtered.length === 0 ? <EmptyState icon={<MinusCircle size={28} />} title="Aún no hay mermas registradas" description="Cuando exista una pérdida o ajuste justificado, podrás registrarlo desde esta sección." action={lotesTrazables.length > 0 ? <button type="button" className="action-button" onClick={() => setCreating(true)}>Registrar merma</button> : undefined} /> : null}
        {filtered.length > 0 ? (
          <div className="data-table-wrap">
            <table className="data-table">
              <thead><tr><th>Lote trazable</th><th>Etapa</th><th>Motivo</th><th>Cantidad</th><th>Fecha</th><th>Estado</th><th /></tr></thead>
              <tbody>{filtered.map((item) => (
                <tr key={item.id}>
                  <td><strong>{item.loteTrazable?.codigo || 'Sin referencia'}</strong></td>
                  <td>{item.etapaOrigen}</td><td>{item.motivo}</td><td>{numberCompact(item.cantidad || 0)}</td><td>{dateShort(item.fechaMerma)}</td><td><StatusBadge value={item.estado} /></td>
                  <td>{String(item.estado || '').toUpperCase() !== 'ANULADA' ? <button type="button" className="icon-action icon-action--danger" title="Anular merma" onClick={() => setPending(item)}><ArchiveX size={15} /></button> : null}</td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        ) : null}
      </section>

      <Modal open={creating} title="Registrar merma" description="La merma modifica la disponibilidad del lote trazable y requiere un motivo claro." icon={<MinusCircle size={20} />} onClose={() => setCreating(false)}>
        {creating ? <MermaForm lotesTrazables={lotesTrazables} onSubmit={create} onCancel={() => setCreating(false)} /> : null}
      </Modal>

      {pending ? (
        <Modal open title="Anular merma" description="Indica el motivo por el que este registro debe dejar de afectar la disponibilidad." size="sm" onClose={() => setPending(null)}>
          <form className="form-shell" onSubmit={(event) => { event.preventDefault(); void annul(); }}>
            <FormSection title="Motivo de anulación" description="Esta acción conserva el historial de la merma y registra la justificación.">
              <div className="form-grid"><label className="form-grid__full"><span>Motivo</span><textarea value={reason} onChange={(event) => setReason(event.target.value)} required maxLength={255} placeholder="Explica la corrección realizada" /></label></div>
            </FormSection>
            <footer className="form-actions form-actions--sticky"><button type="button" className="ghost-button" onClick={() => setPending(null)} disabled={saving}>Cancelar</button><button type="submit" className="action-button" disabled={saving || !reason.trim()}>{saving ? 'Anulando...' : 'Confirmar anulación'}</button></footer>
          </form>
        </Modal>
      ) : null}
    </main>
  );
}
