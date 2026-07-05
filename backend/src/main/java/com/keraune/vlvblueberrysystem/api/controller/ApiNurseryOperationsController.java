package com.keraune.vlvblueberrysystem.api.controller;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.*;
import com.keraune.vlvblueberrysystem.dto.*;
import com.keraune.vlvblueberrysystem.service.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ApiNurseryOperationsController {
    private final JabaService jabas;
    private final RecuperacionRiegoService recuperaciones;
    private final PedidoService pedidos;
    private final EmpaqueService empaques;
    private final RiegoProgramadoService riegos;
    private final CargaDespachoService cargasDespacho;

    public ApiNurseryOperationsController(JabaService jabas, RecuperacionRiegoService recuperaciones, PedidoService pedidos, EmpaqueService empaques, RiegoProgramadoService riegos, CargaDespachoService cargasDespacho) {
        this.jabas = jabas; this.recuperaciones = recuperaciones; this.pedidos = pedidos; this.empaques = empaques; this.riegos = riegos; this.cargasDespacho = cargasDespacho;
    }

    @GetMapping("/jabas")
    @PreAuthorize("@permissionGuard.allows('jabas', 'ver')")
    public ApiResponse<ListResponse<JabaResponse>> listJabas() { List<JabaResponse> rows=jabas.list(); return ApiResponse.ok("Jabas de siembra cargadas", new ListResponse<>(rows.size(), rows)); }
    @PostMapping("/jabas")
    @PreAuthorize("@permissionGuard.allows('jabas', 'crear')")
    public ApiResponse<ListResponse<JabaResponse>> createJaba(@Valid @RequestBody JabaForm form) { List<JabaResponse> rows=jabas.create(form); return ApiResponse.ok("Jaba de siembra registrada", new ListResponse<>(rows.size(), rows)); }
    @PutMapping("/jabas/{id}")
    @PreAuthorize("@permissionGuard.allows('jabas', 'editar')")
    public ApiResponse<ListResponse<JabaResponse>> updateJaba(@PathVariable Long id, @Valid @RequestBody JabaForm form) { List<JabaResponse> rows=jabas.update(id, form); return ApiResponse.ok("Jaba de siembra actualizada", new ListResponse<>(rows.size(), rows)); }
    @PatchMapping("/jabas/{id}/estado")
    @PreAuthorize("@permissionGuard.allows('jabas', 'cambiar_estado')")
    public ApiResponse<ListResponse<JabaResponse>> stateJaba(@PathVariable Long id) { List<JabaResponse> rows=jabas.changeState(id); return ApiResponse.ok("Estado de jaba actualizado", new ListResponse<>(rows.size(), rows)); }

    @GetMapping("/riegos-programados")
    @PreAuthorize("@permissionGuard.allows('riegos', 'ver')")
    public ApiResponse<ListResponse<RiegoProgramadoResponse>> listIrrigation() { List<RiegoProgramadoResponse> rows=riegos.list(); return ApiResponse.ok("Riegos programados cargados", new ListResponse<>(rows.size(), rows)); }
    @PostMapping("/riegos-programados")
    @PreAuthorize("@permissionGuard.allows('riegos', 'programar')")
    public ApiResponse<ListResponse<RiegoProgramadoResponse>> createIrrigation(@Valid @RequestBody RiegoProgramadoForm form) { List<RiegoProgramadoResponse> rows=riegos.create(form); return ApiResponse.ok("Riego programado registrado", new ListResponse<>(rows.size(), rows)); }
    @PatchMapping("/riegos-programados/{id}/realizar")
    @PreAuthorize("@permissionGuard.allows('riegos', 'realizar')")
    public ApiResponse<ListResponse<RiegoProgramadoResponse>> completeIrrigation(@PathVariable Long id, @Valid @RequestBody RiegoRealizadoForm form) { List<RiegoProgramadoResponse> rows=riegos.complete(id, form); return ApiResponse.ok("Riego realizado registrado", new ListResponse<>(rows.size(), rows)); }
    @PatchMapping("/riegos-programados/{id}/cancelar")
    @PreAuthorize("@permissionGuard.allows('riegos', 'cancelar')")
    public ApiResponse<ListResponse<RiegoProgramadoResponse>> cancelIrrigation(@PathVariable Long id) { List<RiegoProgramadoResponse> rows=riegos.cancel(id); return ApiResponse.ok("Riego programado cancelado", new ListResponse<>(rows.size(), rows)); }

    @GetMapping("/recuperaciones-riego")
    @PreAuthorize("@permissionGuard.allows('recuperacion', 'ver')")
    public ApiResponse<ListResponse<RecuperacionRiegoResponse>> listRecovery() { List<RecuperacionRiegoResponse> rows=recuperaciones.list(); return ApiResponse.ok("Recuperaciones por riego cargadas", new ListResponse<>(rows.size(), rows)); }
    @PostMapping("/recuperaciones-riego")
    @PreAuthorize("@permissionGuard.allows('recuperacion', 'crear')")
    public ApiResponse<ListResponse<RecuperacionRiegoResponse>> createRecovery(@Valid @RequestBody RecuperacionRiegoForm form) { List<RecuperacionRiegoResponse> rows=recuperaciones.create(form); return ApiResponse.ok("Plantas enviadas a recuperación por riego", new ListResponse<>(rows.size(), rows)); }
    @PatchMapping("/recuperaciones-riego/{id}/cerrar")
    @PreAuthorize("@permissionGuard.allows('recuperacion', 'cerrar')")
    public ApiResponse<ListResponse<RecuperacionRiegoResponse>> closeRecovery(@PathVariable Long id, @Valid @RequestBody RecuperacionRiegoStatusForm form) { List<RecuperacionRiegoResponse> rows=recuperaciones.close(id, form); return ApiResponse.ok("Recuperación por riego cerrada", new ListResponse<>(rows.size(), rows)); }

    @GetMapping("/pedidos")
    @PreAuthorize("@permissionGuard.allows('pedidos', 'ver')")
    public ApiResponse<ListResponse<PedidoResponse>> listOrders() { List<PedidoResponse> rows=pedidos.list(); return ApiResponse.ok("Pedidos cargados", new ListResponse<>(rows.size(), rows)); }
    @PostMapping("/pedidos")
    @PreAuthorize("@permissionGuard.allows('pedidos', 'crear')")
    public ApiResponse<ListResponse<PedidoResponse>> createOrder(@Valid @RequestBody PedidoForm form) { List<PedidoResponse> rows=pedidos.create(form); return ApiResponse.ok("Pedido registrado", new ListResponse<>(rows.size(), rows)); }
    @PutMapping("/pedidos/{id}")
    @PreAuthorize("@permissionGuard.allows('pedidos', 'editar')")
    public ApiResponse<ListResponse<PedidoResponse>> updateOrder(@PathVariable Long id, @Valid @RequestBody PedidoForm form) { List<PedidoResponse> rows=pedidos.update(id, form); return ApiResponse.ok("Pedido actualizado", new ListResponse<>(rows.size(), rows)); }
    @PatchMapping("/pedidos/{id}/estado")
    @PreAuthorize("@permissionGuard.allows('pedidos', 'cambiar_estado')")
    public ApiResponse<ListResponse<PedidoResponse>> stateOrder(@PathVariable Long id, @RequestParam String estado) { List<PedidoResponse> rows=pedidos.changeState(id, estado); return ApiResponse.ok("Estado del pedido actualizado", new ListResponse<>(rows.size(), rows)); }

    @GetMapping("/empaques")
    @PreAuthorize("@permissionGuard.allows('empaques', 'ver')")
    public ApiResponse<ListResponse<EmpaqueResponse>> listPackages() { List<EmpaqueResponse> rows=empaques.list(); return ApiResponse.ok("Empaques cargados", new ListResponse<>(rows.size(), rows)); }
    @PostMapping("/empaques")
    @PreAuthorize("@permissionGuard.allows('empaques', 'crear')")
    public ApiResponse<ListResponse<EmpaqueResponse>> createPackage(@Valid @RequestBody EmpaqueForm form) { List<EmpaqueResponse> rows=empaques.create(form); return ApiResponse.ok("Empaque preparado", new ListResponse<>(rows.size(), rows)); }
    @PatchMapping("/empaques/{id}/anular")
    @PreAuthorize("@permissionGuard.allows('empaques', 'anular')")
    public ApiResponse<ListResponse<EmpaqueResponse>> annulPackage(@PathVariable Long id) { List<EmpaqueResponse> rows=empaques.annul(id); return ApiResponse.ok("Empaque anulado", new ListResponse<>(rows.size(), rows)); }

    @GetMapping("/cargas-despacho")
    @PreAuthorize("@permissionGuard.allows('despacho', 'ver')")
    public ApiResponse<ListResponse<CargaDespachoResponse>> listTrailerLoads() { List<CargaDespachoResponse> rows=cargasDespacho.list(); return ApiResponse.ok("Cargas de tráiler cargadas", new ListResponse<>(rows.size(), rows)); }
    @PostMapping("/cargas-despacho")
    @PreAuthorize("@permissionGuard.allows('despacho', 'crear')")
    public ApiResponse<ListResponse<CargaDespachoResponse>> createTrailerLoad(@Valid @RequestBody CargaDespachoForm form) { List<CargaDespachoResponse> rows=cargasDespacho.create(form); return ApiResponse.ok("Carga de tráiler preparada", new ListResponse<>(rows.size(), rows)); }
    @PostMapping("/cargas-despacho/{id}/lineas")
    @PreAuthorize("@permissionGuard.allows('despacho', 'editar')")
    public ApiResponse<ListResponse<CargaDespachoResponse>> addTrailerLoadLine(@PathVariable Long id, @Valid @RequestBody CargaDespachoLineaForm form) { List<CargaDespachoResponse> rows=cargasDespacho.addLine(id, form.despachoId()); return ApiResponse.ok("Línea agregada a la carga", new ListResponse<>(rows.size(), rows)); }
    @DeleteMapping("/cargas-despacho/{id}/lineas/{despachoId}")
    @PreAuthorize("@permissionGuard.allows('despacho', 'editar')")
    public ApiResponse<ListResponse<CargaDespachoResponse>> removeTrailerLoadLine(@PathVariable Long id, @PathVariable Long despachoId) { List<CargaDespachoResponse> rows=cargasDespacho.removeLine(id, despachoId); return ApiResponse.ok("Línea retirada de la carga", new ListResponse<>(rows.size(), rows)); }
    @PatchMapping("/cargas-despacho/{id}/estado")
    @PreAuthorize("@permissionGuard.allows('despacho', 'cambiar_estado')")
    public ApiResponse<ListResponse<CargaDespachoResponse>> changeTrailerLoadState(@PathVariable Long id, @RequestParam String estado) { List<CargaDespachoResponse> rows=cargasDespacho.changeState(id, estado); return ApiResponse.ok("Estado de carga actualizado", new ListResponse<>(rows.size(), rows)); }
}
