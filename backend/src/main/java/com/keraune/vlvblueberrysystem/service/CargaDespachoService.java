package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.CargaDespachoResponse;
import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.DespachoResponse;
import com.keraune.vlvblueberrysystem.api.mapper.ApiRecordMapper;
import com.keraune.vlvblueberrysystem.audit.AuditService;
import com.keraune.vlvblueberrysystem.dto.CargaDespachoForm;
import com.keraune.vlvblueberrysystem.entity.CargaDespacho;
import com.keraune.vlvblueberrysystem.entity.Despacho;
import com.keraune.vlvblueberrysystem.entity.Pedido;
import com.keraune.vlvblueberrysystem.repository.CargaDespachoRepository;
import com.keraune.vlvblueberrysystem.repository.DespachoRepository;
import com.keraune.vlvblueberrysystem.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Groups individually traceable dispatch lines into one physical trailer load.
 * The order, variety and package are still validated in each Despacho line.
 */
@Service
@Transactional
public class CargaDespachoService {
    private static final Set<String> ESTADOS = Set.of("PREPARADA", "CARGADA", "DESPACHADA", "CANCELADA");

    private final CargaDespachoRepository cargas;
    private final DespachoRepository despachos;
    private final PedidoRepository pedidos;
    private final AccountService account;
    private final DespachoService despachoService;
    private final ApiRecordMapper mapper;
    private final AuditService audit;

