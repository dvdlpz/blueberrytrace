export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface ListResponse<T> {
  total: number;
  items: T[];
}

export interface ReferenceResponse {
  id: number;
  codigo: string;
  descripcion: string | null;
}

export interface UserReferenceResponse {
  id: number;
  username: string;
  nombreCompleto: string;
  email: string;
  cargo: string | null;
  telefono: string | null;
  avatarColor: string | null;
  avatarImage: string | null;
  rol: string | null;
  activo: boolean;
  fechaCreacion: string | null;
  fechaActualizacion: string | null;
}

export interface AuthenticatedUserResponse {
  username: string;
  nombreCompleto: string;
  email: string;
  cargo: string | null;
  telefono: string | null;
  avatarColor: string | null;
  avatarImage: string | null;
  rol: string | null;
  requiereCambioPassword: boolean;
  authorities: string[];
}

export interface UserFormPayload {
  username: string;
  nombreCompleto: string;
  email: string;
  cargo?: string;
  telefono?: string;
  avatarColor?: string;
  rol: string;
  password?: string;
  activo: boolean;
}


export interface ProfileUpdatePayload {
  nombreCompleto: string;
  email: string;
  cargo?: string;
  telefono?: string;
  avatarColor?: string;
  avatarImage?: string | null;
}

export interface PasswordChangePayload {
  currentPassword: string;
  newPassword: string;
}

export interface DashboardSummary {
  lotesRegistrados: number;
  lotesActivos: number;
  lotesInactivos: number;
  camasRegistradas: number;
  camasActivas: number;
  camasInactivas: number;
  capacidadReferencialTotal: number;
  siembrasRegistradas: number;
  plantasSembradas: number;
  uniformizacionesRegistradas: number;
  formalizacionesRegistradas: number;
  clasificacionesRegistradas: number;
  clasificacionesPendientes: number;
  clasificacionesValidadas: number;
  despachosRegistrados: number;
  plantasDespachadas: number;
  porcentajeLotesActivos: number;
  porcentajeCamasActivas: number;
  porcentajeClasificacionesValidadas: number;
  porcentajePlantasDespachadas: number;
  porcentajeUniformizacionesSobreSiembras: number;
  porcentajeFormalizacionesSobreSiembras: number;
  porcentajeClasificacionesSobreSiembras: number;
  porcentajeDespachosSobreSiembras: number;
}

export interface ModuleResponse {
  key: string;
  label: string;
  mvcPath: string;
  apiPath: string;
}

export interface PaletteResponse {
  darkGreen: string;
  primaryGreen: string;
  blueberryBlue: string;
  blueberryPurple: string;
  lime: string;
  orange: string;
  surface: string;
  background: string;
}

export interface FrontendBootstrapResponse {
  appName: string;
  apiVersion: string;
  strategy: string;
  supportedFrontends: string[];
  endpoints: Array<{
    method: string;
    path: string;
    description: string;
  }>;
  modules: ModuleResponse[];
  palette: PaletteResponse;
}

export interface DashboardApiResponse {
  summary: DashboardSummary;
  modules: ModuleResponse[];
}

export interface LoteResponse {
  id: number;
  codigo: string;
  descripcion: string | null;
  cultivo: string | null;
  variedad: string | null;
  fechaRegistro: string | null;
  observacion: string | null;
  estado: string | null;
  usuarioRegistro: UserReferenceResponse | null;
  fechaCreacion: string | null;
  fechaActualizacion: string | null;
}

export interface CamaResponse {
  id: number;
  codigo: string;
  descripcion: string | null;
  capacidadReferencial: number | null;
  estado: string | null;
  lote: ReferenceResponse | null;
  usuarioRegistro: UserReferenceResponse | null;
  fechaCreacion: string | null;
  fechaActualizacion: string | null;
}

export interface JabaResponse {
  id: number;
  codigo: string;
  capacidadMacetas: number;
  ordenEnCama: number;
  macetasOcupadas: number;
  macetasDisponibles: number;
  macetasEnRecuperacion: number;
  estado: string;
  observacion: string | null;
  cama: ReferenceResponse | null;
  usuarioRegistro: UserReferenceResponse | null;
  fechaCreacion: string | null;
  fechaActualizacion: string | null;
}

export interface RecuperacionRiegoResponse {
  id: number;
  loteTrazable: ReferenceResponse | null;
  jaba: ReferenceResponse | null;
  etapaOrigen: string;
  etapaRetorno: string;
  fechaIngresoRiego: string;
  cantidadIngresada: number;
  cantidadRecuperada: number;
  cantidadDescartada: number;
  motivoDescarte: string | null;
  observacion: string | null;
  estado: string;
  mermaGenerada: ReferenceResponse | null;
  usuarioRegistro: UserReferenceResponse | null;
  fechaCreacion: string | null;
  fechaActualizacion: string | null;
}

