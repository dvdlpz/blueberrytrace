package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.OperationReadinessResponse;
import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.OperationReadinessStepResponse;
import com.keraune.vlvblueberrysystem.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OperationReadinessService {
    private final LoteRepository lotes;
    private final CamaRepository camas;
    private final JabaRepository jabas;
    private final LoteTrazableRepository trazables;
    private final SiembraRepository siembras;
    private final RiegoProgramadoRepository riegos;
    private final UniformizacionRepository uniformizaciones;
    private final FormalizacionRepository formalizaciones;
    private final RecuperacionRiegoRepository recuperaciones;
    private final ClasificacionRepository clasificaciones;
    private final PedidoRepository pedidos;
    private final EmpaqueRepository empaques;
    private final DespachoRepository despachos;

    public OperationReadinessService(LoteRepository lotes, CamaRepository camas, JabaRepository jabas, LoteTrazableRepository trazables,
                                     SiembraRepository siembras, RiegoProgramadoRepository riegos, UniformizacionRepository uniformizaciones, FormalizacionRepository formalizaciones,
                                     RecuperacionRiegoRepository recuperaciones, ClasificacionRepository clasificaciones, PedidoRepository pedidos,
                                     EmpaqueRepository empaques, DespachoRepository despachos) {
        this.lotes=lotes; this.camas=camas; this.jabas=jabas; this.trazables=trazables; this.siembras=siembras; this.riegos=riegos; this.uniformizaciones=uniformizaciones;
        this.formalizaciones=formalizaciones; this.recuperaciones=recuperaciones; this.clasificaciones=clasificaciones; this.pedidos=pedidos; this.empaques=empaques; this.despachos=despachos;
    }

    public OperationReadinessResponse readiness() {
        long activeLots=lotes.countByEstadoIgnoreCase("ACTIVO");
        long activeBeds=camas.countByEstadoIgnoreCase("ACTIVA");
        long activeCrates=jabas.findAll().stream().filter(item -> "ACTIVA".equalsIgnoreCase(item.getEstado())).count();
        long activeTraces=trazables.findByEstadoIgnoreCaseOrderByCodigoAsc("ACTIVO").size();
        long sowed=siembras.findAll().stream().filter(item -> item.getLoteTrazable()!=null && "REGISTRADA".equalsIgnoreCase(item.getEstado())).count();
        long scheduledIrrigation=riegos.findAll().stream().filter(item -> "PROGRAMADO".equalsIgnoreCase(item.getEstado()) || "REALIZADO".equalsIgnoreCase(item.getEstado())).count();
        long uniform=uniformizaciones.findAll().stream().filter(item -> item.getLoteTrazable()!=null && "REGISTRADA".equalsIgnoreCase(item.getEstado())).count();
        long formal=formalizaciones.findAll().stream().filter(item -> item.getLoteTrazable()!=null && "REGISTRADA".equalsIgnoreCase(item.getEstado())).count();
        long recovery=recuperaciones.findAll().stream().filter(item -> "EN_RIEGO".equalsIgnoreCase(item.getEstado())).count();
        long validated=clasificaciones.findAll().stream().filter(item -> item.getLoteTrazable()!=null && "VALIDADA".equalsIgnoreCase(item.getEstado())).count();
        long activeOrders=pedidos.findAll().stream().filter(item -> !"CANCELADO".equalsIgnoreCase(item.getEstado()) && !"COMPLETADO".equalsIgnoreCase(item.getEstado())).count();
        long prepared=empaques.findAll().stream().filter(item -> "PREPARADO".equalsIgnoreCase(item.getEstado()) || "PARCIAL".equalsIgnoreCase(item.getEstado())).count();
        long sent=despachos.findAll().stream().filter(item -> item.getLoteTrazable()!=null && item.getEmpaque()!=null && "DESPACHADO".equalsIgnoreCase(item.getEstado())).count();

        List<OperationReadinessStepResponse> steps=List.of(
                step("lotes", "Registrar invernaderos", "Crea el espacio físico donde se organizarán las camas.", "Registrar invernadero", true, activeLots>0, activeLots),
                step("camas", "Organizar camas", "Registra las filas productivas dentro del invernadero.", "Registrar cama", activeLots>0, activeBeds>0, activeBeds),
                step("jabas", "Registrar jabas", "Define las jabas y su capacidad de macetas dentro de cada cama.", "Registrar jaba", activeBeds>0, activeCrates>0, activeCrates),
                step("lotes-trazables", "Crear lote trazable", "Registra la variedad y procedencia del grupo de plantas.", "Crear lote trazable", activeCrates>0, activeTraces>0, activeTraces),
                step("siembra", "Registrar siembra", "Ubica macetas sembradas dentro de una jaba activa.", "Registrar siembra", activeTraces>0 && activeCrates>0, sowed>0, sowed),
                step("riegos", "Programar riego", "Organiza los riegos de crecimiento o recuperación por cama y jaba.", "Programar riego", sowed>0, scheduledIrrigation>0, scheduledIrrigation),
                step("uniformizaciones", "Uniformizar macetas", "Agrupa plantas por tamaño y registra limpieza de malezas después del riego de crecimiento.", "Registrar uniformización", scheduledIrrigation>0, uniform>0, uniform),
                step("formalizaciones", "Formalizar jabas", "Organiza jabas completas por tamaño para preparar la clasificación.", "Registrar formalización", uniform>0, formal>0, formal),
                step("recuperacion", "Gestionar recuperación", "Usa este paso solo si se detectan plantas secas; programa y confirma el riego antes de cerrarlo.", "Gestionar recuperación", uniform>0 || formal>0, recovery==0, recovery),
                step("clasificacion", "Clasificar plantas", "Valida plantas aptas, húmedas y listas para cumplir pedidos.", "Registrar clasificación", formal>0, validated>0, validated),
                step("pedidos", "Registrar pedido", "Define las cantidades solicitadas por variedad y destino.", "Registrar pedido", validated>0, activeOrders>0, activeOrders),
                step("empaques", "Preparar empaques", "Forma jabas cosecheras de 15 macetas o bins de madera con capacidad registrada.", "Preparar empaque", validated>0 && activeOrders>0, prepared>0, prepared),
                step("despacho", "Cargar y despachar tráiler", "Consolida líneas por variedad del mismo pedido, marca el tráiler cargado y confirma su salida.", "Gestionar cargas", prepared>0, sent>0, sent)
        );
        OperationReadinessStepResponse recommended=steps.stream().filter(step -> step.available() && !step.completed()).findFirst().orElseGet(() -> steps.stream().filter(OperationReadinessStepResponse::available).reduce((first,second)->second).orElse(steps.getFirst()));
        return new OperationReadinessResponse("Preparación operativa", "Sigue el recorrido real: cama, jaba, maceta, recuperación, empaque y despacho por variedad.", recommended.key(), recommended.actionLabel(), steps);
    }
    private OperationReadinessStepResponse step(String key,String title,String description,String action,boolean available,boolean completed,long total){return new OperationReadinessStepResponse(key,title,description,action,available,completed,total);}
}
