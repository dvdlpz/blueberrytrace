package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.EmpaqueResponse;
import com.keraune.vlvblueberrysystem.api.mapper.ApiRecordMapper;
import com.keraune.vlvblueberrysystem.audit.AuditService;
import com.keraune.vlvblueberrysystem.dto.EmpaqueForm;
import com.keraune.vlvblueberrysystem.entity.*;
import com.keraune.vlvblueberrysystem.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class EmpaqueService {
    public static final String JABA_COSECHERA = "JABA_COSECHERA";
    public static final String BIN_MADERA = "BIN_MADERA";
    private static final Set<String> TIPOS = Set.of(JABA_COSECHERA, BIN_MADERA);
    private final EmpaqueRepository empaques;
    private final ClasificacionRepository clasificaciones;
    private final PedidoDetalleRepository pedidosDetalles;
    private final TraceabilityService traceability;
    private final AccountService account;
    private final ApiRecordMapper mapper;
    private final AuditService audit;
    private final DespachoRepository despachos;

    public EmpaqueService(EmpaqueRepository empaques, ClasificacionRepository clasificaciones, PedidoDetalleRepository pedidosDetalles,
                          TraceabilityService traceability, AccountService account, ApiRecordMapper mapper, AuditService audit, DespachoRepository despachos) {
        this.empaques = empaques; this.clasificaciones = clasificaciones; this.pedidosDetalles = pedidosDetalles;
        this.traceability = traceability; this.account = account; this.mapper = mapper; this.audit = audit; this.despachos = despachos;
    }

    @Transactional(readOnly = true)
    public List<EmpaqueResponse> list() { return empaques.findAllByOrderByFechaEmpaqueDescIdDesc().stream().map(this::response).toList(); }

    @Transactional(readOnly = true)
    public Empaque get(Long id) { return empaques.findById(id).orElseThrow(() -> new IllegalArgumentException("Empaque no encontrado.")); }

    public List<EmpaqueResponse> create(EmpaqueForm form) {
        LoteTrazable trace = traceability.activeTrace(form.loteTrazableId());
        Clasificacion classification = clasificaciones.findById(form.clasificacionId()).orElseThrow(() -> new IllegalArgumentException("Clasificación no encontrada."));
        if (!"VALIDADA".equalsIgnoreCase(classification.getEstado()) || classification.getLoteTrazable() == null || !Objects.equals(classification.getLoteTrazable().getId(), trace.getId())) {
            throw new IllegalArgumentException("El empaque debe crearse a partir de una clasificación validada del lote trazable seleccionado.");
        }
        PedidoDetalle detail = pedidosDetalles.findById(form.pedidoDetalleId()).orElseThrow(() -> new IllegalArgumentException("Detalle de pedido no encontrado."));
        if (!"CONFIRMADO".equalsIgnoreCase(detail.getPedido().getEstado()) && !"PARCIAL".equalsIgnoreCase(detail.getPedido().getEstado())) {
            throw new IllegalArgumentException("Confirma el pedido antes de preparar empaques para su variedad.");
        }
        if (!detail.getVariedad().trim().equalsIgnoreCase(trace.getVariedad().trim())) {
            throw new IllegalArgumentException("La variedad del pedido debe coincidir con la variedad del lote trazable.");
        }
        String type = type(form.tipo()); int capacity = capacity(type, form.capacidadPorUnidad());
        int plants = Math.multiplyExact(capacity, form.cantidadUnidades());
        if (form.fechaEmpaque() == null || form.fechaEmpaque().isAfter(LocalDate.now()) || form.fechaEmpaque().isBefore(classification.getFechaClasificacion())) {
            throw new IllegalArgumentException("La fecha de empaque debe ser igual o posterior a la clasificación y no puede ser futura.");
        }
        long alreadyPackedFromClassification = empaques.findByClasificacionId(classification.getId()).stream().filter(item -> !"ANULADO".equalsIgnoreCase(item.getEstado())).mapToLong(item -> safe(item.getCantidadPlantas())).sum();
        long classificationAvailable = Math.max(0, safe(classification.getCantidad()) - alreadyPackedFromClassification);
        if (plants > classificationAvailable) throw new IllegalArgumentException("El empaque excede las plantas validadas disponibles de la clasificación (" + classificationAvailable + ").");
        long packedForOrder = empaques.findByPedidoDetalleId(detail.getId()).stream().filter(item -> !"ANULADO".equalsIgnoreCase(item.getEstado())).mapToLong(item -> safe(item.getCantidadPlantas())).sum();
        long orderAvailable = Math.max(0, safe(detail.getCantidadSolicitada()) - packedForOrder);
        if (plants > orderAvailable) throw new IllegalArgumentException("El empaque excede la cantidad pendiente del pedido para esta variedad (" + orderAvailable + ").");

        Empaque entity = new Empaque(); entity.setLoteTrazable(trace); entity.setClasificacion(classification); entity.setPedidoDetalle(detail); entity.setTipo(type); entity.setCapacidadPorUnidad(capacity); entity.setCantidadUnidades(form.cantidadUnidades()); entity.setCantidadPlantas(plants); entity.setFechaEmpaque(form.fechaEmpaque()); entity.setEstado("PREPARADO"); entity.setObservacion(trim(form.observacion())); entity.setUsuarioRegistro(account.currentUser());
        empaques.save(entity);
        audit.record("EMPAQUES", "CREAR", "Empaque", entity.getId(), trace.getCodigo(), "Se prepararon " + plants + " plantas para el pedido " + detail.getPedido().getCodigo() + ".");
        return list();
    }

    public List<EmpaqueResponse> annul(Long id) {
        Empaque entity = get(id);
        if (despachos.findByEmpaqueId(entity.getId()).stream().anyMatch(dispatch -> !"CANCELADO".equalsIgnoreCase(dispatch.getEstado()))) {
            throw new IllegalArgumentException("No se puede anular un empaque que tiene un despacho preparado o confirmado.");
        }
        entity.setEstado("ANULADO");
        audit.record("EMPAQUES", "ANULAR", "Empaque", entity.getId(), entity.getLoteTrazable().getCodigo(), "El empaque fue anulado sin eliminar el historial.");
        return list();
    }

    public int capacity(String type, Integer specified) {
        if (JABA_COSECHERA.equals(type)) return 15;
        if (specified == null || specified <= 100) throw new IllegalArgumentException("Indica la capacidad real del bin de madera. Debe ser mayor a 100 plantas.");
        return specified;
    }

    public void refreshState(Empaque empaque, long dispatchedUnits) {
        if (empaque == null || "ANULADO".equalsIgnoreCase(empaque.getEstado())) return;
        if (dispatchedUnits >= safe(empaque.getCantidadUnidades())) empaque.setEstado("DESPACHADO");
        else if (dispatchedUnits > 0) empaque.setEstado("PARCIAL");
        else empaque.setEstado("PREPARADO");
    }

    private EmpaqueResponse response(Empaque item) {
        int sent = (int) despachos.findByEmpaqueId(item.getId()).stream()
                .filter(dispatch -> "DESPACHADO".equalsIgnoreCase(dispatch.getEstado()))
                .mapToLong(dispatch -> dispatch.getUnidadesEmpaque() == null ? 0 : dispatch.getUnidadesEmpaque()).sum();
        int total = safe(item.getCantidadUnidades());
        Pedido pedido = item.getPedidoDetalle() == null ? null : item.getPedidoDetalle().getPedido();
        return new EmpaqueResponse(item.getId(), mapper.reference(item.getLoteTrazable()), mapper.reference(item.getClasificacion()),
                mapper.reference(pedido), mapper.reference(item.getPedidoDetalle()), item.getTipo(), item.getCapacidadPorUnidad(),
                total, item.getCantidadPlantas(), sent, Math.max(0, total - sent), item.getFechaEmpaque(), item.getEstado(), item.getObservacion(), mapper.user(item.getUsuarioRegistro()), item.getFechaCreacion(), item.getFechaActualizacion());
    }

    private String type(String value) { String result = value == null ? "" : value.trim().toUpperCase(Locale.ROOT); if (!TIPOS.contains(result)) throw new IllegalArgumentException("Tipo de empaque no válido. Selecciona jaba cosechera o bin de madera."); return result; }
    private int safe(Integer value) { return value == null ? 0 : value; }
    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