export interface RiegoProgramadoResponse {
  id: number;
  loteTrazable: ReferenceResponse | null;
  cama: ReferenceResponse | null;
  jaba: ReferenceResponse | null;
  fechaProgramada: string;
  horaProgramada: string;
  fechaEjecucion: string | null;
  horaEjecucion: string | null;
  etapaAplicacion: string;
  estado: string;
  observacion: string | null;
  usuarioRegistro: UserReferenceResponse | null;
  fechaCreacion: string | null;
  fechaActualizacion: string | null;
}

export interface PedidoDetalleResponse {
  id: number;
  variedad: string;
  cantidadSolicitada: number;
  cantidadDespachada: number;
  cantidadPendiente: number;
  observacion: string | null;
}

export interface PedidoResponse {
  id: number;
  codigo: string;
  cliente: string;
  destino: string | null;
  fechaCompromiso: string;
  estado: string;
  observacion: string | null;
  detalles: PedidoDetalleResponse[];
  usuarioRegistro: UserReferenceResponse | null;
  fechaCreacion: string | null;
  fechaActualizacion: string | null;
}

export interface EmpaqueResponse {
  id: number;
  loteTrazable: ReferenceResponse | null;
  clasificacion: ReferenceResponse | null;
  pedido: ReferenceResponse | null;
  pedidoDetalle: ReferenceResponse | null;
  tipo: string;
  capacidadPorUnidad: number;
  cantidadUnidades: number;
  cantidadPlantas: number;
  unidadesDespachadas: number;
  unidadesPendientes: number;
  fechaEmpaque: string;
  estado: string;
  observacion: string | null;
  usuarioRegistro: UserReferenceResponse | null;
  fechaCreacion: string | null;
  fechaActualizacion: string | null;
}

export interface SiembraResponse {
  id: number;
  lote: ReferenceResponse | null;
  cama: ReferenceResponse | null;
  jaba: ReferenceResponse | null;
  fechaSiembra: string | null;
  cantidadRegistrada: number | null;
  observacion: string | null;
  estado: string | null;
  usuarioRegistro: UserReferenceResponse | null;
  loteTrazable?: ReferenceResponse | null;
  fechaCreacion: string | null;
  fechaActualizacion: string | null;
}

export interface UniformizacionResponse {
  id: number;
  lote: ReferenceResponse | null;
  cama: ReferenceResponse | null;
  jabaOrigen: ReferenceResponse | null;
  jabaDestino: ReferenceResponse | null;
  fechaUniformizacion: string | null;
  criterio: string | null;
  cantidadInicial: number | null;
  cantidadUniformizada: number | null;
  origenOperativo: string | null;
  cantidadRecuperacion: number | null;
  recuperacionRiego: ReferenceResponse | null;
  malezasRetiradas: boolean | null;
  observacion: string | null;
  estado: string | null;
  usuarioRegistro: UserReferenceResponse | null;
  loteTrazable?: ReferenceResponse | null;
  fechaCreacion: string | null;
  fechaActualizacion: string | null;
}

export interface FormalizacionResponse {
  id: number;
  lote: ReferenceResponse | null;
  cama: ReferenceResponse | null;
  fechaFormalizacion: string | null;
  detalle: string | null;
  cantidadBandejas: number | null;
  jabasMovidas: ReferenceResponse[];
  cantidadPlantas: number | null;
  ordenamientoJabas: string | null;
  observacion: string | null;
  estado: string | null;
  usuarioRegistro: UserReferenceResponse | null;
  loteTrazable?: ReferenceResponse | null;
  fechaCreacion: string | null;
  fechaActualizacion: string | null;
}

export interface ProcesoOperativoResponse {
  uniformizaciones: ListResponse<UniformizacionResponse>;
  formalizaciones: ListResponse<FormalizacionResponse>;
}

export interface ClasificacionResponse {
  id: number;
  lote: ReferenceResponse | null;
  cama: ReferenceResponse | null;
  jaba: ReferenceResponse | null;
  fechaClasificacion: string | null;
  estadoPlanta: string | null;
  tamano: string | null;
  condicion: string | null;
  cantidad: number | null;
  observacion: string | null;
  estado: string | null;
  recuperacionRiego: ReferenceResponse | null;
  usuarioRegistro: UserReferenceResponse | null;
  loteTrazable?: ReferenceResponse | null;
  fechaCreacion: string | null;
  fechaActualizacion: string | null;
}

