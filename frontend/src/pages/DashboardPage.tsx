import { AlertTriangle, ArrowRight, CheckCircle2, ClipboardList, Factory, Leaf, ListChecks, MoreVertical, PackageCheck, Route, Sprout, Tag, Truck } from 'lucide-react';
import type { CSSProperties } from 'react';
import { dateShort, numberCompact } from '../lib/format';
import type {
  CamaResponse,
  ClasificacionResponse,
  DashboardApiResponse,
  DespachoResponse,
  LoteResponse,
  OperationReadinessResponse,
  ProcesoOperativoResponse,
  SiembraResponse,
  LoteTrazableResponse
} from '../types/api';

interface DashboardPageProps {
  dashboard: DashboardApiResponse | null;
  readiness: OperationReadinessResponse | null;
  lotes: LoteResponse[];
  camas: CamaResponse[];
  siembras: SiembraResponse[];
  procesos: ProcesoOperativoResponse | null;
  clasificaciones: ClasificacionResponse[];
  despachos: DespachoResponse[];
  lotesTrazables: LoteTrazableResponse[];
  availableModuleKeys: string[];
  onNavigate: (key: string) => void;
}

type ActivityTone = 'green' | 'purple' | 'amber' | 'slate';

interface ActivityEntry {
  id: string;
  tone: ActivityTone;
  title: string;
  meta: string;
  time: string;
  icon: typeof Sprout;
}

function valueOf(value: number | null | undefined) {
  return Number.isFinite(value || 0) ? Number(value || 0) : 0;
}

function recordDate(...values: Array<string | null | undefined>) {
  return values.find(Boolean) || null;
}

function byLatestDate<T>(items: T[], dateOf: (item: T) => string | null | undefined) {
  return [...items].sort((left, right) => new Date(dateOf(right) || 0).getTime() - new Date(dateOf(left) || 0).getTime());
}

function actorName(value: { usuarioRegistro?: { nombreCompleto?: string | null } | null }) {
  return value.usuarioRegistro?.nombreCompleto || 'Sistema';
}

function statusClass(value?: string | null) {
  const source = (value || '').toLowerCase();
  if (/activo|producci|validado|completado|aprob/i.test(source)) return 'success';
  if (/proceso|pendiente|revisi/i.test(source)) return 'warning';
  if (/observ|anulado|inactivo|rechaz/i.test(source)) return 'danger';
  return 'neutral';
}

interface TraceProgressSummary {
  siembras: number;
  uniformizaciones: number;
  formalizaciones: number;
  clasificaciones: number;
  despachos: number;
}

function traceProgress(trace?: TraceProgressSummary | null) {
  return [
    { label: 'Siembra', active: valueOf(trace?.siembras) > 0, icon: Sprout },
    { label: 'Uniformización', active: valueOf(trace?.uniformizaciones) > 0, icon: Leaf },
    { label: 'Formalización', active: valueOf(trace?.formalizaciones) > 0, icon: ClipboardList },
    { label: 'Clasificación', active: valueOf(trace?.clasificaciones) > 0, icon: Tag },
    { label: 'Despachado', active: valueOf(trace?.despachos) > 0, icon: Truck }
  ];
}

function monthName(value?: string | null) {
  const parsed = new Date(value || '');
  if (Number.isNaN(parsed.getTime())) return 'Actual';
  return parsed.toLocaleString('es-PE', { month: 'short' }).replace('.', '');
}

function buildRendimiento(siembras: SiembraResponse[]) {
  const totals = new Map<string, number>();
  siembras
    .filter((item) => Boolean(item.loteTrazable?.id) && String(item.estado || '').toUpperCase() === 'REGISTRADA')
    .forEach((item) => {
      const key = item.loteTrazable?.codigo || 'Sin lote trazable';
      const amount = valueOf(item.cantidadRegistrada);
      if (amount > 0) totals.set(key, (totals.get(key) || 0) + amount);
    });

  const bars = [...totals.entries()]
    .map(([label, value]) => ({ label, value }))
    .sort((left, right) => right.value - left.value)
    .slice(0, 5);
  const max = Math.max(...bars.map((item) => item.value), 1);
  return { bars, max };
}

