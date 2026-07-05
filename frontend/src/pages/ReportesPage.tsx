import { useEffect, useMemo, useRef, useState } from 'react';
import { BarChart3, Download, Eye, FileSpreadsheet, FileText, Leaf, Tags, Truck, type LucideIcon } from 'lucide-react';
import vlvLogo from '../assets/vlv-logo.png';
import { EmptyState } from '../components/EmptyState';
import { ModuleHeader } from '../components/ModuleHeader';
import { dateShort, numberCompact } from '../lib/format';
import { blueberryApi } from '../lib/api';
import { downloadTechnicalCsv, downloadTechnicalPdf, downloadTechnicalXlsx, type TechnicalReportCell, type TechnicalReportData } from '../lib/technicalReport';
import type {
  AuthenticatedUserResponse,
  ClasificacionResponse,
  DespachoResponse,
  LoteResponse,
  ProcesoOperativoResponse,
  ReferenceResponse,
  SiembraResponse,
  CamaResponse,
  LoteTrazableResponse,
  MermaResponse,
  AuditResponse
} from '../types/api';

interface ReportesPageProps {
  lotes: LoteResponse[];
  camas: CamaResponse[];
  siembras: SiembraResponse[];
  procesos: ProcesoOperativoResponse | null;
  clasificaciones: ClasificacionResponse[];
  despachos: DespachoResponse[];
  lotesTrazables: LoteTrazableResponse[];
  user: AuthenticatedUserResponse | null;
  availableModuleKeys: string[];
}

type ReportType = 'trazabilidad' | 'produccion' | 'clasificacion' | 'despachos' | 'mermas' | 'auditoria';
type ReportRow = Record<string, TechnicalReportCell> & { _loteId?: number | null; _date?: string | null };

interface ReportDefinition {
  key: ReportType;
  label: string;
  description: string;
  filename: string;
  icon: LucideIcon;
  headers: string[];
  rows: ReportRow[];
}

const reportOptions: Array<{ key: ReportType; label: string }> = [
  { key: 'trazabilidad', label: 'Trazabilidad por lote' },
  { key: 'produccion', label: 'Producción por lote' },
  { key: 'clasificacion', label: 'Clasificación y calidad' },
  { key: 'despachos', label: 'Despachos operativos' },
  { key: 'mermas', label: 'Mermas y mortalidad' },
  { key: 'auditoria', label: 'Auditoría administrativa' }
];

const allLots = 'TODOS';

function uniqueLots(...groups: Array<Array<ReferenceResponse | LoteResponse | null | undefined>>) {
  const map = new Map<number, ReferenceResponse>();
  groups.flat().forEach((item) => {
    if (!item?.id) return;
    map.set(item.id, { id: item.id, codigo: item.codigo, descripcion: item.descripcion || null });
  });
  return Array.from(map.values()).sort((left, right) => left.codigo.localeCompare(right.codigo));
}

function lotId(item: { lote?: ReferenceResponse | null }) {
  return item.lote?.id || null;
}

function inRange(value: string | null | undefined, from: string, to: string) {
  if (!value) return !from && !to;
  const onlyDate = value.slice(0, 10);
  return (!from || onlyDate >= from) && (!to || onlyDate <= to);
}

function safeNumber(value: TechnicalReportCell) {
  return typeof value === 'number' && Number.isFinite(value) ? value : 0;
}

function fileSuffix(reportType: ReportType, selectedLote: string, lotOptions: ReferenceResponse[]) {
  const lot = selectedLote === allLots ? 'todos-los-lotes' : lotOptions.find((item) => String(item.id) === selectedLote)?.codigo || 'lote';
  return `${reportType}-${lot}`.toLowerCase().replace(/[^a-z0-9-_]+/gi, '-');
}

function periodLabel(from: string, to: string) {
  if (from && to) return `${dateShort(from)} al ${dateShort(to)}`;
  if (from) return `Desde ${dateShort(from)}`;
  if (to) return `Hasta ${dateShort(to)}`;
  return 'Todo el período disponible';
}

