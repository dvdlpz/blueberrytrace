import { useEffect, useMemo, useState } from 'react';
import { AlertTriangle, Download, GitBranch, Leaf, Printer, Search, Sprout, Truck } from 'lucide-react';
import { EmptyState } from '../components/EmptyState';
import { ModuleHeader } from '../components/ModuleHeader';
import { StatusBadge } from '../components/StatusBadge';
import { blueberryApi } from '../lib/api';
import { downloadCsv, printCurrentView } from '../lib/export';
import { dateShort, numberCompact } from '../lib/format';
import type { LoteTrazableDetailResponse, LoteTrazableResponse, TimelineEventResponse } from '../types/api';

interface TrazabilidadPageProps {
  lotesTrazables: LoteTrazableResponse[];
}

function eventTone(event: TimelineEventResponse) {
  const state = String(event.estado || '').toUpperCase();
  if (/ANULADA|CANCELADO|OBSERVADA|RECHAZ/.test(state)) return 'red';
  if (event.etapa === 'Despacho') return 'blue';
  if (event.etapa === 'Merma') return 'orange';
  return 'green';
}

function latestTrace(items: LoteTrazableResponse[]) {
  return [...items].sort((a, b) => new Date(b.fechaActualizacion || b.fechaIngreso || 0).getTime() - new Date(a.fechaActualizacion || a.fechaIngreso || 0).getTime())[0] || null;
}

