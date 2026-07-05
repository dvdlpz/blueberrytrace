package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.dto.DashboardSummary;
import com.keraune.vlvblueberrysystem.entity.Clasificacion;
import com.keraune.vlvblueberrysystem.entity.Despacho;
import com.keraune.vlvblueberrysystem.entity.Formalizacion;
import com.keraune.vlvblueberrysystem.entity.Siembra;
import com.keraune.vlvblueberrysystem.entity.Uniformizacion;
import com.keraune.vlvblueberrysystem.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Current indicators are calculated from traceable movements only. Historical
 * rows without a traceable lot remain available for review, but cannot alter
 * the stock shown to operational users.
 */
@Service
@Transactional(readOnly = true)
public class DashboardMetricsService {
    private final LoteRepository lotes;
    private final CamaRepository camas;
    private final SiembraRepository siembras;
    private final UniformizacionRepository uniformizaciones;
    private final FormalizacionRepository formalizaciones;
    private final ClasificacionRepository clasificaciones;
    private final DespachoRepository despachos;

    public DashboardMetricsService(LoteRepository lotes, CamaRepository camas, SiembraRepository siembras,
                                   UniformizacionRepository uniformizaciones, FormalizacionRepository formalizaciones,
                                   ClasificacionRepository clasificaciones, DespachoRepository despachos) {
        this.lotes = lotes;
        this.camas = camas;
        this.siembras = siembras;
        this.uniformizaciones = uniformizaciones;
        this.formalizaciones = formalizaciones;
        this.clasificaciones = clasificaciones;
        this.despachos = despachos;
    }

    public DashboardSummary summary() {
        List<Siembra> allSiembras = siembras.findAll();
        List<Uniformizacion> allUniformizaciones = uniformizaciones.findAll();
        List<Formalizacion> allFormalizaciones = formalizaciones.findAll();
        List<Clasificacion> allClasificaciones = clasificaciones.findAll();
        List<Despacho> allDespachos = despachos.findAll();

        long lotesRegistrados = lotes.count();
        long lotesActivos = lotes.countByEstadoIgnoreCase("ACTIVO");
        long camasRegistradas = camas.count();
        long camasActivas = camas.countByEstadoIgnoreCase("ACTIVA");
        long capacidad = camas.findAll().stream().filter(item -> is("ACTIVA", item.getEstado()))
                .mapToLong(item -> value(item.getCapacidadReferencial())).sum();

        long siembrasRegistradas = allSiembras.stream().filter(this::traceableRegistered).count();
        long plantasSembradas = allSiembras.stream().filter(this::traceableRegistered)
                .mapToLong(item -> value(item.getCantidadRegistrada())).sum();
        long uniformizacionesRegistradas = allUniformizaciones.stream().filter(this::traceableRegistered).count();
        long formalizacionesRegistradas = allFormalizaciones.stream().filter(this::traceableRegistered).count();
        long clasificacionesRegistradas = allClasificaciones.stream().filter(this::traceableClassification).count();
        long pendientes = allClasificaciones.stream().filter(item -> hasTrace(item) && is("PENDIENTE", item.getEstado())).count();
        long validadas = allClasificaciones.stream().filter(item -> hasTrace(item) && is("VALIDADA", item.getEstado())).count();
        long despachosRegistrados = allDespachos.stream().filter(this::traceableDispatched).count();
        long plantasDespachadas = allDespachos.stream().filter(this::traceableDispatched)
                .mapToLong(item -> value(item.getCantidadDespachada())).sum();

        return new DashboardSummary(
                lotesRegistrados, lotesActivos, Math.max(lotesRegistrados - lotesActivos, 0),
                camasRegistradas, camasActivas, Math.max(camasRegistradas - camasActivas, 0), capacidad,
                siembrasRegistradas, plantasSembradas, uniformizacionesRegistradas, formalizacionesRegistradas,
                clasificacionesRegistradas, pendientes, validadas, despachosRegistrados, plantasDespachadas,
                pct(lotesActivos, lotesRegistrados), pct(camasActivas, camasRegistradas), pct(validadas, clasificacionesRegistradas), pct(plantasDespachadas, plantasSembradas),
                pct(uniformizacionesRegistradas, siembrasRegistradas), pct(formalizacionesRegistradas, siembrasRegistradas), pct(clasificacionesRegistradas, siembrasRegistradas), pct(despachosRegistrados, siembrasRegistradas)
        );
    }

    private boolean hasTrace(Siembra item) { return item.getLoteTrazable() != null; }
    private boolean hasTrace(Uniformizacion item) { return item.getLoteTrazable() != null; }
    private boolean hasTrace(Formalizacion item) { return item.getLoteTrazable() != null; }
    private boolean hasTrace(Clasificacion item) { return item.getLoteTrazable() != null; }
    private boolean hasTrace(Despacho item) { return item.getLoteTrazable() != null && item.getClasificacion() != null; }
    private boolean traceableRegistered(Siembra item) { return hasTrace(item) && is("REGISTRADA", item.getEstado()); }
    private boolean traceableRegistered(Uniformizacion item) { return hasTrace(item) && is("REGISTRADA", item.getEstado()); }
    private boolean traceableRegistered(Formalizacion item) { return hasTrace(item) && is("REGISTRADA", item.getEstado()); }
    private boolean traceableClassification(Clasificacion item) { return hasTrace(item) && (is("PENDIENTE", item.getEstado()) || is("VALIDADA", item.getEstado())); }
    private boolean traceableDispatched(Despacho item) { return hasTrace(item) && is("DESPACHADO", item.getEstado()); }
    private boolean is(String expected, String state) { return state != null && expected.equals(state.trim().toUpperCase(Locale.ROOT)); }
    private long value(Integer value) { return value == null ? 0L : value.longValue(); }
    private double pct(long part, long total) { return total <= 0 ? 0 : Math.round((part * 10000.0 / total)) / 100.0; }
}
