package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.dto.TrazabilidadRow;
import com.keraune.vlvblueberrysystem.entity.Lote;
import com.keraune.vlvblueberrysystem.repository.CamaRepository;
import com.keraune.vlvblueberrysystem.repository.ClasificacionRepository;
import com.keraune.vlvblueberrysystem.repository.DespachoRepository;
import com.keraune.vlvblueberrysystem.repository.FormalizacionRepository;
import com.keraune.vlvblueberrysystem.repository.LoteRepository;
import com.keraune.vlvblueberrysystem.repository.SiembraRepository;
import com.keraune.vlvblueberrysystem.repository.UniformizacionRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrazabilidadQueryService {

    private final LoteRepository loteRepository;
    private final CamaRepository camaRepository;
    private final SiembraRepository siembraRepository;
    private final UniformizacionRepository uniformizacionRepository;
    private final FormalizacionRepository formalizacionRepository;
    private final ClasificacionRepository clasificacionRepository;
    private final DespachoRepository despachoRepository;

    public TrazabilidadQueryService(LoteRepository loteRepository,
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
    public List<TrazabilidadRow> buscar(String codigo, String variedad, LocalDate fechaRegistro) {
        List<Lote> lotes = new ArrayList<>(loteRepository.findAllByOrderByFechaRegistroDescIdDesc());

        if (fechaRegistro != null) {
            lotes.removeIf(lote -> lote.getFechaRegistro() == null || !fechaRegistro.equals(lote.getFechaRegistro()));
        }

        if (variedad != null && !variedad.isBlank()) {
            String variedadNormalizada = normalizar(variedad);
            lotes.removeIf(lote -> lote.getVariedad() == null
                    || !normalizar(lote.getVariedad()).contains(variedadNormalizada));
        }

        if (codigo != null && !codigo.isBlank()) {
            String codigoNormalizado = normalizar(codigo).toUpperCase(Locale.ROOT);
            List<Lote> ordenadosPorCodigo = new ArrayList<>(lotes);
            ordenadosPorCodigo.sort(Comparator.comparing(
                    lote -> lote.getCodigo() == null ? "" : lote.getCodigo().toUpperCase(Locale.ROOT)));

            int index = busquedaBinariaPorCodigo(ordenadosPorCodigo, codigoNormalizado);
            if (index < 0) {
                return List.of();
            }

            String codigoEncontrado = ordenadosPorCodigo.get(index).getCodigo();
            lotes.removeIf(lote -> lote.getCodigo() == null || !lote.getCodigo().equalsIgnoreCase(codigoEncontrado));
        }

        return lotes.stream().map(this::construirFila).toList();
    }

    private TrazabilidadRow construirFila(Lote lote) {
        Long loteId = lote.getId();
        long siembras = siembraRepository.countByLoteId(loteId);
        long uniformizaciones = uniformizacionRepository.countByLoteId(loteId);
        long formalizaciones = formalizacionRepository.countByLoteId(loteId);
        long clasificaciones = clasificacionRepository.countByLoteId(loteId);
        long despachos = despachoRepository.countByLoteId(loteId);

        return new TrazabilidadRow(
                lote,
                camaRepository.countByLoteId(loteId),
                siembras,
                siembraRepository.sumCantidadRegistradaByLoteId(loteId),
                uniformizaciones,
                formalizaciones,
                clasificaciones,
                despachos,
                despachoRepository.sumCantidadDespachadaByLoteId(loteId),
                resolverUltimoEvento(siembras, uniformizaciones, formalizaciones, clasificaciones, despachos)
        );
    }

    private String resolverUltimoEvento(long siembras, long uniformizaciones, long formalizaciones,
                                        long clasificaciones, long despachos) {
        if (despachos > 0) return "Despacho registrado";
        if (clasificaciones > 0) return "Clasificación registrada";
        if (formalizaciones > 0) return "Formalización registrada";
        if (uniformizaciones > 0) return "Uniformización registrada";
        if (siembras > 0) return "Siembra registrada";
        return "Solo estructura base";
    }

    private int busquedaBinariaPorCodigo(List<Lote> lotes, String codigoBuscado) {
        int izquierda = 0;
        int derecha = lotes.size() - 1;

        while (izquierda <= derecha) {
            int medio = (izquierda + derecha) >>> 1;
            String codigoActual = lotes.get(medio).getCodigo() == null
                    ? ""
                    : lotes.get(medio).getCodigo().toUpperCase(Locale.ROOT);

            int comparacion = codigoActual.compareTo(codigoBuscado);
            if (comparacion == 0) {
                return medio;
            }
            if (comparacion < 0) {
                izquierda = medio + 1;
            } else {
                derecha = medio - 1;
            }
        }
        return -1;
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
    }
}