export function TrazabilidadPage({ lotesTrazables }: TrazabilidadPageProps) {
  const [query, setQuery] = useState('');
  const [selectedId, setSelectedId] = useState('');
  const [detail, setDetail] = useState<LoteTrazableDetailResponse | null>(null);
  const [loading, setLoading] = useState(false);

  const visible = useMemo(() => {
    const search = query.trim().toLowerCase();
    return lotesTrazables.filter((item) => !search || [item.codigo, item.variedad, item.procedencia, item.estado, item.loteFisico?.codigo, item.camaInicial?.codigo]
      .some((value) => String(value || '').toLowerCase().includes(search)));
  }, [lotesTrazables, query]);

  useEffect(() => {
    if (!selectedId) {
      const preferred = latestTrace(lotesTrazables.filter((item) => String(item.estado || '').toUpperCase() === 'ACTIVO')) || latestTrace(lotesTrazables);
      if (preferred) setSelectedId(String(preferred.id));
    }
  }, [lotesTrazables, selectedId]);

  useEffect(() => {
    if (!selectedId) {
      setDetail(null);
      return;
    }
    let active = true;
    setLoading(true);
    void blueberryApi.loteTrazable(Number(selectedId))
      .then((response) => { if (active) setDetail(response); })
      .catch(() => { if (active) setDetail(null); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [selectedId]);

  function exportCsv() {
    if (!detail) return;
    downloadCsv(`blueberrytrace-trazabilidad-${detail.loteTrazable.codigo}.csv`,
      ['Fecha', 'Etapa', 'Referencia', 'Detalle', 'Cantidad', 'Estado', 'Responsable'],
      detail.lineaTiempo.map((event) => [event.fecha || '', event.etapa, event.referencia, event.detalle || '', event.cantidad ?? '', event.estado, event.responsable || '']));
  }

  const balance = detail?.balance;
  const stages = [
    { label: 'Siembra', value: balance?.sembradas || 0, icon: Sprout },
    { label: 'Uniformización', value: balance?.uniformizadas || 0, icon: Leaf },
    { label: 'Formalización', value: balance?.formalizadas || 0, icon: GitBranch },
    { label: 'Clasificación validada', value: balance?.clasificacionValidada || 0, icon: GitBranch },
    { label: 'Despacho', value: balance?.despachadas || 0, icon: Truck }
  ];

  return (
    <main className="content-grid trace-screen">
      <ModuleHeader
        eyebrow="Seguimiento operativo"
        title="Trazabilidad por lote"
        description="Consulta cada lote de plantas sin mezclar movimientos históricos con el saldo operativo actual."
        icon={<GitBranch size={21} />}
        tone="green"
        actions={<div className="button-group"><button type="button" className="ghost-button" onClick={exportCsv} disabled={!detail}><Download size={15} /> Exportar CSV</button><button type="button" className="ghost-button" onClick={printCurrentView}><Printer size={15} /> Imprimir</button></div>}
      />

      <section className="panel-card trace-selector-card">
        <div className="panel-card__header"><div><h2>Selecciona un lote trazable</h2><p>El saldo y la línea de tiempo se calculan únicamente con movimientos vinculados a esta unidad de plantas.</p></div></div>
        <div className="trace-control-panel__search">
          <label className="filter-toolbar__search trace-search-control"><span className="sr-only">Buscar lote trazable</span><Search size={16} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Buscar código, variedad, procedencia o ubicación..." /></label>
          <label className="trace-select-control"><span>Lote trazable</span><select value={selectedId} onChange={(event) => setSelectedId(event.target.value)}><option value="">Selecciona un lote trazable</option>{visible.map((item) => <option key={item.id} value={item.id}>{item.codigo} · {item.variedad} · {item.procedencia}</option>)}</select></label>
        </div>
      </section>

      {loading ? <section className="panel-card"><p className="muted-text">Cargando la trazabilidad del lote...</p></section> : null}

      {detail && balance ? <>
        <section className="trace-kpi-grid">
          <article className="trace-kpi trace-kpi--green"><span><Sprout size={17} /></span><div><strong>{numberCompact(balance.sembradas)}</strong><small>Plantas sembradas</small></div></article>
          <article className="trace-kpi trace-kpi--purple"><span><GitBranch size={17} /></span><div><strong>{numberCompact(balance.clasificacionValidada)}</strong><small>Clasificadas y validadas</small></div></article>
          <article className="trace-kpi trace-kpi--blue"><span><Truck size={17} /></span><div><strong>{numberCompact(balance.despachadas)}</strong><small>Plantas despachadas</small></div></article>
          <article className="trace-kpi trace-kpi--green"><span><Leaf size={17} /></span><div><strong>{numberCompact(balance.saldoDisponible)}</strong><small>Saldo disponible</small></div></article>
          <article className="trace-kpi trace-kpi--orange"><span><AlertTriangle size={17} /></span><div><strong>{numberCompact(balance.enRecuperacion)}</strong><small>En recuperación por riego</small></div></article>
        </section>

        <section className="trace-overview-grid">
          <article className="panel-card trace-lot-card">
            <div className="trace-lot-card__header"><span><GitBranch size={22} /></span><div><strong>{detail.loteTrazable.codigo}</strong><small>{detail.loteTrazable.variedad} · {detail.loteTrazable.procedencia}</small></div><StatusBadge value={detail.loteTrazable.estado} /></div>
            <div className="trace-lot-card__facts">
              <article className="trace-lot-card__fact"><span>Invernadero</span><strong>{detail.loteTrazable.loteFisico?.codigo || 'Sin invernadero'}</strong></article>
              <article className="trace-lot-card__fact"><span>Cama inicial</span><strong>{detail.loteTrazable.camaInicial?.codigo || 'Sin cama'}</strong></article>
              <article className="trace-lot-card__fact"><span>Mermas</span><strong>{numberCompact(balance.mermas)}</strong></article>
              <article className="trace-lot-card__fact"><span>En recuperación</span><strong>{numberCompact(balance.enRecuperacion)}</strong></article>
              <article className="trace-lot-card__fact"><span>Anulaciones</span><strong>{numberCompact(balance.anuladas)}</strong></article>
            </div>
          </article>
          <article className="panel-card trace-flow-card"><div className="panel-card__header"><div><h2>Flujo operativo</h2><p>Resumen de cantidades por etapa.</p></div></div><div className="trace-stage-row">{stages.map(({ label, value, icon: Icon }) => <div key={label} className={value > 0 ? 'trace-stage trace-stage--done' : 'trace-stage'}><Icon size={18} /><strong>{label}</strong><span>{numberCompact(value)}</span></div>)}</div></article>
        </section>

        {detail.pendientesLegado.length > 0 ? <section className="operational-notice operational-notice--warning"><AlertTriangle size={18} /><div><strong>Hay movimientos históricos pendientes de revisión.</strong><span>Se conservan como referencia y no se incluyen en el saldo operativo hasta contar con un vínculo verificable.</span></div></section> : null}

        <section className="panel-card trace-timeline-card"><div className="panel-card__header"><div><h2>Línea de tiempo operativa</h2><p>Eventos vinculados al lote trazable seleccionado.</p></div><span className="panel-card__count">{detail.lineaTiempo.length} eventos</span></div>{detail.lineaTiempo.length > 0 ? <div className="trace-timeline">{detail.lineaTiempo.map((event, index) => <article key={`${event.etapa}-${event.referencia}-${index}`} className={`trace-event trace-event--${eventTone(event)}`}><span className="trace-event__dot" /><div className="trace-event__body"><header><div><strong>{event.referencia}</strong><small>{event.etapa} · {dateShort(event.fecha)}</small></div><StatusBadge value={event.estado} /></header><p>{event.detalle || 'Movimiento registrado.'}</p><footer><span>{event.responsable || 'Sin responsable registrado'}</span>{event.cantidad !== null ? <b>{numberCompact(event.cantidad)} plantas</b> : null}</footer></div></article>)}</div> : <EmptyState compact icon={<GitBranch size={24} />} title="Sin movimientos vinculados" description="Registra las etapas operativas para construir la trazabilidad del lote." />}</section>
      </> : !loading ? <EmptyState icon={<GitBranch size={28} />} title="Aún no hay lotes trazables" description="Registra un lote trazable para comenzar a controlar la cadena de plantas." /> : null}
    </main>
  );
}