export interface DespachoResponse {
  id: number;
  lote: ReferenceResponse | null;
  fechaDespacho: string | null;
  modalidad: string | null;
  cantidadDespachada: number | null;
  destino: string | null;
  guiaRemision: string | null;
  validacionCalidad: string | null;
  observacion: string | null;
  estado: string | null;
  usuarioRegistro: UserReferenceResponse | null;
  loteTrazable?: ReferenceResponse | null;
  clasificacion?: ReferenceResponse | null;
  pedido?: ReferenceResponse | null;
  pedidoDetalle?: ReferenceResponse | null;
  empaque?: ReferenceResponse | null;
  unidadesEmpaque?: number | null;
  vehiculo?: string | null;
  cargaDespacho?: ReferenceResponse | null;
  fechaCreacion: string | null;
  fechaActualizacion: string | null;
}

export interface CargaDespachoResponse {
  id: number;
  codigo: string;
  pedido: ReferenceResponse | null;
  fechaCarga: string | null;
  vehiculo: string;
  guiaRemision: string | null;
  destino: string | null;
  estado: string;
  observacion: string | null;
  totalLineas: number;
  totalUnidades: number;
  totalPlantas: number;
  lineas: DespachoResponse[];
  usuarioRegistro: UserReferenceResponse | null;
  fechaCreacion: string | null;
  fechaActualizacion: string | null;
}

export interface TrazabilidadResponse {
  id?: number;
  lote: ReferenceResponse | null;
  camas: number;
  siembras: number;
  plantasSembradas: number;
  uniformizaciones: number;
  formalizaciones: number;
  clasificaciones: number;
  despachos: number;
  plantasDespachadas: number;
  ultimoEvento: string | null;
}

export interface CatalogResponse {
  lotes: ReferenceResponse[];
  camas: ReferenceResponse[];
  roles: string[];
  estadosLote: string[];
  estadosCama: string[];
  estadosOperativos: string[];
  estadosClasificacion: string[];
  estadosDespacho: string[];
  modalidadesDespacho: string[];
  validacionesCalidad: string[];
  lotesTrazables?: ReferenceResponse[];
}

export interface OperationReadinessStepResponse {
  key: string;
  title: string;
  description: string;
  actionLabel: string;
  available: boolean;
  completed: boolean;
  completedItems: number;
}

export interface OperationReadinessResponse {
  title: string;
  description: string;
  recommendedKey: string;
  recommendedLabel: string;
  steps: OperationReadinessStepResponse[];
}

export interface LoteFormPayload {
  codigo: string;
  descripcion: string;
  cultivo: string;
  variedad: string;
  fechaRegistro: string;
  observacion?: string;
  estado: string;
}

export interface CamaFormPayload {
  codigo: string;
  descripcion: string;
  capacidadReferencial: number;
  estado: string;
  loteId: number;
}

export interface SiembraFormPayload {
  loteTrazableId: number;
  loteId: number;
  camaId: number;
  jabaId: number;
  fechaSiembra: string;
  cantidadRegistrada: number;
  observacion?: string;
  estado: string;
}

export interface UniformizacionFormPayload {
  loteTrazableId: number;
  loteId: number;
  camaId: number;
  jabaOrigenId: number;
  jabaDestinoId: number;
  fechaUniformizacion: string;
  criterio: string;
  cantidadInicial: number;
  cantidadUniformizada: number;
  origenOperativo: string;
  cantidadRecuperacion: number;
  malezasRetiradas: boolean;
  observacion?: string;
  estado: string;
}

export interface FormalizacionFormPayload {
  loteTrazableId: number;
  loteId: number;
  camaId: number;
  fechaFormalizacion: string;
  detalle: string;
  cantidadBandejas: number;
  jabaIds: number[];
  cantidadPlantas: number;
  ordenamientoJabas: string;
  observacion?: string;
  estado: string;
}

export interface ClasificacionFormPayload {
  loteTrazableId: number;
  loteId: number;
  camaId: number;
  jabaId: number;
  fechaClasificacion: string;
  estadoPlanta: string;
  tamano: string;
  condicion: string;
  cantidad: number;
  observacion?: string;
  estado: string;
}

export interface DespachoFormPayload {
  loteTrazableId: number;
  clasificacionId: number;
  loteId: number;
  pedidoId: number;
  pedidoDetalleId: number;
  empaqueId: number;
  unidadesEmpaque: number;
  fechaDespacho: string;
  vehiculo?: string;
  guiaRemision?: string;
  validacionCalidad: string;
  observacion?: string;
  estado: string;
}

export interface CargaDespachoFormPayload {
  codigo: string;
  pedidoId: number;
  fechaCarga: string;
  vehiculo: string;
  guiaRemision?: string;
  observacion?: string;
}

export interface CsrfResponse {
  headerName: string;
  parameterName: string;
  token: string;
}

export interface LoginPayload {
  identifier: string;
  password: string;
}


export interface RolePermissionResponse {
  module: string;
  label: string;
  actions: string[];
  accionesDisponibles: string[];
}