    public CargaDespachoService(CargaDespachoRepository cargas, DespachoRepository despachos, PedidoRepository pedidos,
                                AccountService account, DespachoService despachoService, ApiRecordMapper mapper, AuditService audit) {
        this.cargas = cargas;
        this.despachos = despachos;
        this.pedidos = pedidos;
        this.account = account;
        this.despachoService = despachoService;
        this.mapper = mapper;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<CargaDespachoResponse> list() {
        return cargas.findAllByOrderByFechaCargaDescIdDesc().stream().map(this::response).toList();
    }

    public List<CargaDespachoResponse> create(CargaDespachoForm form) {
        String code = code(form.codigo());
        if (cargas.existsByCodigoIgnoreCase(code)) {
            throw new IllegalArgumentException("Ya existe una carga de tráiler con ese código.");
        }
        Pedido pedido = pedido(form.pedidoId());
        assertPedidoDisponible(pedido);
        LocalDate date = date(form.fechaCarga());
        String destination = trim(pedido.getDestino());
        if (destination == null) {
            throw new IllegalArgumentException("El pedido debe tener un destino antes de preparar la carga de tráiler.");
        }
        CargaDespacho carga = new CargaDespacho();
        carga.setUsuarioRegistro(account.currentUser());
        carga.setCodigo(code);
        carga.setPedido(pedido);
        carga.setFechaCarga(date);
        carga.setVehiculo(required(form.vehiculo(), "vehículo o tráiler"));
        carga.setGuiaRemision(trim(form.guiaRemision()));
        carga.setDestino(destination);
        carga.setObservacion(trim(form.observacion()));
        carga.setEstado("PREPARADA");
        cargas.save(carga);
        audit.record("DESPACHO", "CREAR_CARGA", "CargaDespacho", carga.getId(), carga.getCodigo(),
                "Se preparó una carga de tráiler para el pedido " + pedido.getCodigo() + ".");
        return list();
    }

    public List<CargaDespachoResponse> addLine(Long cargaId, Long despachoId) {
        CargaDespacho carga = get(cargaId);
        assertEditable(carga);
        Despacho despacho = despachos.findById(despachoId).orElseThrow(() -> new IllegalArgumentException("Línea de despacho no encontrada."));
        if (despacho.getCargaDespacho() != null) {
            throw new IllegalArgumentException("Esta línea ya pertenece a otra carga de tráiler.");
        }
        if (!"REGISTRADO".equalsIgnoreCase(despacho.getEstado())) {
            throw new IllegalArgumentException("Solo se pueden cargar líneas de despacho registradas y aún no confirmadas.");
        }
        if (despacho.getPedido() == null || !Objects.equals(despacho.getPedido().getId(), carga.getPedido().getId())) {
            throw new IllegalArgumentException("La línea seleccionada debe pertenecer al mismo pedido de la carga.");
        }
        if (!Objects.equals(despacho.getFechaDespacho(), carga.getFechaCarga())) {
            throw new IllegalArgumentException("La fecha de la línea debe coincidir con la fecha programada para la carga de tráiler.");
        }
        despacho.setCargaDespacho(carga);
        despacho.setVehiculo(carga.getVehiculo());
        despacho.setGuiaRemision(carga.getGuiaRemision());
        despacho.setDestino(carga.getDestino());
        audit.record("DESPACHO", "AGREGAR_A_CARGA", "Despacho", despacho.getId(), reference(despacho),
                "La línea se incorporó a la carga " + carga.getCodigo() + ".");
        return list();
    }

    public List<CargaDespachoResponse> removeLine(Long cargaId, Long despachoId) {
        CargaDespacho carga = get(cargaId);
        assertEditable(carga);
        Despacho despacho = despachos.findById(despachoId).orElseThrow(() -> new IllegalArgumentException("Línea de despacho no encontrada."));
        if (despacho.getCargaDespacho() == null || !Objects.equals(despacho.getCargaDespacho().getId(), carga.getId())) {
            throw new IllegalArgumentException("La línea indicada no pertenece a esta carga de tráiler.");
        }
        despacho.setCargaDespacho(null);
        audit.record("DESPACHO", "RETIRAR_DE_CARGA", "Despacho", despacho.getId(), reference(despacho),
                "La línea se retiró de la carga " + carga.getCodigo() + ".");
        return list();
    }

    public List<CargaDespachoResponse> changeState(Long id, String requestedState) {
        CargaDespacho carga = get(id);
        String state = state(requestedState);
        List<Despacho> lines = despachos.findByCargaDespachoIdOrderByIdAsc(carga.getId());
        if ("DESPACHADA".equalsIgnoreCase(carga.getEstado())) {
            throw new IllegalArgumentException("Una carga ya despachada no puede cambiarse. Registra una corrección auditada.");
        }
        assertTransition(carga.getEstado(), state);
        if ("CARGADA".equals(state) || "DESPACHADA".equals(state)) {
            if (lines.isEmpty()) {
                throw new IllegalArgumentException("Agrega al menos una línea de despacho antes de cargar el tráiler.");
            }
        }
        if ("CANCELADA".equals(state)) {
            for (Despacho line : lines) {
                if ("DESPACHADO".equalsIgnoreCase(line.getEstado())) {
                    throw new IllegalArgumentException("No se puede cancelar una carga que ya contiene líneas despachadas.");
                }
            }
            for (Despacho line : lines) {
                line.setCargaDespacho(null);
            }
        } else if ("DESPACHADA".equals(state)) {
            for (Despacho line : lines) {
                despachoService.confirmFromCarga(line, carga);
            }
        }
        carga.setEstado(state);
        audit.record("DESPACHO", "CAMBIAR_ESTADO_CARGA", "CargaDespacho", carga.getId(), carga.getCodigo(),
                "La carga de tráiler quedó " + state + ".");
        return list();
    }

    private CargaDespachoResponse response(CargaDespacho carga) {
        List<DespachoResponse> lines = despachos.findByCargaDespachoIdOrderByIdAsc(carga.getId()).stream().map(mapper::despacho).toList();
        int units = lines.stream().map(DespachoResponse::unidadesEmpaque).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        int plants = lines.stream().map(DespachoResponse::cantidadDespachada).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        return new CargaDespachoResponse(carga.getId(), carga.getCodigo(), mapper.reference(carga.getPedido()), carga.getFechaCarga(),
                carga.getVehiculo(), carga.getGuiaRemision(), carga.getDestino(), carga.getEstado(), carga.getObservacion(),
                lines.size(), units, plants, lines, mapper.user(carga.getUsuarioRegistro()), carga.getFechaCreacion(), carga.getFechaActualizacion());
    }

    private CargaDespacho get(Long id) {
        return cargas.findById(id).orElseThrow(() -> new IllegalArgumentException("Carga de tráiler no encontrada."));
    }

    private Pedido pedido(Long id) {
        return pedidos.findById(id).orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));
    }

    private void assertPedidoDisponible(Pedido pedido) {
        if (!"CONFIRMADO".equalsIgnoreCase(pedido.getEstado()) && !"PARCIAL".equalsIgnoreCase(pedido.getEstado())) {
            throw new IllegalArgumentException("El pedido debe estar confirmado o parcial antes de preparar una carga de tráiler.");
        }
    }

    private void assertEditable(CargaDespacho carga) {
        if (!"PREPARADA".equalsIgnoreCase(carga.getEstado())) {
            throw new IllegalArgumentException("Solo una carga preparada permite agregar o retirar líneas. Regrésala a preparada antes de modificarla.");
        }
    }

    private void assertTransition(String currentState, String requestedState) {
        String current = currentState == null ? "PREPARADA" : currentState.trim().toUpperCase(Locale.ROOT);
        if (Objects.equals(current, requestedState)) {
            throw new IllegalArgumentException("La carga ya se encuentra en el estado solicitado.");
        }
        boolean allowed = switch (current) {
            case "PREPARADA" -> "CARGADA".equals(requestedState) || "CANCELADA".equals(requestedState);
            case "CARGADA" -> "PREPARADA".equals(requestedState) || "DESPACHADA".equals(requestedState) || "CANCELADA".equals(requestedState);
            default -> false;
        };
        if (!allowed) {
            throw new IllegalArgumentException("El cambio de estado solicitado no corresponde al flujo de carga de tráiler.");
        }
    }

    private LocalDate date(LocalDate value) {
        if (value == null || value.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de carga no puede ser futura.");
        }
        return value;
    }

    private String state(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!ESTADOS.contains(normalized)) {
            throw new IllegalArgumentException("Estado de carga no válido.");
        }
        return normalized;
    }

    private String code(String value) {
        String normalized = trim(value);
        if (normalized == null) {
            throw new IllegalArgumentException("El código de carga es obligatorio.");
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String required(String value, String label) {
        String normalized = trim(value);
        if (normalized == null) {
            throw new IllegalArgumentException("El campo " + label + " es obligatorio.");
        }
        return normalized;
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String reference(Despacho despacho) {
        return despacho.getCargaDespacho() != null ? despacho.getCargaDespacho().getCodigo() + " · D-" + despacho.getId() : "D-" + despacho.getId();
    }
}
