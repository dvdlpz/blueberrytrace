import { useEffect, useMemo, useState } from 'react';
import { CalendarDays, ChevronLeft, ChevronRight, ClipboardList, Eye, FilterX, Search, ShieldCheck } from 'lucide-react';
import { DetailDrawer } from '../components/DetailDrawer';
import { EmptyState } from '../components/EmptyState';
import { InfoGrid } from '../components/InfoGrid';
import { ModuleHeader } from '../components/ModuleHeader';
import { StatusBadge } from '../components/StatusBadge';
import { blueberryApi } from '../lib/api';
import { dateShort } from '../lib/format';
import type { AuditResponse, PageResponse } from '../types/api';

const PAGE_SIZE = 25;

function labelForModule(value: string) {
  return value.replace(/_/g, ' ').replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export function AuditoriaPage() {
  const [pageData, setPageData] = useState<PageResponse<AuditResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState('');
  const [moduleFilter, setModuleFilter] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<AuditResponse | null>(null);

  async function reload() {
    try {
      setLoading(true);
      setError(null);
      const response = await blueberryApi.auditoria({ page, size: PAGE_SIZE, modulo: moduleFilter || undefined, referencia: query || undefined, desde: from || undefined, hasta: to || undefined });
      setPageData(response);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'No se pudo cargar la auditoría.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { void reload(); }, [page, moduleFilter, from, to]);
  const items = pageData?.content || [];
  const modules = useMemo(() => Array.from(new Set(items.map((item) => item.modulo).filter(Boolean))).sort(), [items]);
  const filtered = useMemo(() => {
    const term = query.trim().toLowerCase();
    return items.filter((item) => !term || [item.usuario?.nombreCompleto, item.usuario?.username, item.rolNombre, item.modulo, item.accion, item.entidadTipo, item.referencia, item.descripcion, item.motivo]
      .some((value) => String(value || '').toLowerCase().includes(term)));
  }, [items, query]);
  const hasFilters = Boolean(query.trim() || moduleFilter || from || to);
  const totalPages = pageData?.totalPages || 0;

  function applySearch() { setPage(0); void reload(); }
  function clearFilters() {
    setQuery('');
    setModuleFilter('');
    setFrom('');
    setTo('');
    setPage(0);
  }

  return <main className="content-grid">
    <ModuleHeader eyebrow="Control y cumplimiento" title="Auditoría operativa" description="Consulta el historial de cambios, sesiones, perfiles y operaciones relevantes." icon={<ClipboardList size={21}/>} tone="purple"/>
    <section className="summary-strip summary-strip--three">
      <article className="summary-pill summary-pill--purple"><span className="summary-pill__icon"><ClipboardList size={18}/></span><strong>{pageData?.totalElements || 0}</strong><span>Eventos registrados</span><small>según filtros aplicados</small></article>
      <article className="summary-pill summary-pill--blue"><span className="summary-pill__icon"><ShieldCheck size={18}/></span><strong>{modules.length}</strong><span>Áreas visibles</span><small>en la página actual</small></article>
      <article className="summary-pill summary-pill--green"><span className="summary-pill__icon"><Eye size={18}/></span><strong>{items.filter(item => /LOGIN|LOGOUT/i.test(item.accion)).length}</strong><span>Sesiones visibles</span><small>en la página actual</small></article>
    </section>
    <section className="panel-card panel-card--interactive">
      <div className="audit-filter-panel">
        <div className="audit-filter-panel__heading"><div><h2>Filtrar historial</h2><p>Combina texto, área y período para ubicar un evento específico.</p></div>{hasFilters ? <button type="button" className="audit-clear-button" onClick={clearFilters}><FilterX size={15} /> Limpiar filtros</button> : null}</div>
        <div className="audit-filter-toolbar">
          <label className="filter-toolbar__search audit-search-field"><span className="sr-only">Buscar en auditoría</span><Search size={16}/><input value={query} onChange={event => setQuery(event.target.value)} onKeyDown={event => { if (event.key === 'Enter') applySearch(); }} placeholder="Buscar por referencia, persona o detalle"/></label>
          <label className="audit-select-field"><span>Área</span><select value={moduleFilter} onChange={event => { setModuleFilter(event.target.value); setPage(0); }}><option value="">Todas las áreas</option>{modules.map(item => <option key={item} value={item}>{labelForModule(item)}</option>)}</select></label>
          <label className="audit-date-field"><span><CalendarDays size={14}/> Desde</span><input type="date" value={from} max={to || undefined} onChange={event => { setFrom(event.target.value); setPage(0); }}/></label>
          <label className="audit-date-field"><span><CalendarDays size={14}/> Hasta</span><input type="date" value={to} min={from || undefined} onChange={event => { setTo(event.target.value); setPage(0); }}/></label>
          <button type="button" className="action-button audit-filter-panel__submit" onClick={applySearch}><Search size={15}/> Aplicar filtros</button>
        </div>
      </div>
      {error ? <EmptyState icon={<ClipboardList size={28}/>} title="No se pudo cargar la auditoría" description={error} action={<button type="button" className="action-button" onClick={() => void reload()}>Reintentar</button>}/> : null}
      {!error && !loading && filtered.length === 0 ? <EmptyState icon={<ClipboardList size={28}/>} title="Sin eventos para los filtros" description={hasFilters ? 'Ajusta el período o los criterios de búsqueda para ampliar el resultado.' : 'Los cambios relevantes de usuarios, perfiles y operaciones aparecerán aquí.'} action={hasFilters ? <button type="button" className="ghost-button" onClick={clearFilters}>Limpiar filtros</button> : undefined}/> : null}
      {filtered.length > 0 ? <div className="data-table-wrap"><table className="data-table audit-data-table"><thead><tr><th>Fecha</th><th>Persona</th><th>Área</th><th>Acción</th><th>Referencia</th><th aria-label="Acciones"/></tr></thead><tbody>{filtered.map(item => <tr key={item.id}><td><span className="audit-date-value">{dateShort(item.fechaEvento)}</span></td><td><div className="audit-person"><strong>{item.usuario?.nombreCompleto || 'Sistema'}</strong><small className="table-subtext">{item.rolNombre?.replace(/_/g, ' ') || 'Sin rol asignado'}</small></div></td><td><StatusBadge value={labelForModule(item.modulo)}/></td><td>{item.accion.replace(/_/g, ' ')}</td><td>{item.referencia || item.entidadTipo || 'Sin referencia'}</td><td><button type="button" className="icon-action" title="Ver detalle" onClick={() => setSelected(item)}><Eye size={15}/></button></td></tr>)}</tbody></table></div> : null}
      <footer className="table-footer-note audit-table-footer"><span>Página {totalPages === 0 ? 0 : page + 1} de {totalPages}. Se muestran hasta {PAGE_SIZE} eventos por página.</span><span className="button-group"><button type="button" className="icon-action" title="Página anterior" disabled={page <= 0 || loading} onClick={() => setPage(current => Math.max(0, current - 1))}><ChevronLeft size={15}/></button><button type="button" className="icon-action" title="Página siguiente" disabled={loading || page + 1 >= totalPages} onClick={() => setPage(current => current + 1)}><ChevronRight size={15}/></button></span></footer>
    </section>
    <DetailDrawer open={Boolean(selected)} title={selected?.referencia || selected?.accion || 'Evento de auditoría'} subtitle={selected?.modulo ? labelForModule(selected.modulo) : ''} onClose={() => setSelected(null)}>
      {selected ? <><InfoGrid items={[{label:'Fecha y hora',value:dateShort(selected.fechaEvento),tone:'blue'},{label:'Persona',value:selected.usuario?.nombreCompleto || 'Sistema',tone:'green'},{label:'Rol',value:selected.rolNombre?.replace(/_/g, ' ') || 'Sin rol asignado'},{label:'Acción',value:selected.accion.replace(/_/g, ' '),tone:'purple'},{label:'Registro',value:selected.entidadTipo || 'Sin registro asociado'},{label:'Referencia',value:selected.referencia || 'Sin referencia'}]}/><section className="drawer-section"><h3>Descripción</h3><p>{selected.descripcion}</p>{selected.motivo ? <p><strong>Motivo:</strong> {selected.motivo}</p> : null}</section><section className="drawer-section"><h3>Datos de origen</h3><p><strong>Dirección de acceso:</strong> {selected.ipOrigen || 'No disponible'}<br/><strong>Aplicación utilizada:</strong> {selected.agenteUsuario || 'No disponible'}</p></section></> : null}
    </DetailDrawer>
  </main>;
}
