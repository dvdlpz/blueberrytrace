import { useEffect, useMemo, useState } from 'react';
import { AlertTriangle, Loader2, RefreshCcw } from 'lucide-react';
import { CommandPalette, type CommandSearchItem } from './components/CommandPalette';
import { ProfileSettingsModal } from './components/ProfileSettingsModal';
import { Sidebar } from './components/Sidebar';
import { ToastStack, type ToastItem, type ToastTone } from './components/ToastStack';
import { Topbar, type TopbarNotification } from './components/Topbar';
import { useAppRoute } from './hooks/useAppRoute';
import { routeByKey } from './lib/routes';
import { BLUEBERRY_TOAST_EVENT, type BlueberryToastDetail } from './lib/uiEvents';
import { ApiError, blueberryApi } from './lib/api';
import { CamasPage } from './pages/CamasPage';
import { ClasificacionPage } from './pages/ClasificacionPage';
import { DashboardPage } from './pages/DashboardPage';
import { DespachoPage } from './pages/DespachoPage';
import { LoginPage } from './pages/LoginPage';
import { LotesPage } from './pages/LotesPage';
import { ProcesosPage } from './pages/ProcesosPage';
import { ReportesPage } from './pages/ReportesPage';
import { TrazabilidadPage } from './pages/TrazabilidadPage';
import { SiembrasPage } from './pages/SiembrasPage';
import { UsuariosPage } from './pages/UsuariosPage';
import { RolesPage } from './pages/RolesPage';
import { LotesTrazablesPage } from './pages/LotesTrazablesPage';
import { MermasPage } from './pages/MermasPage';
import { AuditoriaPage } from './pages/AuditoriaPage';
import { JabasPage } from './pages/JabasPage';
import { RecuperacionesPage } from './pages/RecuperacionesPage';
import { PedidosPage } from './pages/PedidosPage';
import { EmpaquesPage } from './pages/EmpaquesPage';
import { RiegosPage } from './pages/RiegosPage';
import type {
  AuthenticatedUserResponse,
  CamaResponse,
  CatalogResponse,
  ClasificacionResponse,
  DashboardApiResponse,
  DespachoResponse,
  CargaDespachoResponse,
  FrontendBootstrapResponse,
  LoteResponse,
  LoteTrazableResponse,
  RoleDetailResponse,
  ProcesoOperativoResponse,
  SiembraResponse,
  UserReferenceResponse,
  OperationReadinessResponse,
  ListResponse,
  JabaResponse,
  RecuperacionRiegoResponse,
  PedidoResponse,
  EmpaqueResponse,
  RiegoProgramadoResponse
} from './types/api';


function sortByMostRecent<T>(items: T[], dateOf: (item: T) => string | null | undefined) {
  return [...items].sort((left, right) => {
    const leftDate = dateOf(left);
    const rightDate = dateOf(right);
    return new Date(rightDate || 0).getTime() - new Date(leftDate || 0).getTime();
  });
}

