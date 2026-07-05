package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.dto.DashboardSummary;
import com.keraune.vlvblueberrysystem.entity.Despacho;
import com.keraune.vlvblueberrysystem.entity.LoteTrazable;
import com.keraune.vlvblueberrysystem.entity.Siembra;
import com.keraune.vlvblueberrysystem.repository.CamaRepository;
import com.keraune.vlvblueberrysystem.repository.ClasificacionRepository;
import com.keraune.vlvblueberrysystem.repository.DespachoRepository;
import com.keraune.vlvblueberrysystem.repository.FormalizacionRepository;
import com.keraune.vlvblueberrysystem.repository.LoteRepository;
import com.keraune.vlvblueberrysystem.repository.SiembraRepository;
import com.keraune.vlvblueberrysystem.repository.UniformizacionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardMetricsServiceTest {
    @Test
    void excludesHistoricalMovementsWithoutTraceabilityFromCurrentIndicators() {
        LoteRepository lotes = mock(LoteRepository.class);
        CamaRepository camas = mock(CamaRepository.class);
        SiembraRepository siembras = mock(SiembraRepository.class);
        UniformizacionRepository uniformizaciones = mock(UniformizacionRepository.class);
        FormalizacionRepository formalizaciones = mock(FormalizacionRepository.class);
        ClasificacionRepository clasificaciones = mock(ClasificacionRepository.class);
        DespachoRepository despachos = mock(DespachoRepository.class);

        LoteTrazable trace = mock(LoteTrazable.class);
        Siembra traceableSiembra = new Siembra();
        traceableSiembra.setLoteTrazable(trace);
        traceableSiembra.setEstado("REGISTRADA");
        traceableSiembra.setCantidadRegistrada(490);

        Despacho historicalDispatch = new Despacho();
        historicalDispatch.setEstado("DESPACHADO");
        historicalDispatch.setCantidadDespachada(1600);

        when(lotes.count()).thenReturn(0L);
        when(lotes.countByEstadoIgnoreCase("ACTIVO")).thenReturn(0L);
        when(camas.count()).thenReturn(0L);
        when(camas.countByEstadoIgnoreCase("ACTIVA")).thenReturn(0L);
        when(camas.findAll()).thenReturn(List.of());
        when(siembras.findAll()).thenReturn(List.of(traceableSiembra));
        when(uniformizaciones.findAll()).thenReturn(List.of());
        when(formalizaciones.findAll()).thenReturn(List.of());
        when(clasificaciones.findAll()).thenReturn(List.of());
        when(despachos.findAll()).thenReturn(List.of(historicalDispatch));

        DashboardSummary summary = new DashboardMetricsService(lotes, camas, siembras, uniformizaciones,
                formalizaciones, clasificaciones, despachos).summary();

        assertEquals(490, summary.plantasSembradas());
        assertEquals(0, summary.despachosRegistrados());
        assertEquals(0, summary.plantasDespachadas());
    }
}
