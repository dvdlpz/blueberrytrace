import type {
  ApiResponse,
  AuthenticatedUserResponse,
  CamaFormPayload,
  CamaResponse,
  CargaDespachoFormPayload,
  CargaDespachoResponse,
  CatalogResponse,
  ClasificacionFormPayload,
  ClasificacionResponse,
  CsrfResponse,
  DashboardApiResponse,
  DespachoFormPayload,
  DespachoResponse,
  FormalizacionFormPayload,
  FrontendBootstrapResponse,
  ListResponse,
  LoginPayload,
  LoteFormPayload,
  PasswordChangePayload,
  ProfileUpdatePayload,
  LoteResponse,
  ProcesoOperativoResponse,
  SiembraFormPayload,
  SiembraResponse,
  TrazabilidadResponse,
  UniformizacionFormPayload,
  UserFormPayload,
  UserReferenceResponse,
  RoleDetailResponse,
  RoleUpdatePayload,
  LoteTrazableResponse,
  LoteTrazableFormPayload,
  LoteTrazableDetailResponse,
  MermaResponse,
  MermaFormPayload,
  LegacyNormalizationPayload,
  AuditResponse,
  PageResponse,
  OperationReadinessResponse,
  JabaResponse,
  JabaFormPayload,
  RecuperacionRiegoResponse,
  RecuperacionRiegoFormPayload,
  RecuperacionRiegoStatusPayload,
  RiegoProgramadoResponse,
  RiegoProgramadoFormPayload,
  RiegoRealizadoPayload,
  PedidoResponse,
  PedidoFormPayload,
  EmpaqueResponse,
  EmpaqueFormPayload
} from '../types/api';
import { messageForUser } from './userMessage';

const apiBase = (import.meta.env.VITE_BLUEBERRYTRACE_API_BASE || '/api/v1').replace(/\/$/, '');

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly payload?: unknown
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

function buildUrl(path: string, params?: Record<string, string | number | undefined | null>) {
  const cleanPath = path.startsWith('/') ? path : `/${path}`;
  const base = apiBase.startsWith('http') ? apiBase : window.location.origin + apiBase;
  const url = new URL(`${base}${cleanPath}`);

  Object.entries(params || {})
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .forEach(([key, value]) => url.searchParams.set(key, String(value)));

  return url.toString();
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(buildUrl(path), {
      ...init,
      credentials: 'include',
      headers: {
        Accept: 'application/json',
        'X-Requested-With': 'BlueberryTraceReact',
        ...(init?.headers || {})
      }
    });
  } catch (exception) {
    throw new ApiError(messageForUser(exception instanceof Error ? exception.message : undefined), 0);
  }

  const contentType = response.headers.get('content-type') || '';
  const payload = contentType.includes('application/json') ? await response.json() : await response.text();

  if (!response.ok) {
    const rawMessage = typeof payload === 'object' && payload && 'message' in payload
      ? String((payload as { message?: string }).message)
      : undefined;
    throw new ApiError(messageForUser(rawMessage, response.status), response.status, payload);
  }

  return payload as T;
}

async function getData<T>(path: string): Promise<T> {
  const payload = await request<ApiResponse<T>>(path);
  return payload.data;
}

function queryPath(path: string, params?: Record<string, string | number | undefined | null>) {
  const query = new URLSearchParams();
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') query.set(key, String(value));
  });
  const serialized = query.toString();
  return serialized ? `${path}?${serialized}` : path;
}

async function mutateData<T>(path: string, method: string, body?: unknown): Promise<T> {
  const csrfPayload = await request<ApiResponse<CsrfResponse>>('/auth/csrf');
  const csrf = csrfPayload.data;
  const payload = await request<ApiResponse<T>>(path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      [csrf.headerName]: csrf.token
    },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  return payload.data;
}

