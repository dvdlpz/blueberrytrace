package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.DespachoResponse;
import com.keraune.vlvblueberrysystem.api.mapper.ApiRecordMapper;
import com.keraune.vlvblueberrysystem.audit.AuditService;
import com.keraune.vlvblueberrysystem.dto.DespachoForm;
import com.keraune.vlvblueberrysystem.entity.*;
import com.keraune.vlvblueberrysystem.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class DespachoService {
    private static final Set<String> ESTADOS = Set.of("REGISTRADO", "DESPACHADO", "OBSERVADO", "CANCELADO");
    private final DespachoRepository repository;
    private final ClasificacionRepository clasificaciones;
    private final PedidoRepository pedidos;
    private final PedidoDetalleRepository pedidoDetalles;
    private final AccountService account;
    private final OperationReferenceService references;
    private final TraceabilityService traceability;
    private final OperationalQuantityGuard guard;
    private final EmpaqueService empaques;
    private final PedidoService pedidoService;
    private final ApiRecordMapper mapper;
    private final AuditService audit;

    public DespachoService(DespachoRepository repository, ClasificacionRepository clasificaciones, PedidoRepository pedidos,
                           PedidoDetalleRepository pedidoDetalles, AccountService account, OperationReferenceService references,
                           TraceabilityService traceability, OperationalQuantityGuard guard, EmpaqueService empaques,
                           PedidoService pedidoService, ApiRecordMapper mapper, AuditService audit) {
        this.repository = repository; this.clasificaciones = clasificaciones; this.pedidos = pedidos; this.pedidoDetalles = pedidoDetalles;
        this.account = account; this.references = references; this.traceability = traceability; this.guard = guard; this.empaques = empaques;
        this.pedidoService = pedidoService; this.mapper = mapper; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<DespachoResponse> list() { return repository.findAllByOrderByFechaDespachoDescIdDesc().stream().map(mapper::despacho).toList(); }

    public List<DespachoResponse> create(DespachoForm form) {
        Despacho entity = new Despacho(); entity.setUsuarioRegistro(account.currentUser()); apply(entity, form); repository.save(entity);
        audit.record("DESPACHO", "CREAR", "Despacho", entity.getId(), reference(entity), "Se preparó un despacho según pedido, variedad y empaque.");
        refreshLifecycle(entity);
        return list();
    }

    public List<DespachoResponse> update(Long id, DespachoForm form) {
        Despacho entity = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Despacho no encontrado."));
        ensureTraceable(entity);
        if (entity.getCargaDespacho() != null && !"CANCELADA".equalsIgnoreCase(entity.getCargaDespacho().getEstado())) {
            throw new IllegalArgumentException("Esta línea pertenece a una carga de tráiler. Retírala de la carga preparada antes de editarla.");
        }
        if ("DESPACHADO".equalsIgnoreCase(entity.getEstado())) throw new IllegalArgumentException("Un despacho confirmado no se edita. Registra una corrección o cancelación auditada.");
        apply(entity, form);
        audit.record("DESPACHO", "ACTUALIZAR", "Despacho", entity.getId(), reference(entity), "Se actualizó un despacho preparado.");
        refreshLifecycle(entity);
        return list();
    }

    public List<DespachoResponse> changeStatus(Long id, String requestedState) {
        Despacho entity = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Despacho no encontrado."));
        String state = references.allowedState(requestedState, "REGISTRADO", ESTADOS, "despacho");
        ensureTraceable(entity);
        if ("CANCELADO".equalsIgnoreCase(entity.getEstado()) && !"CANCELADO".equals(state)) {
            throw new IllegalArgumentException("Un despacho cancelado no puede reactivarse. Registra uno nuevo para conservar el historial.");
        }
        if ("DESPACHADO".equalsIgnoreCase(entity.getEstado()) && "CANCELADO".equals(state)) {
            throw new IllegalArgumentException("Un despacho confirmado no se cancela desde esta pantalla. Registra una corrección operativa auditada.");
        }
        if (entity.getCargaDespacho() != null && !"CANCELADA".equalsIgnoreCase(entity.getCargaDespacho().getEstado())) {
            throw new IllegalArgumentException("Esta línea forma parte de la carga " + entity.getCargaDespacho().getCodigo() + ". Confirma o regulariza la carga desde el módulo de despachos.");
        }
        if ("DESPACHADO".equals(state)) {
            throw new IllegalArgumentException("La salida debe confirmarse desde una carga de tráiler. Agrega esta línea a una carga preparada y confirma la salida del vehículo.");
        }
        entity.setEstado(state);
        refreshLifecycle(entity);
        audit.record("DESPACHO", "CAMBIAR_ESTADO", "Despacho", entity.getId(), reference(entity), "El despacho quedó " + state + ".");
        return list();
    }

    /** Confirms a registered line as part of one already validated trailer load. */
    public void confirmFromCarga(Despacho entity, CargaDespacho carga) {
        if (entity == null || carga == null || entity.getCargaDespacho() == null || !Objects.equals(entity.getCargaDespacho().getId(), carga.getId())) {
            throw new IllegalArgumentException("La línea no pertenece a la carga de tráiler indicada.");
        }
        ensureTraceable(entity);
        if (!"REGISTRADO".equalsIgnoreCase(entity.getEstado())) {
            throw new IllegalArgumentException("Solo una línea registrada puede confirmarse con la carga de tráiler.");
        }
        guard.validateDespacho(entity.getLoteTrazable(), entity.getClasificacion(), entity.getId(), entity.getCantidadDespachada(), "DESPACHADO");
        assertPackageUnits(entity.getEmpaque(), entity.getId(), entity.getUnidadesEmpaque());
        entity.setFechaDespacho(carga.getFechaCarga());
        entity.setVehiculo(carga.getVehiculo());
        entity.setGuiaRemision(carga.getGuiaRemision());
        entity.setDestino(carga.getDestino());
        entity.setEstado("DESPACHADO");
        refreshLifecycle(entity);
        audit.record("DESPACHO", "CONFIRMAR_DESDE_CARGA", "Despacho", entity.getId(), reference(entity),
                "La línea se confirmó al salir el tráiler " + carga.getCodigo() + ".");
    }

    private void apply(Despacho entity, DespachoForm form) {
        Lote lote = references.lote(form.loteId());
        LoteTrazable trace = traceability.traceForDispatch(form.loteTrazableId(), lote.getId());
        Clasificacion classification = clasificaciones.findById(form.clasificacionId()).orElseThrow(() -> new IllegalArgumentException("Clasificación no encontrada."));
        Pedido pedido = pedidos.findById(form.pedidoId()).orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));
        PedidoDetalle detail = pedidoDetalles.findById(form.pedidoDetalleId()).orElseThrow(() -> new IllegalArgumentException("Detalle de pedido no encontrado."));
        Empaque empaque = empaques.get(form.empaqueId());
        if (detail.getPedido() == null || !Objects.equals(detail.getPedido().getId(), pedido.getId())) throw new IllegalArgumentException("El detalle seleccionado no pertenece al pedido indicado.");
        if (empaque.getPedidoDetalle() == null || !Objects.equals(empaque.getPedidoDetalle().getId(), detail.getId())) throw new IllegalArgumentException("El empaque seleccionado no corresponde a la variedad solicitada.");
        if (empaque.getClasificacion() == null || !Objects.equals(empaque.getClasificacion().getId(), classification.getId())) throw new IllegalArgumentException("El empaque seleccionado no corresponde a la clasificación indicada.");
        if (classification.getLoteTrazable() == null || !Objects.equals(classification.getLoteTrazable().getId(), trace.getId()) || empaque.getLoteTrazable() == null || !Objects.equals(empaque.getLoteTrazable().getId(), trace.getId())) throw new IllegalArgumentException("La clasificación y el empaque deben pertenecer al lote trazable seleccionado.");
        if (!"VALIDADA".equalsIgnoreCase(classification.getEstado())) throw new IllegalArgumentException("Solo se puede despachar una clasificación validada.");
        if ("ANULADO".equalsIgnoreCase(empaque.getEstado())) throw new IllegalArgumentException("El empaque seleccionado fue anulado.");
        if (!"CONFIRMADO".equalsIgnoreCase(pedido.getEstado()) && !"PARCIAL".equalsIgnoreCase(pedido.getEstado())) throw new IllegalArgumentException("El pedido debe estar confirmado antes de preparar el despacho.");
        int units = form.unidadesEmpaque();
        assertPackageUnits(empaque, entity.getId(), units);
        int quantity = Math.multiplyExact(units, empaque.getCapacidadPorUnidad());
        String state = references.allowedState(form.estado(), "REGISTRADO", ESTADOS, "despacho");
        if (entity.getId() == null && !"REGISTRADO".equals(state)) throw new IllegalArgumentException("Un despacho nuevo debe iniciar como registrado. Confírmalo cuando las unidades ingresen al tráiler.");
        validateDate(trace, form.fechaDespacho(), classification, empaque);
        guard.validateDespacho(trace, classification, entity.getId(), quantity, state);
        entity.setLote(lote); entity.setLoteTrazable(trace); entity.setClasificacion(classification); entity.setPedido(pedido); entity.setPedidoDetalle(detail); entity.setEmpaque(empaque);
        entity.setUnidadesEmpaque(units); entity.setFechaDespacho(form.fechaDespacho()); entity.setModalidadDespacho(empaque.getTipo()); entity.setModalidad(empaque.getTipo());
        entity.setCantidadDespachada(quantity); entity.setCantidad(quantity); entity.setDestino(pedido.getDestino()); entity.setVehiculo(references.trim(form.vehiculo())); entity.setGuiaRemision(references.trim(form.guiaRemision()));
        entity.setValidacionCalidad(form.validacionCalidad().trim()); entity.setObservacion(references.trim(form.observacion())); entity.setEstado(state);
    }

    private void assertPackageUnits(Empaque empaque, Long currentId, int units) {
        long reserved = repository.findByEmpaqueId(empaque.getId()).stream().filter(item -> !sameId(item.getId(), currentId)).filter(item -> !"CANCELADO".equalsIgnoreCase(item.getEstado())).mapToLong(item -> safe(item.getUnidadesEmpaque())).sum();
        long available = Math.max(0, safe(empaque.getCantidadUnidades()) - reserved);
        if (units > available) throw new IllegalArgumentException("Las unidades indicadas superan el empaque disponible (" + available + ").");
    }

    private void refreshLifecycle(Despacho entity) {
        Empaque empaque = entity.getEmpaque();
        if (empaque != null) {
            long dispatchedUnits = repository.findByEmpaqueId(empaque.getId()).stream().filter(item -> "DESPACHADO".equalsIgnoreCase(item.getEstado())).mapToLong(item -> safe(item.getUnidadesEmpaque())).sum();
            empaques.refreshState(empaque, dispatchedUnits);
        }
        Pedido pedido = entity.getPedido();
        if (pedido != null) {
            long requested = pedidoDetalles.findByPedidoIdOrderByVariedadAsc(pedido.getId()).stream().mapToLong(item -> safe(item.getCantidadSolicitada())).sum();
            long dispatched = pedidoDetalles.findByPedidoIdOrderByVariedadAsc(pedido.getId()).stream().flatMap(detail -> repository.findByPedidoDetalleId(detail.getId()).stream()).filter(item -> "DESPACHADO".equalsIgnoreCase(item.getEstado())).mapToLong(item -> safe(item.getCantidadDespachada())).sum();
            pedidoService.refreshLifecycle(pedido, dispatched, requested);
        }
    }

    private void ensureTraceable(Despacho entity) { if (entity.getLoteTrazable() == null || entity.getClasificacion() == null || entity.getEmpaque() == null) throw new IllegalArgumentException("Este despacho histórico se conserva como referencia. Registra un despacho nuevo desde un empaque preparado."); }
    private void validateDate(LoteTrazable trace, LocalDate date, Clasificacion classification, Empaque empaque) { if (date == null || date.isAfter(LocalDate.now())) throw new IllegalArgumentException("La fecha de despacho no puede ser futura."); if (date.isBefore(trace.getFechaIngreso()) || date.isBefore(classification.getFechaClasificacion()) || date.isBefore(empaque.getFechaEmpaque())) throw new IllegalArgumentException("La fecha de despacho debe ser igual o posterior a la clasificación y al empaque."); }
    private String reference(Despacho entity) { return entity.getLoteTrazable() == null ? "Despacho #" + entity.getId() : entity.getLoteTrazable().getCodigo(); }
    private boolean sameId(Long a, Long b) { return b != null && Objects.equals(a,b); }
    private int safe(Integer value) { return value == null ? 0 : value; }
}