function percentLabel(value: number | null | undefined) {
  const normalized = Math.max(0, Math.min(100, valueOf(value)));
  return `${Number.isInteger(normalized) ? normalized : normalized.toFixed(1)}%`;
}

function dashboardMeta(total: number, label: string, percent?: number | null) {
  const totalText = `${numberCompact(total)} ${label}`;
  return percent === undefined || percent === null ? totalText : `${totalText} · ${percentLabel(percent)} activo`;
}

export function DashboardPage({ dashboard, readiness, lotes, camas, siembras, procesos, clasificaciones, despachos, lotesTrazables, availableModuleKeys, onNavigate }: DashboardPageProps) {
  const summary = dashboard?.summary;
  const activeLots = summary?.lotesActivos ?? lotes.filter((lote) => (lote.estado || '').toUpperCase() === 'ACTIVO').length;
  const activeBeds = summary?.camasActivas ?? camas.filter((cama) => (cama.estado || '').toUpperCase() === 'ACTIVA').length;
  const traceableSiembras = siembras.filter((item) => Boolean(item.loteTrazable?.id) && String(item.estado || '').toUpperCase() === 'REGISTRADA');
  const traceableUniformizaciones = (procesos?.uniformizaciones.items || []).filter((item) => Boolean(item.loteTrazable?.id) && String(item.estado || '').toUpperCase() === 'REGISTRADA');
  const traceableFormalizaciones = (procesos?.formalizaciones.items || []).filter((item) => Boolean(item.loteTrazable?.id) && String(item.estado || '').toUpperCase() === 'REGISTRADA');
  const traceableClasificaciones = clasificaciones.filter((item) => Boolean(item.loteTrazable?.id) && /PENDIENTE|VALIDADA|OBSERVADA/i.test(item.estado || ''));
  const traceableDespachos = despachos.filter((item) => Boolean(item.loteTrazable?.id && item.clasificacion?.id));
  const confirmedDispatches = traceableDespachos.filter((item) => String(item.estado || '').toUpperCase() === 'DESPACHADO');
  const planted = summary?.plantasSembradas ?? traceableSiembras.reduce((total, item) => total + valueOf(item.cantidadRegistrada), 0);
  const dispatches = summary?.despachosRegistrados ?? confirmedDispatches.length;
  const shipped = summary?.plantasDespachadas ?? confirmedDispatches.reduce((total, item) => total + valueOf(item.cantidadDespachada), 0);
  const selectedTrace = byLatestDate(lotesTrazables.filter((item) => String(item.estado || '').toUpperCase() === 'ACTIVO'), (item) => item.fechaActualizacion || item.fechaIngreso || item.fechaCreacion)[0] || null;
  const selectedTraceId = selectedTrace?.id;
  const selectedTraceSummary: TraceProgressSummary | null = selectedTraceId ? {
    siembras: traceableSiembras.filter((item) => item.loteTrazable?.id === selectedTraceId).length,
    uniformizaciones: traceableUniformizaciones.filter((item) => item.loteTrazable?.id === selectedTraceId).length,
    formalizaciones: traceableFormalizaciones.filter((item) => item.loteTrazable?.id === selectedTraceId).length,
    clasificaciones: traceableClasificaciones.filter((item) => item.loteTrazable?.id === selectedTraceId).length,
    despachos: confirmedDispatches.filter((item) => item.loteTrazable?.id === selectedTraceId).length
  } : null;
  const selectedTraceLastEvent = selectedTraceId ? [
    ...traceableSiembras.filter((item) => item.loteTrazable?.id === selectedTraceId).map((item) => recordDate(item.fechaActualizacion, item.fechaCreacion, item.fechaSiembra)),
    ...traceableUniformizaciones.filter((item) => item.loteTrazable?.id === selectedTraceId).map((item) => recordDate(item.fechaActualizacion, item.fechaCreacion, item.fechaUniformizacion)),
    ...traceableFormalizaciones.filter((item) => item.loteTrazable?.id === selectedTraceId).map((item) => recordDate(item.fechaActualizacion, item.fechaCreacion, item.fechaFormalizacion)),
    ...traceableClasificaciones.filter((item) => item.loteTrazable?.id === selectedTraceId).map((item) => recordDate(item.fechaActualizacion, item.fechaCreacion, item.fechaClasificacion)),
    ...confirmedDispatches.filter((item) => item.loteTrazable?.id === selectedTraceId).map((item) => recordDate(item.fechaActualizacion, item.fechaCreacion, item.fechaDespacho))
  ].filter((value): value is string => Boolean(value)).sort((a, b) => new Date(b).getTime() - new Date(a).getTime())[0] || null : null;
  const recentLots = byLatestDate(lotes, (item) => recordDate(item.fechaActualizacion, item.fechaCreacion, item.fechaRegistro)).slice(0, 5);
  const rendimiento = buildRendimiento(traceableSiembras);

  const recentActivity: ActivityEntry[] = [
    ...confirmedDispatches.map((item) => ({
      id: `despacho-${item.id}`,
      tone: 'purple' as ActivityTone,
      title: `Despacho registrado`,
      meta: `${item.loteTrazable?.codigo || 'Lote trazable'} · ${numberCompact(item.cantidadDespachada)} plantas`,
      time: dateShort(recordDate(item.fechaActualizacion, item.fechaCreacion, item.fechaDespacho)),
      icon: Truck
    })),
    ...traceableSiembras.map((item) => ({
      id: `siembra-${item.id}`,
      tone: 'green' as ActivityTone,
      title: `Siembra registrada`,
      meta: `${item.loteTrazable?.codigo || 'Lote trazable'} · ${numberCompact(item.cantidadRegistrada)} plantas`,
      time: dateShort(recordDate(item.fechaActualizacion, item.fechaCreacion, item.fechaSiembra)),
      icon: Sprout
    })),
    ...traceableUniformizaciones.map((item) => ({
      id: `uniformizacion-${item.id}`,
      tone: 'amber' as ActivityTone,
      title: `Uniformización completada`,
      meta: `${item.loteTrazable?.codigo || 'Lote trazable'} · ${item.cama?.codigo || 'Cama'}`,
      time: dateShort(recordDate(item.fechaActualizacion, item.fechaCreacion, item.fechaUniformizacion)),
      icon: Leaf
    })),
    ...traceableClasificaciones.map((item) => ({
      id: `clasificacion-${item.id}`,
      tone: 'slate' as ActivityTone,
      title: `Clasificación registrada`,
      meta: `${item.loteTrazable?.codigo || 'Lote trazable'} · ${numberCompact(item.cantidad)} plantas`,
      time: dateShort(recordDate(item.fechaActualizacion, item.fechaCreacion, item.fechaClasificacion)),
      icon: Tag
    }))
  ].slice(0, 4);

  const pendingQuality = traceableClasificaciones.filter((item) => /PENDIENTE|OBSERVADA/i.test(item.estado || '')).length;
  const observedDispatches = traceableDespachos.filter((item) => /OBSERVADO|ANULADO/i.test(item.estado || item.validacionCalidad || '')).length;
  const inactiveBeds = camas.filter((item) => (item.estado || '').toUpperCase() !== 'ACTIVA').length;
  const hasOperationalData = lotesTrazables.length + traceableSiembras.length + traceableClasificaciones.length + traceableDespachos.length > 0;
  const hasBalanceInconsistency = planted > 0 && shipped > planted;
  const alerts: Array<{
    icon: typeof AlertTriangle;
    tone: 'amber' | 'purple' | 'green';
    title: string;
    text: string;
    time: string;
  }> = [];

  if (hasBalanceInconsistency) {
    alerts.push({
      icon: AlertTriangle,
      tone: 'amber',
      title: 'Saldo requiere revisión',
      text: 'Hay más plantas despachadas que sembradas en los movimientos trazables. Revisa los registros antes de continuar.',
      time: 'Actual'
    });
  }

  if (pendingQuality > 0) {
    alerts.push({
      icon: AlertTriangle,
      tone: 'amber',
      title: 'Clasificación pendiente',
      text: `${pendingQuality} registros requieren revisión de calidad.`,
      time: 'Actual'
    });
  }

  if (observedDispatches > 0) {
    alerts.push({
      icon: Truck,
      tone: 'purple',
      title: 'Despachos observados',
      text: `${observedDispatches} despachos necesitan seguimiento operativo.`,
      time: 'Actual'
    });
  }

  if (inactiveBeds > 0) {
    alerts.push({
      icon: AlertTriangle,
      tone: 'amber',
      title: 'Camas no activas',
      text: `${inactiveBeds} camas figuran fuera de operación.`,
      time: 'Sistema'
    });
  }

  if (alerts.length === 0 && hasOperationalData) {
    alerts.push({
      icon: CheckCircle2,
      tone: 'green',
      title: 'Sin alertas operativas',
      text: 'Los datos actuales no generan observaciones críticas.',
      time: 'Actual'
    });
  }

  const metricCards = [
    {
      label: 'Lotes activos',
      value: activeLots,
      meta: dashboardMeta(summary?.lotesRegistrados ?? lotes.length, 'lotes registrados', summary?.porcentajeLotesActivos),
      icon: Leaf,
      tone: 'green',
      spark: 'M2 28 C14 24 18 32 30 18 S52 22 62 9'
    },
    {
      label: 'Camas operativas',
      value: activeBeds,
      meta: dashboardMeta(summary?.camasRegistradas ?? camas.length, 'camas registradas', summary?.porcentajeCamasActivas),
      icon: Sprout,
      tone: 'green',
      spark: 'M2 30 C12 26 18 30 26 18 S42 8 62 15'
    },
    {
      label: 'Siembras trazables',
      value: summary?.siembrasRegistradas ?? traceableSiembras.length,
      meta: `${numberCompact(planted)} plantas registradas`,
      icon: Factory,
      tone: 'purple',
      spark: 'M2 27 C15 24 20 19 31 23 S51 16 62 7'
    },
    {
      label: 'Despachos confirmados',
      value: dispatches,
      meta: `${numberCompact(shipped)} plantas despachadas`,
      icon: Truck,
      tone: 'purple',
      spark: 'M2 31 C12 18 22 29 32 17 S51 10 62 14'
    }
  ];

  const traceSteps = traceProgress(selectedTraceSummary);
  const completedTraceSteps = traceSteps.filter((step) => step.active).length;
  const tracePercent = selectedTrace && completedTraceSteps > 0
    ? ((completedTraceSteps - 1) / Math.max(traceSteps.length - 1, 1)) * 100
    : 0;
  const traceLineStyle = { '--trace-progress': `${tracePercent}%` } as CSSProperties;
  const requiredModuleForStep = (key: string) => {
    if (key === 'uniformizaciones' || key === 'formalizaciones') return 'procesos';
    if (key === 'lotes-trazables') return 'lotes_trazables';
    return key;
  };
  const visibleReadinessSteps = readiness?.steps.filter((step) => availableModuleKeys.includes(requiredModuleForStep(step.key))) || [];
  const recommendedStep = visibleReadinessSteps.find((step) => step.key === readiness?.recommendedKey)
    || visibleReadinessSteps.find((step) => step.available && !step.completed)
    || null;

  return (
    <main className="content-grid dashboard-screen dashboard-screen--vlv">
      <section className="vlv-dashboard-title">
        <div>
          <h1>Panel principal</h1>
          <p>Resumen general de operaciones</p>
        </div>
        <span>Información operativa actualizada</span>
      </section>

      <section className="vlv-metric-grid">
        {metricCards.map((metric) => {
          const Icon = metric.icon;
          return (
            <article className={`vlv-metric-card vlv-metric-card--${metric.tone}`} key={metric.label}>
              <div className="vlv-metric-card__icon"><Icon size={28} /></div>
              <div className="vlv-metric-card__body">
                <span>{metric.label}</span>
                <strong>{numberCompact(metric.value)}</strong>
                <small className="vlv-metric-card__meta">{metric.meta}</small>
              </div>
              <svg className="vlv-metric-card__spark" viewBox="0 0 64 36" aria-hidden="true">
                <path d={metric.spark} />
              </svg>
            </article>
          );
        })}
      </section>

      {readiness && recommendedStep && visibleReadinessSteps.length > 0 ? (
        <section className="workflow-readiness" aria-label="Preparación operativa">
          <div className="workflow-readiness__intro">
            <span className="workflow-readiness__icon"><ListChecks size={20} /></span>
            <div>
              <span className="workflow-readiness__eyebrow">{readiness.title}</span>
              <h2>{recommendedStep.title}</h2>
              <p>{recommendedStep.description}</p>
            </div>
            <button type="button" className="action-button" onClick={() => onNavigate(recommendedStep.key)}>
              {recommendedStep.actionLabel} <ArrowRight size={16} />
            </button>
          </div>
          <div className="workflow-readiness__steps">
            {visibleReadinessSteps.map((step, index) => (
              <button
                key={step.key}
                type="button"
                className={`workflow-step ${step.completed ? 'workflow-step--completed' : ''} ${step.available ? 'workflow-step--available' : 'workflow-step--locked'}`}
                onClick={() => step.available && onNavigate(step.key)}
                disabled={!step.available}
                title={step.description}
              >
                <span>{step.completed ? <CheckCircle2 size={16} /> : index + 1}</span>
                <strong>{step.title}</strong>
                <small>{step.completed ? `${numberCompact(step.completedItems)} registros` : step.available ? 'Disponible' : 'Disponible al completar el paso anterior'}</small>
              </button>
            ))}
          </div>
        </section>
      ) : null}

      <section className="vlv-dashboard-main-grid">
        <article className="vlv-panel-card vlv-panel-card--table">
          <header className="vlv-panel-header">
            <div>
              <ClipboardList size={18} />
              <h2>Lotes recientes</h2>
            </div>
            <button type="button" onClick={() => onNavigate('lotes')}>Ver invernaderos</button>
          </header>

          <div className="vlv-table-wrap">
            <table className="vlv-table">
              <thead>
                <tr>
                  <th>Lote</th>
                  <th>Variedad</th>
                  <th>Estado</th>
                  <th>Fecha</th>
                  <th>Responsable</th>
                  <th>Acción</th>
                </tr>
              </thead>
              <tbody>
                {recentLots.length > 0 ? recentLots.map((lote) => (
                  <tr key={lote.id}>
                    <td>{lote.codigo}</td>
                    <td>{lote.variedad || lote.cultivo || 'Sin variedad'}</td>
                    <td><span className={`vlv-status vlv-status--${statusClass(lote.estado)}`}>{lote.estado || 'Sin estado'}</span></td>
                    <td>{dateShort(recordDate(lote.fechaActualizacion, lote.fechaCreacion, lote.fechaRegistro))}</td>
                    <td>{actorName(lote)}</td>
                    <td><button className="vlv-row-action" type="button" aria-label={`Ver invernadero ${lote.codigo}`} onClick={() => onNavigate('lotes')}><MoreVertical size={16} /></button></td>
                  </tr>
                )) : (
                  <tr><td colSpan={6} className="vlv-table-empty">Aún no se han registrado invernaderos. Registra el primero para comenzar la operación.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </article>

        <article className="vlv-panel-card vlv-trace-card">
          <header className="vlv-panel-header">
            <div>
              <Route size={18} />
              <h2>Trazabilidad por lote</h2>
            </div>
            <button type="button" onClick={() => onNavigate('trazabilidad')}>Ver trazabilidad</button>
          </header>

          <div className="vlv-trace-card__title">
            <strong>{selectedTrace?.codigo || 'Sin lote trazable seleccionado'}</strong>
            <span>{selectedTrace ? `${selectedTrace.variedad} · ${selectedTrace.loteFisico?.codigo || 'Sin invernadero'} · ${selectedTrace.camaInicial?.codigo || 'Sin cama'}` : 'Seguimiento trazable disponible al registrar un lote de plantas'}</span>
          </div>

          <div className={selectedTrace ? 'vlv-trace-line vlv-trace-line--with-data' : 'vlv-trace-line vlv-trace-line--empty'} style={traceLineStyle}>
            {traceSteps.map((step) => {
              const Icon = step.icon;
              return (
                <div className={step.active ? 'vlv-trace-step vlv-trace-step--active' : 'vlv-trace-step'} key={step.label}>
                  <span><Icon size={18} /></span>
                  <strong>{step.label}</strong>
                  <small>{step.active ? monthName(selectedTraceLastEvent) : 'Pendiente'}</small>
                </div>
              );
            })}
          </div>

          <div className="vlv-trace-summary">
            <div>
              <span>Estado actual</span>
              <strong>{selectedTrace ? valueOf(selectedTraceSummary?.despachos) > 0 ? 'Despachado' : valueOf(selectedTraceSummary?.clasificaciones) > 0 ? 'Clasificado' : valueOf(selectedTraceSummary?.siembras) > 0 ? 'En producción' : 'Sin movimientos' : 'Sin seguimiento'}</strong>
            </div>
            <div>
              <span>Último evento</span>
              <strong>{selectedTrace ? dateShort(selectedTraceLastEvent) : 'Sin fecha'}</strong>
            </div>
          </div>
        </article>
      </section>

      <section className="vlv-dashboard-bottom-grid">
        <article className="vlv-panel-card vlv-bars-card">
          <header className="vlv-panel-header">
            <div>
              <Factory size={18} />
              <h2>Rendimiento agrícola</h2>
            </div>
            <button type="button" onClick={() => onNavigate('reportes')}>Ver reportes</button>
          </header>
          <div className="vlv-bars">
            {rendimiento.bars.length > 0 ? rendimiento.bars.map((bar) => (
              <div className="vlv-bar" key={bar.label}>
                <span style={{ height: `${Math.max((bar.value / rendimiento.max) * 100, bar.value > 0 ? 10 : 2)}%` }} />
                <strong>{numberCompact(bar.value)}</strong>
                <small>{bar.label}</small>
              </div>
            )) : <p className="vlv-muted">El rendimiento aparecerá cuando se registren siembras.</p>}
          </div>
        </article>

        <article className="vlv-panel-card vlv-activity-card">
          <header className="vlv-panel-header">
            <div>
              <PackageCheck size={18} />
              <h2>Actividad reciente</h2>
            </div>
          </header>
          <div className="vlv-activity-list">
            {recentActivity.length > 0 ? recentActivity.map((entry) => {
              const Icon = entry.icon;
              return (
                <article className={`vlv-activity-item vlv-activity-item--${entry.tone}`} key={entry.id}>
                  <span><Icon size={18} /></span>
                  <div>
                    <strong>{entry.title}</strong>
                    <small>{entry.meta}</small>
                  </div>
                  <time>{entry.time}</time>
                </article>
              );
            }) : <p className="vlv-muted">Sin actividad reciente.</p>}
          </div>
        </article>

        <article className="vlv-panel-card vlv-alert-card">
          <header className="vlv-panel-header">
            <div>
              <AlertTriangle size={18} />
              <h2>Alertas y notificaciones</h2>
            </div>
            <button type="button" onClick={() => onNavigate('reportes')}>Ver reportes</button>
          </header>
          <div className="vlv-alert-list">
            {alerts.length > 0 ? alerts.map((alert) => {
              const Icon = alert.icon;
              return (
                <article className={`vlv-alert-item vlv-alert-item--${alert.tone}`} key={alert.title}>
                  <span><Icon size={18} /></span>
                  <div>
                    <strong>{alert.title}</strong>
                    <small>{alert.text}</small>
                  </div>
                  <time>{alert.time}</time>
                </article>
              );
            }) : <p className="vlv-muted">Sin alertas generadas por los datos actuales.</p>}
          </div>
        </article>
      </section>
    </main>
  );
}