function buildNotifications(
  lotes: LoteResponse[],
  camas: CamaResponse[],
  clasificaciones: ClasificacionResponse[],
  despachos: DespachoResponse[]
): TopbarNotification[] {
  const notifications: TopbarNotification[] = [];

  const pendingClassifications = clasificaciones.filter((item) => Boolean(item.loteTrazable?.id) && /PENDIENTE|OBSERVADA/i.test(item.estado || ''));
  if (pendingClassifications.length > 0) {
    const first = sortByMostRecent(pendingClassifications, (item) => item.fechaActualizacion || item.fechaClasificacion)[0];
    notifications.push({
      id: `clasificaciones-${pendingClassifications.length}`,
      tone: 'warning',
      title: `${pendingClassifications.length} clasificaciones requieren revisión`,
      description: first?.lote?.codigo ? `Último registro asociado al lote ${first.lote.codigo}.` : 'Hay registros pendientes de validación de calidad.',
      createdAt: first?.fechaActualizacion || first?.fechaClasificacion,
      moduleKey: 'clasificacion'
    });
  }

  const observedDispatches = despachos.filter((item) => Boolean(item.loteTrazable?.id && item.clasificacion?.id) && /OBSERVADO|ANULADO/i.test(item.estado || item.validacionCalidad || ''));
  if (observedDispatches.length > 0) {
    const first = sortByMostRecent(observedDispatches, (item) => item.fechaActualizacion || item.fechaDespacho)[0];
    notifications.push({
      id: `despachos-${observedDispatches.length}`,
      tone: 'danger',
      title: `${observedDispatches.length} despachos con observación`,
      description: first?.destino ? `Último destino observado: ${first.destino}.` : 'Revisa el módulo de despacho.',
      createdAt: first?.fechaActualizacion || first?.fechaDespacho,
      moduleKey: 'despacho'
    });
  }

  const inactiveBeds = camas.filter((item) => (item.estado || '').toUpperCase() !== 'ACTIVA');
  if (inactiveBeds.length > 0) {
    notifications.push({
      id: `camas-${inactiveBeds.length}`,
      tone: 'info',
      title: `${inactiveBeds.length} camas no activas`,
      description: 'Revisa disponibilidad y mantenimiento de camas productivas.',
      createdAt: sortByMostRecent(inactiveBeds, (item) => item.fechaActualizacion || item.fechaCreacion)[0]?.fechaActualizacion,
      moduleKey: 'camas'
    });
  }

  const latestLot = sortByMostRecent(lotes, (item) => item.fechaActualizacion || item.fechaCreacion || item.fechaRegistro)[0];
  if (latestLot) {
    notifications.push({
      id: `lote-${latestLot.id}`,
      tone: 'success',
      title: `Lote ${latestLot.codigo} disponible`,
      description: latestLot.estado ? `Estado actual: ${latestLot.estado}.` : 'Registro disponible para el seguimiento operativo.',
      createdAt: latestLot.fechaActualizacion || latestLot.fechaCreacion || latestLot.fechaRegistro,
      moduleKey: 'lotes'
    });
  }

  return notifications.slice(0, 6);
}

function requiredModuleForRoute(key: string) {
  if (key === 'uniformizaciones' || key === 'formalizaciones') return 'procesos';
  if (key === 'lotes-trazables') return 'lotes_trazables';
  return key;
}

function emptyList<T>(): ListResponse<T> {
  return { total: 0, items: [] };
}

function buildSearchItems(
  lotes: LoteResponse[],
  camas: CamaResponse[],
  siembras: SiembraResponse[],
  procesos: ProcesoOperativoResponse | null,
  clasificaciones: ClasificacionResponse[],
  despachos: DespachoResponse[],
  usuarios: UserReferenceResponse[]
): CommandSearchItem[] {
  return [
    ...lotes.map((item) => ({
      id: `lote-${item.id}`,
      label: `Lote ${item.codigo}`,
      description: [item.descripcion, item.variedad, item.estado].filter(Boolean).join(' · '),
      moduleKey: 'lotes',
      type: 'Lote'
    })),
    ...camas.map((item) => ({
      id: `cama-${item.id}`,
      label: `Cama ${item.codigo}`,
      description: [item.lote?.codigo, item.descripcion, item.estado].filter(Boolean).join(' · '),
      moduleKey: 'camas',
      type: 'Cama'
    })),
    ...siembras.map((item) => ({
      id: `siembra-${item.id}`,
      label: `Siembra #${item.id}`,
      description: [item.lote?.codigo, item.cama?.codigo, item.estado].filter(Boolean).join(' · '),
      moduleKey: 'siembra',
      type: 'Siembra'
    })),
    ...(procesos?.uniformizaciones.items || []).map((item) => ({
      id: `uniformizacion-${item.id}`,
      label: `Uniformización #${item.id}`,
      description: [item.lote?.codigo, item.cama?.codigo, item.estado].filter(Boolean).join(' · '),
      moduleKey: 'uniformizaciones',
      type: 'Uniformización'
    })),
    ...(procesos?.formalizaciones.items || []).map((item) => ({
      id: `formalizacion-${item.id}`,
      label: `Formalización #${item.id}`,
      description: [item.lote?.codigo, item.cama?.codigo, item.estado].filter(Boolean).join(' · '),
      moduleKey: 'formalizaciones',
      type: 'Formalización'
    })),
    ...clasificaciones.map((item) => ({
      id: `clasificacion-${item.id}`,
      label: `Clasificación #${item.id}`,
      description: [item.lote?.codigo, item.estadoPlanta, item.estado].filter(Boolean).join(' · '),
      moduleKey: 'clasificacion',
      type: 'Calidad'
    })),
    ...despachos.map((item) => ({
      id: `despacho-${item.id}`,
      label: `Despacho #${item.id}`,
      description: [item.lote?.codigo, item.destino, item.estado].filter(Boolean).join(' · '),
      moduleKey: 'despacho',
      type: 'Despacho'
    })),
    ...usuarios.map((item) => ({
      id: `usuario-${item.id}`,
      label: item.nombreCompleto || item.username,
      description: [item.email, item.rol, item.activo ? 'Activo' : 'Inactivo'].filter(Boolean).join(' · '),
      moduleKey: 'usuarios',
      type: 'Usuario'
    }))
  ];
}