export function ReportesPage({ lotes: _lotes, camas: _camas, siembras, procesos, clasificaciones, despachos, lotesTrazables, user, availableModuleKeys }: ReportesPageProps) {
  const [reportType, setReportType] = useState<ReportType>('trazabilidad');
  const [selectedLote, setSelectedLote] = useState(allLots);
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [previewed, setPreviewed] = useState(false);
  const [generatedAt, setGeneratedAt] = useState<string | null>(null);
  const [mermas, setMermas] = useState<MermaResponse[]>([]);
  const [auditoria, setAuditoria] = useState<AuditResponse[]>([]);
  const previewRef = useRef<HTMLElement | null>(null);

  const canReadMermas = availableModuleKeys.includes('mermas');
  const canReadAuditoria = availableModuleKeys.includes('auditoria');

  useEffect(() => {
    let live = true;
    if (canReadMermas) {
      void blueberryApi.mermas().then((items) => { if (live) setMermas(items); }).catch(() => { if (live) setMermas([]); });
    } else {
      setMermas([]);
    }
    if (canReadAuditoria) {
      void blueberryApi.auditoria({ size: 100 }).then((page) => { if (live) setAuditoria(page.content); }).catch(() => { if (live) setAuditoria([]); });
    } else {
      setAuditoria([]);
      if (reportType === 'auditoria') setReportType('trazabilidad');
    }
    return () => { live = false; };
  }, [canReadAuditoria, canReadMermas, reportType]);

  const uniformizaciones = procesos?.uniformizaciones.items || [];
  const formalizaciones = procesos?.formalizaciones.items || [];

  const traceableSiembras = useMemo(() => siembras.filter((item) => Boolean(item.loteTrazable?.id) && String(item.estado || '').toUpperCase() === 'REGISTRADA'), [siembras]);
  const traceableUniformizaciones = useMemo(() => uniformizaciones.filter((item) => Boolean(item.loteTrazable?.id) && String(item.estado || '').toUpperCase() === 'REGISTRADA'), [uniformizaciones]);
  const traceableFormalizaciones = useMemo(() => formalizaciones.filter((item) => Boolean(item.loteTrazable?.id) && String(item.estado || '').toUpperCase() === 'REGISTRADA'), [formalizaciones]);
  const traceableClasificaciones = useMemo(() => clasificaciones.filter((item) => Boolean(item.loteTrazable?.id) && /PENDIENTE|VALIDADA|OBSERVADA/i.test(item.estado || '')), [clasificaciones]);
  const traceableDespachos = useMemo(() => despachos.filter((item) => Boolean(item.loteTrazable?.id && item.clasificacion?.id)), [despachos]);
  const confirmedDespachos = useMemo(() => traceableDespachos.filter((item) => String(item.estado || '').toUpperCase() === 'DESPACHADO'), [traceableDespachos]);

  const lotOptions = useMemo(() => uniqueLots(lotesTrazables.map((item) => item.loteFisico)), [lotesTrazables]);

  const reportDefinitions = useMemo<Record<ReportType, ReportDefinition>>(() => {
    const trazabilidadRows = lotesTrazables.map((trace) => {
      const traceId = trace.id;
      const traceSiembras = traceableSiembras.filter((item) => item.loteTrazable?.id === traceId);
      const traceUniformizaciones = traceableUniformizaciones.filter((item) => item.loteTrazable?.id === traceId);
      const traceFormalizaciones = traceableFormalizaciones.filter((item) => item.loteTrazable?.id === traceId);
      const traceClasificaciones = traceableClasificaciones.filter((item) => item.loteTrazable?.id === traceId);
      const traceDespachos = confirmedDespachos.filter((item) => item.loteTrazable?.id === traceId);
      const traceMermasClasificacion = mermas.filter((item) =>
        item.loteTrazable?.id === traceId
        && String(item.estado || '').toUpperCase() === 'REGISTRADA'
        && String(item.etapaOrigen || '').toUpperCase() === 'CLASIFICACION'
      );
      const planted = traceSiembras.reduce((total, item) => total + (item.cantidadRegistrada || 0), 0);
      const dispatched = traceDespachos.reduce((total, item) => total + (item.cantidadDespachada || 0), 0);
      const losses = traceMermasClasificacion.reduce((total, item) => total + (item.cantidad || 0), 0);
      const validated = traceClasificaciones
        .filter((item) => String(item.estado || '').toUpperCase() === 'VALIDADA')
        .reduce((total, item) => total + (item.cantidad || 0), 0);
      return {
        _loteId: trace.loteFisico?.id || null,
        _date: trace.fechaActualizacion || trace.fechaIngreso || null,
        'Lote trazable': trace.codigo,
        Invernadero: trace.loteFisico?.codigo || 'Sin invernadero registrado',
        Cama: trace.camaInicial?.codigo || 'Sin cama registrada',
        Variedad: trace.variedad,
        Procedencia: trace.procedencia,
        Estado: trace.estado,
        Siembras: traceSiembras.length,
        'Plantas sembradas': planted,
        Uniformizaciones: traceUniformizaciones.length,
        Formalizaciones: traceFormalizaciones.length,
        'Clasificación validada': validated,
        Despachos: traceDespachos.length,
        'Plantas despachadas': dispatched,
        'Mermas de clasificación': losses,
        'Saldo disponible': Math.max(0, validated - dispatched - losses)
      };
    });

    const produccionRows = trazabilidadRows.map((row) => ({
      _loteId: row._loteId,
      _date: row._date,
      'Lote trazable': row['Lote trazable'],
      Invernadero: row.Invernadero,
      Cama: row.Cama,
      Variedad: row.Variedad,
      Estado: row.Estado,
      'Plantas sembradas': row['Plantas sembradas'],
      'Plantas uniformizadas': traceableUniformizaciones
        .filter((item) => item.loteTrazable?.codigo === row['Lote trazable'])
        .reduce((total, item) => total + (item.cantidadUniformizada || 0), 0),
      'Plantas formalizadas': traceableFormalizaciones
        .filter((item) => item.loteTrazable?.codigo === row['Lote trazable'])
        .reduce((total, item) => total + (item.cantidadPlantas || 0), 0),
      'Clasificación validada': row['Clasificación validada'],
      'Plantas despachadas': row['Plantas despachadas'],
      'Mermas de clasificación': row['Mermas de clasificación'],
      'Saldo disponible': row['Saldo disponible']
    }));

    const clasificacionRows = traceableClasificaciones.map((item) => ({
      _loteId: item.lote?.id || null,
      _date: item.fechaClasificacion,
      'Lote trazable': item.loteTrazable?.codigo || 'Sin lote trazable',
      Invernadero: item.lote?.codigo || 'Sin invernadero registrado',
      Cama: item.cama?.codigo || 'Sin cama registrada',
      Fecha: dateShort(item.fechaClasificacion),
      'Estado de planta': item.estadoPlanta || 'No definido',
      Tamaño: item.tamano || 'No definido',
      Condición: item.condicion || 'No definida',
      Cantidad: item.cantidad || 0,
      Estado: item.estado || 'Sin estado registrado',
      Responsable: item.usuarioRegistro?.nombreCompleto || 'Sin responsable registrado'
    }));

    const despachoRows = traceableDespachos.map((item) => ({
      _loteId: item.lote?.id || null,
      _date: item.fechaDespacho,
      'Lote trazable': item.loteTrazable?.codigo || 'Sin lote trazable',
      Invernadero: item.lote?.codigo || 'Sin invernadero registrado',
      'Clasificación vinculada': item.clasificacion?.codigo || `Clasificación ${item.clasificacion?.id || ''}`.trim(),
      Fecha: dateShort(item.fechaDespacho),
      Modalidad: item.modalidad || 'No definida',
      'Cantidad despachada': item.cantidadDespachada || 0,
      Destino: item.destino || 'Sin destino registrado',
      'Guía de remisión': item.guiaRemision || 'Sin guía registrada',
      'Validación de calidad': item.validacionCalidad || 'Sin validación registrada',
      Estado: item.estado || 'Sin estado registrado',
      Responsable: item.usuarioRegistro?.nombreCompleto || 'Sin responsable registrado'
    }));

    const traceByCode = new Map(lotesTrazables.map((item) => [item.codigo, item]));
    const mermaRows = mermas.map((item) => {
      const trace = traceByCode.get(item.loteTrazable?.codigo || '');
      return {
        _loteId: trace?.loteFisico?.id || null,
        _date: item.fechaMerma,
        'Lote trazable': item.loteTrazable?.codigo || 'Sin lote trazable',
        Invernadero: trace?.loteFisico?.codigo || 'Sin invernadero registrado',
        Etapa: item.etapaOrigen,
        Motivo: item.motivo,
        Cantidad: item.cantidad,
        Fecha: dateShort(item.fechaMerma),
        Estado: item.estado,
        Responsable: item.usuarioRegistro?.nombreCompleto || 'Sin responsable registrado',
        Observación: item.observacion || 'Sin observación'
      };
    });

    const auditoriaRows = auditoria.map((item) => ({
      _loteId: null,
      _date: item.fechaEvento,
      Fecha: dateShort(item.fechaEvento),
      Usuario: item.usuario?.nombreCompleto || item.usuario?.username || 'Sistema',
      Rol: item.rolNombre || 'Sin rol',
      Módulo: item.modulo,
      Acción: item.accion,
      Referencia: item.referencia || 'Sin referencia',
      Descripción: item.descripcion,
      Motivo: item.motivo || 'No aplica'
    }));

    return {
      trazabilidad: {
        key: 'trazabilidad', label: 'Informe técnico de trazabilidad', description: 'Consolidado de lotes trazables y saldos operativos verificados.', filename: 'blueberrytrace-trazabilidad', icon: BarChart3,
        headers: ['Lote trazable', 'Invernadero', 'Cama', 'Variedad', 'Procedencia', 'Estado', 'Siembras', 'Plantas sembradas', 'Uniformizaciones', 'Formalizaciones', 'Clasificación validada', 'Despachos', 'Plantas despachadas', 'Mermas de clasificación', 'Saldo disponible'], rows: trazabilidadRows
      },
      produccion: {
        key: 'produccion', label: 'Informe técnico de producción', description: 'Avance de plantas calculado exclusivamente con movimientos trazables vigentes.', filename: 'blueberrytrace-produccion', icon: Leaf,
        headers: ['Lote trazable', 'Invernadero', 'Cama', 'Variedad', 'Estado', 'Plantas sembradas', 'Plantas uniformizadas', 'Plantas formalizadas', 'Clasificación validada', 'Plantas despachadas', 'Mermas de clasificación', 'Saldo disponible'], rows: produccionRows
      },
      clasificacion: {
        key: 'clasificacion', label: 'Informe técnico de clasificación', description: 'Detalle de condición, tamaño, cantidad y estado de calidad por lote trazable.', filename: 'blueberrytrace-clasificacion', icon: Tags,
        headers: ['Lote trazable', 'Invernadero', 'Cama', 'Fecha', 'Estado de planta', 'Tamaño', 'Condición', 'Cantidad', 'Estado', 'Responsable'], rows: clasificacionRows
      },
      despachos: {
        key: 'despachos', label: 'Informe técnico de despachos', description: 'Salidas vinculadas a una clasificación validada y a su lote trazable.', filename: 'blueberrytrace-despachos', icon: Truck,
        headers: ['Lote trazable', 'Invernadero', 'Clasificación vinculada', 'Fecha', 'Modalidad', 'Cantidad despachada', 'Destino', 'Guía de remisión', 'Validación de calidad', 'Estado', 'Responsable'], rows: despachoRows
      },
      mermas: {
        key: 'mermas', label: 'Informe técnico de mermas y mortalidad', description: 'Pérdidas y ajustes registrados por lote trazable y etapa de origen.', filename: 'blueberrytrace-mermas', icon: Leaf,
        headers: ['Lote trazable', 'Invernadero', 'Etapa', 'Motivo', 'Cantidad', 'Fecha', 'Estado', 'Responsable', 'Observación'], rows: mermaRows
      },
      auditoria: {
        key: 'auditoria', label: 'Informe técnico de auditoría', description: 'Eventos administrativos y operativos disponibles para perfiles autorizados.', filename: 'blueberrytrace-auditoria', icon: FileText,
        headers: ['Fecha', 'Usuario', 'Rol', 'Módulo', 'Acción', 'Referencia', 'Descripción', 'Motivo'], rows: auditoriaRows
      }
    };
  }, [auditoria, confirmedDespachos, lotesTrazables, mermas, traceableClasificaciones, traceableDespachos, traceableFormalizaciones, traceableSiembras, traceableUniformizaciones]);

  const currentReport = reportDefinitions[reportType];
  const filteredRows = useMemo(() => currentReport.rows.filter((item) =>
    (selectedLote === allLots || String(item._loteId || '') === selectedLote)
    && inRange(item._date, dateFrom, dateTo)
  ), [currentReport.rows, dateFrom, dateTo, selectedLote]);

  useEffect(() => {
    setPreviewed(false);
    setGeneratedAt(null);
  }, [reportType, selectedLote, dateFrom, dateTo]);

  const totals = useMemo(() => currentReport.headers.map((header, index) => {
    if (index === 0) return 'Totales';
    const numbers = filteredRows.map((row) => row[header]).filter((value): value is number => typeof value === 'number');
    return numbers.length > 0 ? numbers.reduce((sum, value) => sum + value, 0) : '';
  }), [currentReport.headers, filteredRows]);

  const metrics = useMemo(() => {
    const sum = (header: string) => filteredRows.reduce((total, row) => total + safeNumber(row[header]), 0);
    const countsByState = filteredRows.reduce<Record<string, number>>((accumulator, row) => {
      const state = String(row.Estado || '').trim();
      if (state) accumulator[state] = (accumulator[state] || 0) + 1;
      return accumulator;
    }, {});
    const primaryQuantity = sum('Plantas sembradas') || sum('Cantidad') || sum('Cantidad despachada');
    return {
      list: [
        { label: 'Registros detallados', value: filteredRows.length },
        { label: 'Cantidad principal registrada', value: primaryQuantity },
        { label: 'Plantas despachadas', value: sum('Plantas despachadas') || sum('Cantidad despachada') },
        { label: 'Procesos operativos', value: sum('Uniformizaciones') + sum('Formalizaciones') }
      ],
      states: countsByState
    };
  }, [filteredRows]);

  const observations = useMemo(() => {
    if (filteredRows.length === 0) return ['Sin información disponible para el período y filtros seleccionados.'];
    const statusSummary = Object.entries(metrics.states).map(([state, count]) => `${state}: ${count}`);
    return [
      `${filteredRows.length} registro(s) fueron incluidos en el período evaluado.`,
      statusSummary.length > 0 ? `Estados registrados en el detalle: ${statusSummary.join(', ')}.` : 'El detalle no contiene un campo de estado aplicable.',
      'Las métricas y totales se calculan únicamente con la información disponible en el sistema al momento de generar el informe.'
    ];
  }, [filteredRows.length, metrics.states]);

  const technicalReport = useMemo<TechnicalReportData>(() => ({
    appName: 'BlueberryTrace · Vivero Los Viñedos',
    title: currentReport.label,
    generatedAt: generatedAt || new Date().toLocaleString('es-PE'),
    generatedBy: user?.nombreCompleto || user?.username || 'Usuario autenticado',
    period: periodLabel(dateFrom, dateTo),
    filters: [
      `Lote: ${selectedLote === allLots ? 'Todos los lotes' : lotOptions.find((item) => String(item.id) === selectedLote)?.codigo || 'No identificado'}`,
      `Tipo: ${currentReport.label}`
    ],
    status: filteredRows.length > 0 ? 'Información disponible' : 'Sin información disponible',
    metrics: metrics.list,
    headers: currentReport.headers,
    rows: filteredRows.map((row) => currentReport.headers.map((header) => row[header])),
    totals,
    observations,
    confidentiality: 'Documento de uso interno y confidencial. La información corresponde a registros operativos disponibles en BlueberryTrace.',
    logoUrl: vlvLogo
  }), [currentReport, dateFrom, dateTo, filteredRows, generatedAt, lotOptions, metrics.list, observations, selectedLote, totals, user]);

  const suffix = fileSuffix(reportType, selectedLote, lotOptions);
  const previewRows = filteredRows.slice(0, 10);
  const ReportIcon = currentReport.icon;

  function showPreview() {
    setGeneratedAt(new Date().toLocaleString('es-PE'));
    setPreviewed(true);
  }

  useEffect(() => {
    if (!previewed) return;
    const frame = window.requestAnimationFrame(() => {
      const preview = previewRef.current;
      if (!preview) return;
      const targetTop = Math.max(0, preview.getBoundingClientRect().top + window.scrollY - 92);
      window.scrollTo({ top: targetTop, left: 0, behavior: 'smooth' });
    });
    return () => window.cancelAnimationFrame(frame);
  }, [previewed, generatedAt]);

  async function exportPdf() {
    await downloadTechnicalPdf(`${currentReport.filename}-${suffix}.pdf`, technicalReport);
  }

  async function exportXlsx() {
    await downloadTechnicalXlsx(`${currentReport.filename}-${suffix}.xlsx`, technicalReport);
  }

  function exportCsv() {
    downloadTechnicalCsv(`${currentReport.filename}-${suffix}.csv`, technicalReport.headers, technicalReport.rows);
  }

  return (
    <main className="content-grid report-screen report-screen--apf3">
      <ModuleHeader
        eyebrow="Análisis operativo"
        title="Reportes técnicos"
        description="Genera informes operativos con filtros, métricas calculadas, vista previa y exportación real a PDF y XLSX."
        icon={<BarChart3 size={21} />}
        tone="blue"
      />

      <section className="panel-card report-parameter-card report-parameter-card--refined">
        <div className="panel-card__header">
          <div><h2>Parámetros del informe</h2><p>Define el alcance antes de revisar o descargar el reporte técnico.</p></div>
          <span className="panel-card__count">{filteredRows.length} registros</span>
        </div>
        <div className="report-parameters-grid report-parameters-grid--compact">
          <label>Tipo de informe<select value={reportType} onChange={(event) => setReportType(event.target.value as ReportType)}>{reportOptions.filter((type) => type.key !== 'auditoria' || canReadAuditoria).map((type) => <option key={type.key} value={type.key}>{type.label}</option>)}</select></label>
          <label>Lote<select value={selectedLote} onChange={(event) => setSelectedLote(event.target.value)}><option value={allLots}>Todos los lotes</option>{lotOptions.map((lote) => <option key={lote.id} value={lote.id}>{lote.codigo}</option>)}</select></label>
          <label>Desde<input type="date" value={dateFrom} max={dateTo || undefined} onChange={(event) => setDateFrom(event.target.value)} /></label>
          <label>Hasta<input type="date" value={dateTo} min={dateFrom || undefined} onChange={(event) => setDateTo(event.target.value)} /></label>
        </div>
        <div className="button-group">
          <button type="button" className="action-button" onClick={showPreview}><Eye size={15} /> Ver vista previa</button>
          <button type="button" className="ghost-button" disabled={!previewed} onClick={exportXlsx}><FileSpreadsheet size={15} /> Exportar XLSX</button>
          <button type="button" className="ghost-button" disabled={!previewed} onClick={exportPdf}><FileText size={15} /> Exportar PDF</button>
          <button type="button" className="ghost-button" disabled={!previewed} onClick={exportCsv}><Download size={15} /> Exportar CSV</button>
        </div>
      </section>

      <section className="report-card-grid report-card-grid--real">
        {metrics.list.slice(0, 3).map((metric, index) => <article className="report-card" key={metric.label}><span className="report-card__icon">{index === 0 ? <BarChart3 size={18} /> : index === 1 ? <Leaf size={18} /> : <Truck size={18} />}</span><h3>{metric.label}</h3><p>Valor calculado con los registros actualmente filtrados.</p><div className="report-card__footer"><span>{numberCompact(typeof metric.value === 'number' ? metric.value : 0)}</span></div></article>)}
      </section>

      <section ref={previewRef} className="panel-card technical-report-preview">
        <div className="technical-report-letterhead">
          <img src={vlvLogo} alt="Vivero Los Viñedos" />
          <div><strong>BlueberryTrace · Vivero Los Viñedos</strong><h2>{currentReport.label}</h2><p>Informe técnico operativo para control interno y trazabilidad.</p></div>
        </div>
        {previewed ? (
          <>
            <div className="technical-report-meta"><span><strong>Generado:</strong> {technicalReport.generatedAt}</span><span><strong>Usuario:</strong> {technicalReport.generatedBy}</span><span><strong>Período:</strong> {technicalReport.period}</span><span><strong>Estado:</strong> {technicalReport.status}</span></div>
            <p className="technical-report-filters"><strong>Filtros aplicados:</strong> {technicalReport.filters.join(' · ')}</p>
            <div className="technical-report-metrics">{technicalReport.metrics.map((metric) => <div key={metric.label}><span>{metric.label}</span><strong>{String(metric.value)}</strong></div>)}</div>
            {previewRows.length > 0 ? <div className="data-table-wrap report-preview-table-wrap"><table className="data-table report-preview-table"><thead><tr>{currentReport.headers.map((header) => <th key={header}>{header}</th>)}</tr></thead><tbody>{previewRows.map((row, index) => <tr key={`${reportType}-${index}`}>{currentReport.headers.map((header) => <td key={header}>{String(row[header] ?? '')}</td>)}</tr>)}</tbody><tfoot><tr>{totals.map((value, index) => <td key={index}>{String(value)}</td>)}</tr></tfoot></table></div> : <EmptyState compact title="Sin información disponible" description="El informe conservará el estado sin información para los filtros seleccionados." />}
            <div className="technical-report-observations"><h3>Observaciones</h3><ul>{technicalReport.observations.map((item) => <li key={item}>{item}</li>)}</ul></div>
            <p className="technical-report-confidentiality">{technicalReport.confidentiality}</p>
          </>
        ) : <EmptyState compact title="Vista previa pendiente" description="Selecciona “Ver vista previa” para fijar la fecha de generación y habilitar las descargas." />}
      </section>
    </main>
  );
}
