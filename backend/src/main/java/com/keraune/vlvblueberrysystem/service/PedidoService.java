package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.PedidoResponse;
import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.PedidoDetalleResponse;
import com.keraune.vlvblueberrysystem.api.mapper.ApiRecordMapper;
import com.keraune.vlvblueberrysystem.audit.AuditService;
import com.keraune.vlvblueberrysystem.dto.PedidoDetalleForm;
import com.keraune.vlvblueberrysystem.dto.PedidoForm;
import com.keraune.vlvblueberrysystem.entity.Pedido;
import com.keraune.vlvblueberrysystem.entity.PedidoDetalle;
import com.keraune.vlvblueberrysystem.repository.PedidoDetalleRepository;
import com.keraune.vlvblueberrysystem.repository.PedidoRepository;
import com.keraune.vlvblueberrysystem.repository.DespachoRepository;
import com.keraune.vlvblueberrysystem.repository.EmpaqueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class PedidoService {
    private static final Set<String> ESTADOS = Set.of("BORRADOR", "CONFIRMADO", "PARCIAL", "COMPLETADO", "CANCELADO");
    private final PedidoRepository pedidos;
    private final PedidoDetalleRepository detalles;
    private final AccountService account;
    private final ApiRecordMapper mapper;
    private final AuditService audit;
    private final DespachoRepository despachos;
    private final EmpaqueRepository empaques;

    public PedidoService(PedidoRepository pedidos, PedidoDetalleRepository detalles, AccountService account, ApiRecordMapper mapper, AuditService audit, DespachoRepository despachos, EmpaqueRepository empaques) {
        this.pedidos = pedidos; this.detalles = detalles; this.account = account; this.mapper = mapper; this.audit = audit; this.despachos = despachos; this.empaques = empaques;
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> list() { return pedidos.findAllByOrderByFechaCompromisoAscIdDesc().stream().map(this::response).toList(); }

    @Transactional(readOnly = true)
    public PedidoDetalle detail(Long id) { return detalles.findById(id).orElseThrow(() -> new IllegalArgumentException("Detalle de pedido no encontrado.")); }

    @Transactional(readOnly = true)
    public Pedido get(Long id) { return pedidos.findById(id).orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado.")); }

    public List<PedidoResponse> create(PedidoForm form) {
        String code = code(form.codigo());
        if (pedidos.existsByCodigoIgnoreCase(code)) throw new IllegalArgumentException("Ya existe un pedido con ese código.");
        Pedido entity = new Pedido(); entity.setUsuarioRegistro(account.currentUser());
        apply(entity, form, true); pedidos.save(entity); replaceDetails(entity, form.detalles());
        audit.record("PEDIDOS", "CREAR", "Pedido", entity.getId(), entity.getCodigo(), "Se registró un pedido con cantidades por variedad.");
        return list();
    }

    public List<PedidoResponse> update(Long id, PedidoForm form) {
        Pedido entity = get(id);
        if (!entity.getCodigo().equalsIgnoreCase(code(form.codigo()))) throw new IllegalArgumentException("El código del pedido no puede modificarse.");
        if ("COMPLETADO".equalsIgnoreCase(entity.getEstado()) || "CANCELADO".equalsIgnoreCase(entity.getEstado())) throw new IllegalArgumentException("Este pedido ya no puede modificarse.");
        ensureDetailsCanBeReplaced(entity);
        apply(entity, form, false); replaceDetails(entity, form.detalles());
        audit.record("PEDIDOS", "ACTUALIZAR", "Pedido", entity.getId(), entity.getCodigo(), "Se actualizó el pedido por variedad.");
        return list();
    }

    public List<PedidoResponse> changeState(Long id, String requested) {
        Pedido entity = get(id);
        String state = state(requested);
        if ("COMPLETADO".equals(state)) throw new IllegalArgumentException("El pedido se marca completado automáticamente cuando se confirma el despacho de todas sus cantidades.");
        if ("CANCELADO".equals(state)) ensureCanCancel(entity);
        entity.setEstado(state);
        audit.record("PEDIDOS", "CAMBIAR_ESTADO", "Pedido", entity.getId(), entity.getCodigo(), "El pedido quedó " + state + ".");
        return list();
    }

    public void refreshLifecycle(Pedido pedido, long confirmedQuantity, long requestedQuantity) {
        if (pedido == null || "CANCELADO".equalsIgnoreCase(pedido.getEstado())) return;
        if (requestedQuantity > 0 && confirmedQuantity >= requestedQuantity) pedido.setEstado("COMPLETADO");
        else if (confirmedQuantity > 0) pedido.setEstado("PARCIAL");
        else if (!"BORRADOR".equalsIgnoreCase(pedido.getEstado())) pedido.setEstado("CONFIRMADO");
    }

    private void apply(Pedido entity, PedidoForm form, boolean creating) {
        if (form.fechaCompromiso() == null) throw new IllegalArgumentException("La fecha compromiso es obligatoria.");
        String next = state(form.estado());
        if (creating && !"BORRADOR".equals(next) && !"CONFIRMADO".equals(next)) throw new IllegalArgumentException("Un pedido nuevo debe iniciar como borrador o confirmado.");
        entity.setCodigo(code(form.codigo())); entity.setCliente(form.cliente().trim()); entity.setDestino(trim(form.destino()));
        entity.setFechaCompromiso(form.fechaCompromiso()); entity.setEstado(next); entity.setObservacion(trim(form.observacion()));
    }

    private void replaceDetails(Pedido pedido, List<PedidoDetalleForm> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("Registra al menos una variedad y cantidad solicitada para el pedido.");
        }
        Set<String> varieties = new HashSet<>();
        for (PedidoDetalleForm row : rows) {
            String variety = row.variedad().trim().toUpperCase(Locale.ROOT);
            if (!varieties.add(variety)) throw new IllegalArgumentException("Cada variedad debe aparecer una sola vez dentro del pedido.");
        }
        detalles.findByPedidoIdOrderByVariedadAsc(pedido.getId()).forEach(detalles::delete);
        detalles.flush();
        for (PedidoDetalleForm row : rows) {
            PedidoDetalle detail = new PedidoDetalle();
            detail.setPedido(pedido);
            detail.setVariedad(row.variedad().trim());
            detail.setCantidadSolicitada(row.cantidadSolicitada());
            detail.setObservacion(trim(row.observacion()));
            detalles.save(detail);
        }
    }

    private void ensureDetailsCanBeReplaced(Pedido pedido) {
        boolean alreadyCommitted = detalles.findByPedidoIdOrderByVariedadAsc(pedido.getId()).stream()
                .anyMatch(detail -> !despachos.findByPedidoDetalleId(detail.getId()).isEmpty()
                        || !empaques.findByPedidoDetalleId(detail.getId()).isEmpty());
        if (alreadyCommitted) {
            throw new IllegalArgumentException("No se pueden cambiar las variedades o cantidades de un pedido que ya tiene empaques o despachos vinculados.");
        }
    }

    private void ensureCanCancel(Pedido pedido) {
        boolean alreadyCommitted = detalles.findByPedidoIdOrderByVariedadAsc(pedido.getId()).stream()
                .anyMatch(detail -> despachos.findByPedidoDetalleId(detail.getId()).stream()
                                .anyMatch(despacho -> !"CANCELADO".equalsIgnoreCase(despacho.getEstado()))
                        || empaques.findByPedidoDetalleId(detail.getId()).stream()
                                .anyMatch(empaque -> !"ANULADO".equalsIgnoreCase(empaque.getEstado())));
        if (alreadyCommitted) {
            throw new IllegalArgumentException("No se puede cancelar un pedido con empaques o despachos vigentes. Regulariza primero los movimientos asociados.");
        }
    }

    private PedidoResponse response(Pedido pedido) {
        List<PedidoDetalleResponse> rows = detalles.findByPedidoIdOrderByVariedadAsc(pedido.getId()).stream().map(detail -> {
            int sent = (int) despachos.findByPedidoDetalleId(detail.getId()).stream()
                    .filter(item -> "DESPACHADO".equalsIgnoreCase(item.getEstado()))
                    .mapToLong(item -> item.getCantidadDespachada() == null ? 0 : item.getCantidadDespachada()).sum();
            int requested = detail.getCantidadSolicitada() == null ? 0 : detail.getCantidadSolicitada();
            return new PedidoDetalleResponse(detail.getId(), detail.getVariedad(), requested, sent, Math.max(0, requested - sent), detail.getObservacion());
        }).toList();
        return new PedidoResponse(pedido.getId(), pedido.getCodigo(), pedido.getCliente(), pedido.getDestino(), pedido.getFechaCompromiso(), pedido.getEstado(), pedido.getObservacion(), rows, mapper.user(pedido.getUsuarioRegistro()), pedido.getFechaCreacion(), pedido.getFechaActualizacion());
    }

    private String state(String value) { String result = value == null ? "" : value.trim().toUpperCase(Locale.ROOT); if (!ESTADOS.contains(result)) throw new IllegalArgumentException("Estado de pedido no válido."); return result; }
    private String code(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
