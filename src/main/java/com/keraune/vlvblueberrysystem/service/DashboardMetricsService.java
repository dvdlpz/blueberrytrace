package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.dto.DashboardSummary;
import com.keraune.vlvblueberrysystem.repository.CamaRepository;
import com.keraune.vlvblueberrysystem.repository.ClasificacionRepository;
import com.keraune.vlvblueberrysystem.repository.DespachoRepository;
import com.keraune.vlvblueberrysystem.repository.FormalizacionRepository;
import com.keraune.vlvblueberrysystem.repository.LoteRepository;
import com.keraune.vlvblueberrysystem.repository.SiembraRepository;
import com.keraune.vlvblueberrysystem.repository.UniformizacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardMetricsService {

    private final LoteRepository loteRepository;
    private final CamaRepository camaRepository;
    private final SiembraRepository siembraRepository;
    private final UniformizacionRepository uniformizacionRepository;
    private final FormalizacionRepository formalizacionRepository;
    private final ClasificacionRepository clasificacionRepository;
    private final DespachoRepository despachoRepository;

    public DashboardMetricsService(LoteRepository loteRepository,
                                   CamaRepository camaRepository,
                                   SiembraRepository siembraRepository,
                                   UniformizacionRepository uniformizacionRepository,
                                   FormalizacionRepository formalizacionRepository,
                                   ClasificacionRepository clasificacionRepository,
                                   DespachoRepository despachoRepository) {
        this.loteRepository = loteRepository;
        this.camaRepository = camaRepository;
        this.siembraRepository = siembraRepository;
        this.uniformizacionRepository = uniformizacionRepository;
        this.formalizacionRepository = formalizacionRepository;
        this.clasificacionRepository = clasificacionRepository;
        this.despachoRepository = despachoRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummary obtenerResumen() {
        long lotesRegistrados = loteRepository.count();
        long lotesActivos = loteRepository.countByEstadoIgnoreCase("ACTIVO");
        long camasRegistradas = camaRepository.count();
        long camasActivas = camaRepository.countByEstadoIgnoreCase("ACTIVA");
        long capacidadReferencialTotal = camaRepository.sumCapacidadReferencial();

        long siembrasRegistradas = siembraRepository.count();
        long plantasSembradas = siembraRepository.sumCantidadRegistrada();
        long uniformizacionesRegistradas = uniformizacionRepository.count();
        long formalizacionesRegistradas = formalizacionRepository.count();
        long clasificacionesRegistradas = clasificacionRepository.count();
        long clasificacionesPendientes = clasificacionRepository.countByEstadoIgnoreCase("PENDIENTE");
        long clasificacionesValidadas = clasificacionRepository.countByEstadoIgnoreCase("VALIDADA");
        long despachosRegistrados = despachoRepository.count();
        long plantasDespachadas = despachoRepository.sumCantidadDespachada();

        return new DashboardSummary(
                lotesRegistrados,
                lotesActivos,
                Math.max(0, lotesRegistrados - lotesActivos),
                camasRegistradas,
                camasActivas,
                Math.max(0, camasRegistradas - camasActivas),
                capacidadReferencialTotal,
                siembrasRegistradas,
                plantasSembradas,
                uniformizacionesRegistradas,
                formalizacionesRegistradas,
                clasificacionesRegistradas,
                clasificacionesPendientes,
                clasificacionesValidadas,
                despachosRegistrados,
                plantasDespachadas,
                calcularPorcentaje(lotesActivos, lotesRegistrados),
                calcularPorcentaje(camasActivas, camasRegistradas),
                calcularPorcentaje(clasificacionesValidadas, clasificacionesRegistradas),
                calcularPorcentaje(plantasDespachadas, plantasSembradas),
                calcularPorcentaje(uniformizacionesRegistradas, siembrasRegistradas),
                calcularPorcentaje(formalizacionesRegistradas, siembrasRegistradas),
                calcularPorcentaje(clasificacionesRegistradas, siembrasRegistradas),
                calcularPorcentaje(despachosRegistrados, siembrasRegistradas)
        );
    }

    private int calcularPorcentaje(long parte, long total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round((parte * 100.0) / total);
    }
}