export interface RolePermissionSelectionPayload {
  module: string;
  accion: string;
}

export interface RoleUpdatePayload {
  descripcion: string;
  color: string;
  permisos: RolePermissionSelectionPayload[];
}

export interface RoleDetailResponse {
  id: number;
  codigo: string;
  nombreVisible: string;
  descripcion: string | null;
  color: string;
  activo: boolean;
  usuariosActivos: number;
  usuariosTotales: number;
  permisos: RolePermissionResponse[];
  modulos: string[];
  acciones: string[];
  permisosObligatorios: string[];
  usuariosAsignados: UserReferenceResponse[];
  fechaCreacion: string | null;
  fechaActualizacion: string | null;
}
export interface LoteTrazableResponse {
  id: number; codigo: string; variedad: string; procedencia: string; fechaIngreso: string;
  estado: string; observacion: string | null; legadoPendienteNormalizacion: boolean;
  loteFisico: ReferenceResponse | null; camaInicial: ReferenceResponse | null;
  usuarioResponsable: UserReferenceResponse | null; fechaCreacion: string | null; fechaActualizacion: string | null;
}
export interface LoteTrazableFormPayload {
  codigo: string; variedad: string; procedencia: string; fechaIngreso: string; estado: string;
  observacion?: string; loteFisicoId: number; camaInicialId: number;
}
export interface MermaResponse {
  id: number; loteTrazable: ReferenceResponse | null; etapaOrigen: string; motivo: string; cantidad: number;
  fechaMerma: string; observacion: string | null; estado: string; usuarioRegistro: UserReferenceResponse | null;
  fechaCreacion: string | null; fechaActualizacion: string | null;
}
export interface MermaFormPayload { loteTrazableId: number; etapaOrigen: string; motivo: string; cantidad: number; fechaMerma: string; observacion?: string; }
export interface BalanceOperativoResponse { sembradas:number; uniformizadas:number; formalizadas:number; clasificacionPendiente:number; clasificacionValidada:number; clasificacionObservada:number; despachadas:number; anuladas:number; mermas:number; saldoDisponible:number; enRecuperacion:number; }
export interface TimelineEventResponse { etapa:string; estado:string; cantidad:number | null; fecha:string | null; referencia:string; detalle:string | null; responsable:string | null; }
export interface LegacyMovementResponse { etapa:string; id:number; referencia:string; fecha:string | null; cantidad:number | null; estado:string; lote:ReferenceResponse | null; cama:ReferenceResponse | null; detalle:string | null; }
export interface LegacyNormalizationPayload { etapa:string; registroId:number; evidencia:string; }
export interface LoteTrazableDetailResponse { loteTrazable:LoteTrazableResponse; balance:BalanceOperativoResponse; lineaTiempo:TimelineEventResponse[]; mermas:MermaResponse[]; pendientesLegado:LegacyMovementResponse[]; }
export interface AuditResponse { id:number; usuario:UserReferenceResponse | null; rolNombre:string | null; modulo:string; accion:string; entidadTipo:string | null; entidadId:number | null; referencia:string | null; descripcion:string; motivo:string | null; valoresAnteriores:string | null; valoresPosteriores:string | null; ipOrigen:string | null; agenteUsuario:string | null; fechaEvento:string; }
export interface PageResponse<T> { content:T[]; totalElements:number; totalPages:number; size:number; number:number; }

export interface JabaFormPayload { codigo: string; camaId: number; capacidadMacetas: number; ordenEnCama: number; observacion?: string; estado: string; }
export interface RiegoProgramadoFormPayload { loteTrazableId: number; camaId: number; jabaId?: number; fechaProgramada: string; horaProgramada: string; etapaAplicacion: string; observacion?: string; }
export interface RiegoRealizadoPayload { fechaEjecucion: string; horaEjecucion: string; observacion?: string; }
export interface RecuperacionRiegoFormPayload { loteTrazableId: number; jabaId?: number; etapaOrigen: string; etapaRetorno: string; fechaIngresoRiego: string; cantidadIngresada: number; observacion?: string; }
export interface RecuperacionRiegoStatusPayload { cantidadRecuperada: number; cantidadDescartada: number; motivoDescarte?: string; observacion?: string; }
export interface PedidoDetalleFormPayload { variedad: string; cantidadSolicitada: number; observacion?: string; }
export interface PedidoFormPayload { codigo: string; cliente: string; destino?: string; fechaCompromiso: string; estado: string; observacion?: string; detalles: PedidoDetalleFormPayload[]; }
export interface EmpaqueFormPayload { loteTrazableId: number; clasificacionId: number; pedidoDetalleId: number; tipo: string; capacidadPorUnidad: number; cantidadUnidades: number; fechaEmpaque: string; observacion?: string; }