export default function App() {
  const [bootstrap, setBootstrap] = useState<FrontendBootstrapResponse | null>(null);
  const [user, setUser] = useState<AuthenticatedUserResponse | null>(null);
  const [dashboard, setDashboard] = useState<DashboardApiResponse | null>(null);
  const [operationReadiness, setOperationReadiness] = useState<OperationReadinessResponse | null>(null);
  const [catalogs, setCatalogs] = useState<CatalogResponse | null>(null);
  const [lotes, setLotes] = useState<LoteResponse[]>([]);
  const [camas, setCamas] = useState<CamaResponse[]>([]);
  const [jabas, setJabas] = useState<JabaResponse[]>([]);
  const [recuperaciones, setRecuperaciones] = useState<RecuperacionRiegoResponse[]>([]);
  const [pedidos, setPedidos] = useState<PedidoResponse[]>([]);
  const [empaques, setEmpaques] = useState<EmpaqueResponse[]>([]);
  const [riegos, setRiegos] = useState<RiegoProgramadoResponse[]>([]);
  const [siembras, setSiembras] = useState<SiembraResponse[]>([]);
  const [procesos, setProcesos] = useState<ProcesoOperativoResponse | null>(null);
  const [clasificaciones, setClasificaciones] = useState<ClasificacionResponse[]>([]);
  const [despachos, setDespachos] = useState<DespachoResponse[]>([]);
  const [cargasDespacho, setCargasDespacho] = useState<CargaDespachoResponse[]>([]);
  const [lotesTrazables, setLotesTrazables] = useState<LoteTrazableResponse[]>([]);
  const [usuarios, setUsuarios] = useState<UserReferenceResponse[]>([]);
  const [roles, setRoles] = useState<RoleDetailResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [authRequired, setAuthRequired] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [commandOpen, setCommandOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [toasts, setToasts] = useState<ToastItem[]>([]);
  const { activeKey, navigate } = useAppRoute();

  function pushToast(tone: ToastTone, title: string, description?: string) {
    const id = Date.now() + Math.floor(Math.random() * 1000);
    setToasts((current) => [...current.slice(-3), { id, tone, title, description }]);
    window.setTimeout(() => {
      setToasts((current) => current.filter((toast) => toast.id !== id));
    }, 4200);
  }

  async function load(signal?: AbortSignal) {
    const [bootstrapResponse, userResponse] = await Promise.all([
      blueberryApi.bootstrap(),
      blueberryApi.session()
    ]);

    const isAdministrator = userResponse.authorities.includes('ROLE_ADMINISTRADOR');
    const dashboardResponse = await blueberryApi.dashboard();
    const allowedModules = new Set(dashboardResponse.modules.map((module) => module.key));
    const canRead = (module: string) => allowedModules.has(module);
    const needsCatalogs = ['lotes', 'camas', 'jabas', 'lotes_trazables', 'siembra', 'riegos', 'procesos', 'recuperacion', 'clasificacion', 'pedidos', 'empaques', 'despacho', 'mermas']
      .some((module) => canRead(module));

    const [
      operationReadinessResponse,
      catalogsResponse,
      lotesResponse,
      camasResponse,
      jabasResponse,
      riegosResponse,
      recuperacionesResponse,
      pedidosResponse,
      empaquesResponse,
      siembrasResponse,
      procesosResponse,
      clasificacionesResponse,
      despachosResponse,
      cargasDespachoResponse,
      lotesTrazablesResponse,
      usuariosResponse,
      rolesResponse
    ] = await Promise.all([
      blueberryApi.operationReadiness(),
      needsCatalogs ? blueberryApi.catalogs() : Promise.resolve(null),
      canRead('lotes') ? blueberryApi.lotes() : Promise.resolve(emptyList<LoteResponse>()),
      canRead('camas') ? blueberryApi.camas() : Promise.resolve(emptyList<CamaResponse>()),
      canRead('jabas') ? blueberryApi.jabas() : Promise.resolve(emptyList<JabaResponse>()),
      canRead('riegos') ? blueberryApi.riegosProgramados() : Promise.resolve(emptyList<RiegoProgramadoResponse>()),
      canRead('recuperacion') ? blueberryApi.recuperacionesRiego() : Promise.resolve(emptyList<RecuperacionRiegoResponse>()),
      canRead('pedidos') ? blueberryApi.pedidos() : Promise.resolve(emptyList<PedidoResponse>()),
      canRead('empaques') ? blueberryApi.empaques() : Promise.resolve(emptyList<EmpaqueResponse>()),
      canRead('siembra') ? blueberryApi.siembras() : Promise.resolve(emptyList<SiembraResponse>()),
      canRead('procesos') ? blueberryApi.procesos() : Promise.resolve(null),
      canRead('clasificacion') ? blueberryApi.clasificaciones() : Promise.resolve(emptyList<ClasificacionResponse>()),
      canRead('despacho') ? blueberryApi.despachos() : Promise.resolve(emptyList<DespachoResponse>()),
      canRead('despacho') ? blueberryApi.cargasDespacho() : Promise.resolve(emptyList<CargaDespachoResponse>()),
      canRead('lotes_trazables') || canRead('trazabilidad') ? blueberryApi.lotesTrazablesActivos().catch(() => []) : Promise.resolve([] as LoteTrazableResponse[]),
      isAdministrator ? blueberryApi.usuarios() : Promise.resolve(emptyList<UserReferenceResponse>()),
      isAdministrator ? blueberryApi.roles() : Promise.resolve([] as RoleDetailResponse[])
    ]);

    if (signal?.aborted) {
      return;
    }

    setBootstrap(bootstrapResponse);
    setUser(userResponse);
    setDashboard(dashboardResponse);
    setOperationReadiness(operationReadinessResponse);
    setCatalogs(catalogsResponse);
    setLotes(lotesResponse.items);
    setCamas(camasResponse.items);
    setJabas(jabasResponse.items);
    setRiegos(riegosResponse.items);
    setRecuperaciones(recuperacionesResponse.items);
    setPedidos(pedidosResponse.items);
    setEmpaques(empaquesResponse.items);
    setSiembras(siembrasResponse.items);
    setProcesos(procesosResponse);
    setClasificaciones(clasificacionesResponse.items);
    setDespachos(despachosResponse.items);
    setCargasDespacho(cargasDespachoResponse.items);
    setLotesTrazables(lotesTrazablesResponse);
    setUsuarios(usuariosResponse.items);
    setRoles(rolesResponse);
    if (userResponse.requiereCambioPassword) {
      setProfileOpen(true);
    }
    setAuthRequired(false);
    setError(null);
  }

  useEffect(() => {
    const controller = new AbortController();

    async function boot() {
      try {
        setLoading(true);
        await load(controller.signal);
      } catch (exception) {
        if (!controller.signal.aborted) {
          if (exception instanceof ApiError && (exception.status === 401 || exception.status === 403)) {
            setAuthRequired(true);
            setError(null);
          } else {
            setError(exception instanceof Error ? exception.message : 'No se pudo cargar la información del panel.');
          }
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    boot();
    return () => controller.abort();
  }, []);

  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setSidebarOpen(false);
      }
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        setCommandOpen(true);
      }
    }

    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, []);

  useEffect(() => {
    function onToast(event: Event) {
      const detail = (event as CustomEvent<BlueberryToastDetail>).detail;
      if (detail) {
        pushToast(detail.tone, detail.title, detail.description);
      }
    }

    window.addEventListener(BLUEBERRY_TOAST_EVENT, onToast);
    return () => window.removeEventListener(BLUEBERRY_TOAST_EVENT, onToast);
  }, []);

  async function refresh() {
    try {
      setRefreshing(true);
      await load();
      pushToast('success', 'Datos sincronizados', 'La información operativa fue actualizada correctamente.');
    } catch (exception) {
      if (exception instanceof ApiError && (exception.status === 401 || exception.status === 403)) {
        setAuthRequired(true);
        setUser(null);
        setError(null);
        pushToast('warning', 'Sesión expirada', 'Inicia sesión nuevamente para continuar.');
      } else {
        const message = exception instanceof Error ? exception.message : 'No se pudo actualizar la información.';
        setError(message);
        pushToast('error', 'No se pudo sincronizar', message);
      }
    } finally {
      setRefreshing(false);
    }
  }

  async function handleAuthenticated(authenticatedUser: AuthenticatedUserResponse) {
    setUser(authenticatedUser);
    setAuthRequired(false);
    if (window.location.pathname === '/login') {
      window.history.pushState({}, '', '/dashboard');
    }
    setLoading(true);
    try {
      await load();
      pushToast('success', 'Sesión iniciada', `Bienvenido, ${authenticatedUser.nombreCompleto || authenticatedUser.username}.`);
    } finally {
      setLoading(false);
    }
  }

  async function handleLogout() {
    try {
      await blueberryApi.logout();
    } finally {
      setUser(null);
      setAuthRequired(true);
      setDashboard(null);
      setOperationReadiness(null);
      setCatalogs(null);
      setJabas([]);
      setRecuperaciones([]);
      setPedidos([]);
      setEmpaques([]);
      setDespachos([]);
      setCargasDespacho([]);
      setLotesTrazables([]);
      setRoles([]);
      window.history.pushState({}, '', '/login');
      pushToast('info', 'Sesión cerrada', 'Puedes iniciar sesión nuevamente cuando lo necesites.');
    }
  }

  const modules = useMemo(() => (dashboard?.modules || bootstrap?.modules || []).filter((module) => module.key !== 'usuarios' || user?.authorities.includes('ROLE_ADMINISTRADOR')), [bootstrap, dashboard, user]);
  const activeModule = modules.find((module) => module.key === requiredModuleForRoute(activeKey));
  const activeRoute = routeByKey(activeKey);
  const canAccessActiveRoute = modules.some((module) => module.key === requiredModuleForRoute(activeKey));
  const loteReferences = catalogs?.lotes || lotes.map((lote) => ({ id: lote.id, codigo: lote.codigo, descripcion: lote.descripcion }));
  const notifications = useMemo(() => buildNotifications(lotes, camas, clasificaciones, despachos), [lotes, camas, clasificaciones, despachos]);
  const searchItems = useMemo(() => buildSearchItems(lotes, camas, siembras, procesos, clasificaciones, despachos, usuarios), [
    lotes,
    camas,
    siembras,
    procesos,
    clasificaciones,
    despachos,
    usuarios
  ]);

  if (loading) {
    return (
      <div className="boot-screen">
        <Loader2 className="spin" size={34} />
        <strong>Cargando BlueberryTrace</strong>
        <span>Conectando con el servicio operativo</span>
      </div>
    );
  }

  if (authRequired) {
    return <LoginPage onAuthenticated={handleAuthenticated} />;
  }

  if (error) {
    return (
      <div className="boot-screen boot-screen--error">
        <AlertTriangle size={38} />
        <strong>No se pudo conectar con el servicio</strong>
        <span>{error}</span>
        <div className="boot-actions">
          <button type="button" className="action-button" onClick={refresh}><RefreshCcw size={16} /> Reintentar</button>
          <button type="button" className="ghost-button" onClick={() => setAuthRequired(true)}>Iniciar sesión</button>
        </div>
      </div>
    );
  }

  return (
    <div className={sidebarOpen ? 'app-shell app-shell--sidebar-open' : 'app-shell'}>
      <button
        type="button"
        className="mobile-sidebar-backdrop"
        aria-label="Cerrar menú de navegación"
        onClick={() => setSidebarOpen(false)}
      />
      <Sidebar
        modules={modules}
        activeKey={activeKey}
        user={user}
        onSelect={(key) => {
          navigate(key);
          setSidebarOpen(false);
        }}
        onLogout={handleLogout}
        onOpenProfile={() => {
          setProfileOpen(true);
          setSidebarOpen(false);
        }}
        onClose={() => setSidebarOpen(false)}
      />
      <section className="main-shell">
        <Topbar
          user={user}
          activeModule={activeModule?.label || activeRoute.label || 'Control de trazabilidad'}
          activeKey={activeKey}
          notifications={notifications}
          onOpenSearch={() => setCommandOpen(true)}
          onOpenSidebar={() => setSidebarOpen(true)}
          onRefresh={refresh}
          onNavigate={navigate}
          onOpenProfile={() => setProfileOpen(true)}
          onLogout={handleLogout}
          refreshing={refreshing}
        />
        <div key={activeKey} className="route-transition">
          {!canAccessActiveRoute ? (
            <section className="panel-card">
              <div className="empty-state">
                <AlertTriangle size={28} />
                <strong>Acceso restringido</strong>
                <span>Tu rol no tiene permisos para acceder a este módulo.</span>
                <div className="empty-state__action">
                  <button type="button" className="action-button" onClick={() => navigate('dashboard')}>Ir al panel operativo</button>
                </div>
              </div>
            </section>
          ) : null}
          {canAccessActiveRoute && activeKey === 'dashboard' && (
            <DashboardPage
              dashboard={dashboard}
              readiness={operationReadiness}
              lotes={lotes}
              camas={camas}
              siembras={siembras}
              procesos={procesos}
              clasificaciones={clasificaciones}
              despachos={despachos}
              lotesTrazables={lotesTrazables}
              availableModuleKeys={modules.map((module) => module.key)}
              onNavigate={navigate}
            />
          )}
          {canAccessActiveRoute && activeKey === 'lotes' && <LotesPage lotes={lotes} camas={camas} siembras={siembras} onLotesChange={setLotes} />}
          {canAccessActiveRoute && activeKey === 'camas' && <CamasPage camas={camas} lotes={loteReferences} onCamasChange={setCamas} />}
          {canAccessActiveRoute && activeKey === 'jabas' && <JabasPage jabas={jabas} camas={camas} onJabasChange={setJabas} />}
          {canAccessActiveRoute && activeKey === 'riegos' && <RiegosPage riegos={riegos} lotesTrazables={lotesTrazables} jabas={jabas} onChanged={setRiegos} />}
          {canAccessActiveRoute && activeKey === 'lotes-trazables' && <LotesTrazablesPage lotes={loteReferences} camas={camas} canManage={Boolean(user?.authorities.includes('ROLE_ADMINISTRADOR') || user?.authorities.includes('ROLE_SUPERVISOR'))} isAdministrator={Boolean(user?.authorities.includes('ROLE_ADMINISTRADOR'))} onChanged={setLotesTrazables} />}
          {canAccessActiveRoute && activeKey === 'siembra' && <SiembrasPage siembras={siembras} lotes={loteReferences} camas={camas} lotesTrazables={lotesTrazables} jabas={jabas} onSiembrasChange={setSiembras} />}
          {canAccessActiveRoute && activeKey === 'uniformizaciones' && <ProcesosPage mode="uniformizacion" procesos={procesos} lotes={loteReferences} camas={camas} siembras={siembras} lotesTrazables={lotesTrazables} jabas={jabas} onProcesosChange={setProcesos} />}
          {canAccessActiveRoute && activeKey === 'formalizaciones' && <ProcesosPage mode="formalizacion" procesos={procesos} lotes={loteReferences} camas={camas} siembras={siembras} lotesTrazables={lotesTrazables} jabas={jabas} onProcesosChange={setProcesos} />}
          {canAccessActiveRoute && activeKey === 'clasificacion' && <ClasificacionPage clasificaciones={clasificaciones} lotes={loteReferences} camas={camas} lotesTrazables={lotesTrazables} jabas={jabas} onClasificacionesChange={setClasificaciones} />}
          {canAccessActiveRoute && activeKey === 'despacho' && <DespachoPage despachos={despachos} cargas={cargasDespacho} lotes={loteReferences} lotesTrazables={lotesTrazables} clasificaciones={clasificaciones} pedidos={pedidos} empaques={empaques} validaciones={catalogs?.validacionesCalidad || []} onDespachosChange={setDespachos} onCargasChange={setCargasDespacho} />}
          {canAccessActiveRoute && activeKey === 'recuperacion' && <RecuperacionesPage recuperaciones={recuperaciones} lotesTrazables={lotesTrazables} jabas={jabas} onChanged={setRecuperaciones} />}
          {canAccessActiveRoute && activeKey === 'pedidos' && <PedidosPage pedidos={pedidos} onChanged={setPedidos} />}
          {canAccessActiveRoute && activeKey === 'empaques' && <EmpaquesPage empaques={empaques} lotesTrazables={lotesTrazables} clasificaciones={clasificaciones} pedidos={pedidos} onChanged={setEmpaques} />}
          {canAccessActiveRoute && activeKey === 'mermas' && <MermasPage lotesTrazables={lotesTrazables} />}
          {canAccessActiveRoute && activeKey === 'trazabilidad' && <TrazabilidadPage lotesTrazables={lotesTrazables} />}
          {canAccessActiveRoute && activeKey === 'reportes' && <ReportesPage lotes={lotes} camas={camas} siembras={siembras} procesos={procesos} clasificaciones={clasificaciones} despachos={despachos} lotesTrazables={lotesTrazables} user={user} availableModuleKeys={modules.map((module) => module.key)} />}
          {canAccessActiveRoute && activeKey === 'usuarios' && <UsuariosPage usuarios={usuarios} roles={roles} onUsuariosChange={setUsuarios} />}
          {canAccessActiveRoute && activeKey === 'roles' && <RolesPage onRolesChange={setRoles} />}
          {canAccessActiveRoute && activeKey === 'auditoria' && <AuditoriaPage />}
        </div>
      </section>
      <CommandPalette
        open={commandOpen}
        modules={modules}
        activeKey={activeKey}
        searchItems={searchItems}
        onClose={() => setCommandOpen(false)}
        onSelect={navigate}
        onRefresh={refresh}
      />
      <ProfileSettingsModal
        open={profileOpen}
        user={user}
        onClose={() => { if (!user?.requiereCambioPassword) setProfileOpen(false); }}
        requiresPasswordChange={Boolean(user?.requiereCambioPassword)}
        onUpdated={(updatedUser) => {
          setUser(updatedUser);
          pushToast('success', 'Perfil actualizado', 'Tus datos corporativos fueron guardados correctamente.');
        }}
        onPasswordChanged={() => {
          setProfileOpen(false);
          pushToast('success', 'Contraseña actualizada', 'Por seguridad, inicia sesión nuevamente con la nueva contraseña.');
          void handleLogout();
        }}
      />
      <ToastStack toasts={toasts} onDismiss={(id) => setToasts((current) => current.filter((toast) => toast.id !== id))} />
    </div>
  );
}