export const blueberryApi = {
  csrf: () => getData<CsrfResponse>('/auth/csrf'),
  login: (payload: LoginPayload) => mutateData<AuthenticatedUserResponse>('/auth/login', 'POST', payload),
  logout: () => mutateData<void>('/auth/logout', 'POST'),
  bootstrap: () => getData<FrontendBootstrapResponse>('/frontend/bootstrap'),
  session: () => getData<AuthenticatedUserResponse>('/session/me'),
  updateProfile: (payload: ProfileUpdatePayload) => mutateData<AuthenticatedUserResponse>('/session/me', 'PUT', payload),
  changePassword: (payload: PasswordChangePayload) => mutateData<void>('/session/me/password', 'PATCH', payload),
  dashboard: () => getData<DashboardApiResponse>('/dashboard/summary'),
  operationReadiness: () => getData<OperationReadinessResponse>('/operations/readiness'),
  catalogs: () => getData<CatalogResponse>('/catalogs/operations'),
  lotes: () => getData<ListResponse<LoteResponse>>('/lotes'),
  createLote: (payload: LoteFormPayload) => mutateData<ListResponse<LoteResponse>>('/lotes', 'POST', payload),
  updateLote: (id: number, payload: LoteFormPayload) => mutateData<ListResponse<LoteResponse>>(`/lotes/${id}`, 'PUT', payload),
  toggleLoteStatus: (id: number) => mutateData<ListResponse<LoteResponse>>(`/lotes/${id}/estado`, 'PATCH'),
  deleteLote: (id: number) => mutateData<ListResponse<LoteResponse>>(`/lotes/${id}`, 'DELETE'),
  camas: () => getData<ListResponse<CamaResponse>>('/camas'),
  createCama: (payload: CamaFormPayload) => mutateData<ListResponse<CamaResponse>>('/camas', 'POST', payload),
  updateCama: (id: number, payload: CamaFormPayload) => mutateData<ListResponse<CamaResponse>>(`/camas/${id}`, 'PUT', payload),
  toggleCamaStatus: (id: number) => mutateData<ListResponse<CamaResponse>>(`/camas/${id}/estado`, 'PATCH'),
  jabas: () => getData<ListResponse<JabaResponse>>('/jabas'),
  createJaba: (payload: JabaFormPayload) => mutateData<ListResponse<JabaResponse>>('/jabas', 'POST', payload),
  updateJaba: (id: number, payload: JabaFormPayload) => mutateData<ListResponse<JabaResponse>>(`/jabas/${id}`, 'PUT', payload),
  toggleJabaStatus: (id: number) => mutateData<ListResponse<JabaResponse>>(`/jabas/${id}/estado`, 'PATCH'),
  riegosProgramados: () => getData<ListResponse<RiegoProgramadoResponse>>('/riegos-programados'),
  createRiegoProgramado: (payload: RiegoProgramadoFormPayload) => mutateData<ListResponse<RiegoProgramadoResponse>>('/riegos-programados', 'POST', payload),
  completeRiegoProgramado: (id: number, payload: RiegoRealizadoPayload) => mutateData<ListResponse<RiegoProgramadoResponse>>(`/riegos-programados/${id}/realizar`, 'PATCH', payload),
  cancelRiegoProgramado: (id: number) => mutateData<ListResponse<RiegoProgramadoResponse>>(`/riegos-programados/${id}/cancelar`, 'PATCH'),
  recuperacionesRiego: () => getData<ListResponse<RecuperacionRiegoResponse>>('/recuperaciones-riego'),
  createRecuperacionRiego: (payload: RecuperacionRiegoFormPayload) => mutateData<ListResponse<RecuperacionRiegoResponse>>('/recuperaciones-riego', 'POST', payload),
  closeRecuperacionRiego: (id: number, payload: RecuperacionRiegoStatusPayload) => mutateData<ListResponse<RecuperacionRiegoResponse>>(`/recuperaciones-riego/${id}/cerrar`, 'PATCH', payload),
  pedidos: () => getData<ListResponse<PedidoResponse>>('/pedidos'),
  createPedido: (payload: PedidoFormPayload) => mutateData<ListResponse<PedidoResponse>>('/pedidos', 'POST', payload),
  updatePedido: (id: number, payload: PedidoFormPayload) => mutateData<ListResponse<PedidoResponse>>(`/pedidos/${id}`, 'PUT', payload),
  changePedidoStatus: (id: number, estado: string) => mutateData<ListResponse<PedidoResponse>>(`/pedidos/${id}/estado?estado=${encodeURIComponent(estado)}`, 'PATCH'),
  empaques: () => getData<ListResponse<EmpaqueResponse>>('/empaques'),
  createEmpaque: (payload: EmpaqueFormPayload) => mutateData<ListResponse<EmpaqueResponse>>('/empaques', 'POST', payload),
  annulEmpaque: (id: number) => mutateData<ListResponse<EmpaqueResponse>>(`/empaques/${id}/anular`, 'PATCH'),
  siembras: () => getData<ListResponse<SiembraResponse>>('/siembras'),
  createSiembra: (payload: SiembraFormPayload) => mutateData<ListResponse<SiembraResponse>>('/siembras', 'POST', payload),
  updateSiembra: (id: number, payload: SiembraFormPayload) => mutateData<ListResponse<SiembraResponse>>(`/siembras/${id}`, 'PUT', payload),
  toggleSiembraStatus: (id: number) => mutateData<ListResponse<SiembraResponse>>(`/siembras/${id}/estado`, 'PATCH'),
  deleteSiembra: (id: number) => mutateData<ListResponse<SiembraResponse>>(`/siembras/${id}`, 'DELETE'),
  procesos: () => getData<ProcesoOperativoResponse>('/procesos'),
  createUniformizacion: (payload: UniformizacionFormPayload) => mutateData<ProcesoOperativoResponse>('/procesos/uniformizaciones', 'POST', payload),
  updateUniformizacion: (id: number, payload: UniformizacionFormPayload) => mutateData<ProcesoOperativoResponse>(`/procesos/uniformizaciones/${id}`, 'PUT', payload),
  toggleUniformizacionStatus: (id: number) => mutateData<ProcesoOperativoResponse>(`/procesos/uniformizaciones/${id}/estado`, 'PATCH'),
  deleteUniformizacion: (id: number) => mutateData<ProcesoOperativoResponse>(`/procesos/uniformizaciones/${id}`, 'DELETE'),
  createFormalizacion: (payload: FormalizacionFormPayload) => mutateData<ProcesoOperativoResponse>('/procesos/formalizaciones', 'POST', payload),
  updateFormalizacion: (id: number, payload: FormalizacionFormPayload) => mutateData<ProcesoOperativoResponse>(`/procesos/formalizaciones/${id}`, 'PUT', payload),
  toggleFormalizacionStatus: (id: number) => mutateData<ProcesoOperativoResponse>(`/procesos/formalizaciones/${id}/estado`, 'PATCH'),
  deleteFormalizacion: (id: number) => mutateData<ProcesoOperativoResponse>(`/procesos/formalizaciones/${id}`, 'DELETE'),
  clasificaciones: () => getData<ListResponse<ClasificacionResponse>>('/clasificaciones'),
  createClasificacion: (payload: ClasificacionFormPayload) => mutateData<ListResponse<ClasificacionResponse>>('/clasificaciones', 'POST', payload),
  updateClasificacion: (id: number, payload: ClasificacionFormPayload) => mutateData<ListResponse<ClasificacionResponse>>(`/clasificaciones/${id}`, 'PUT', payload),
  changeClasificacionStatus: (id: number, estado: string) => mutateData<ListResponse<ClasificacionResponse>>(`/clasificaciones/${id}/estado?estado=${encodeURIComponent(estado)}`, 'PATCH'),
  despachos: () => getData<ListResponse<DespachoResponse>>('/despachos'),
  createDespacho: (payload: DespachoFormPayload) => mutateData<ListResponse<DespachoResponse>>('/despachos', 'POST', payload),
  updateDespacho: (id: number, payload: DespachoFormPayload) => mutateData<ListResponse<DespachoResponse>>(`/despachos/${id}`, 'PUT', payload),
  changeDespachoStatus: (id: number, estado: string) => mutateData<ListResponse<DespachoResponse>>(`/despachos/${id}/estado?estado=${encodeURIComponent(estado)}`, 'PATCH'),
  cargasDespacho: () => getData<ListResponse<CargaDespachoResponse>>('/cargas-despacho'),
  createCargaDespacho: (payload: CargaDespachoFormPayload) => mutateData<ListResponse<CargaDespachoResponse>>('/cargas-despacho', 'POST', payload),
  addCargaDespachoLinea: (id: number, despachoId: number) => mutateData<ListResponse<CargaDespachoResponse>>(`/cargas-despacho/${id}/lineas`, 'POST', { despachoId }),
  removeCargaDespachoLinea: (id: number, despachoId: number) => mutateData<ListResponse<CargaDespachoResponse>>(`/cargas-despacho/${id}/lineas/${despachoId}`, 'DELETE'),
  changeCargaDespachoStatus: (id: number, estado: string) => mutateData<ListResponse<CargaDespachoResponse>>(`/cargas-despacho/${id}/estado?estado=${encodeURIComponent(estado)}`, 'PATCH'),
  trazabilidad: () => getData<ListResponse<TrazabilidadResponse>>('/reportes/trazabilidad'),
  usuarios: () => getData<ListResponse<UserReferenceResponse>>('/usuarios'),
  roles: () => getData<RoleDetailResponse[]>('/roles'),
  role: (id: number) => getData<RoleDetailResponse>(`/roles/${id}`),
  updateRole: (id: number, payload: RoleUpdatePayload) => mutateData<RoleDetailResponse>(`/roles/${id}`, 'PUT', payload),
  changeRoleState: (id: number, activo: boolean) => mutateData<RoleDetailResponse>(`/roles/${id}/estado`, 'PATCH', { activo }),
  createUsuario: (payload: UserFormPayload) => mutateData<ListResponse<UserReferenceResponse>>('/usuarios', 'POST', payload),
  updateUsuario: (id: number, payload: UserFormPayload) => mutateData<ListResponse<UserReferenceResponse>>(`/usuarios/${id}`, 'PUT', payload),
  toggleUsuarioStatus: (id: number) => mutateData<ListResponse<UserReferenceResponse>>(`/usuarios/${id}/estado`, 'PATCH'),
  resetUsuarioPassword: (id: number, temporaryPassword: string) => mutateData<void>(`/usuarios/${id}/password`, 'PATCH', { temporaryPassword }),
  lotesTrazables: () => getData<LoteTrazableResponse[]>('/lotes-trazables'),
  lotesTrazablesActivos: () => getData<LoteTrazableResponse[]>('/lotes-trazables/activos'),
  loteTrazable: (id: number) => getData<LoteTrazableDetailResponse>(`/lotes-trazables/${id}`),
  createLoteTrazable: (payload: LoteTrazableFormPayload) => mutateData<LoteTrazableResponse>('/lotes-trazables', 'POST', payload),
  updateLoteTrazable: (id: number, payload: LoteTrazableFormPayload) => mutateData<LoteTrazableResponse>(`/lotes-trazables/${id}`, 'PUT', payload),
  changeLoteTrazableState: (id: number, estado: string, motivo?: string) => mutateData<LoteTrazableResponse>(`/lotes-trazables/${id}/estado?estado=${encodeURIComponent(estado)}${motivo ? `&motivo=${encodeURIComponent(motivo)}` : ''}`, 'PATCH'),
  normalizeLegacyMovement: (traceId: number, payload: LegacyNormalizationPayload) => mutateData<LoteTrazableDetailResponse>(`/lotes-trazables/${traceId}/normalizar-legado`, 'POST', payload),
  mermas: () => getData<MermaResponse[]>('/mermas'),
  createMerma: (payload: MermaFormPayload) => mutateData<MermaResponse[]>('/mermas', 'POST', payload),
  annulMerma: (id: number, motivo: string) => mutateData<MermaResponse[]>(`/mermas/${id}/anular?motivo=${encodeURIComponent(motivo)}`, 'PATCH'),
  auditoria: (params?: { page?: number; size?: number; modulo?: string; accion?: string; usuario?: string; referencia?: string; desde?: string; hasta?: string }) => getData<PageResponse<AuditResponse>>(queryPath('/auditoria', params))

};
