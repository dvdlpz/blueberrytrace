package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.TrazabilidadResponse;
import com.keraune.vlvblueberrysystem.api.mapper.ApiRecordMapper;
import com.keraune.vlvblueberrysystem.dto.TrazabilidadRow;
import com.keraune.vlvblueberrysystem.entity.*;
import com.keraune.vlvblueberrysystem.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TrazabilidadQueryService {
    private final LoteRepository loteRepository;
    private final CamaRepository camaRepository;
    private final SiembraRepository siembraRepository;
    private final UniformizacionRepository uniformizacionRepository;
    private final FormalizacionRepository formalizacionRepository;
    private final ClasificacionRepository clasificacionRepository;
    private final DespachoRepository despachoRepository;
    private final ApiRecordMapper mapper;

    public TrazabilidadQueryService(LoteRepository loteRepository, CamaRepository camaRepository, SiembraRepository siembraRepository,
                                    UniformizacionRepository uniformizacionRepository, FormalizacionRepository formalizacionRepository,
                                    ClasificacionRepository clasificacionRepository, DespachoRepository despachoRepository, ApiRecordMapper mapper) {
        this.loteRepository = loteRepository;
        this.camaRepository = camaRepository;
        this.siembraRepository = siembraRepository;
        this.uniformizacionRepository = uniformizacionRepository;
        this.formalizacionRepository = formalizacionRepository;
        this.clasificacionRepository = clasificacionRepository;
        this.despachoRepository = despachoRepository;
        this.mapper = mapper;
    }

    public List<TrazabilidadResponse> list() {
        List<Lote> lotes = loteRepository.findAllByOrderByFechaRegistroDescIdDesc();
        Map<Long, List<Cama>> camas = groupByLote(camaRepository.findAll(), cama -> cama.getLote() == null ? null : cama.getLote().getId());
        Map<Long, List<Siembra>> siembras = groupByLote(siembraRepository.findAll().stream().filter(item -> item.getLoteTrazable() == null).toList(), siembra -> siembra.getLote() == null ? null : siembra.getLote().getId());
        Map<Long, List<Uniformizacion>> uniformizaciones = groupByLote(uniformizacionRepository.findAll().stream().filter(item -> item.getLoteTrazable() == null).toList(), item -> item.getLote() == null ? null : item.getLote().getId());
        Map<Long, List<Formalizacion>> formalizaciones = groupByLote(formalizacionRepository.findAll().stream().filter(item -> item.getLoteTrazable() == null).toList(), item -> item.getLote() == null ? null : item.getLote().getId());
        Map<Long, List<Clasificacion>> clasificaciones = groupByLote(clasificacionRepository.findAll().stream().filter(item -> item.getLoteTrazable() == null).toList(), item -> item.getLote() == null ? null : item.getLote().getId());
        Map<Long, List<Despacho>> despachos = groupByLote(despachoRepository.findAll().stream().filter(item -> item.getLoteTrazable() == null).toList(), item -> item.getLote() == null ? null : item.getLote().getId());

        return lotes.stream()
                .map(lote -> rowFor(
                        lote,
                        valueOf(camas, lote.getId()),
                        valueOf(siembras, lote.getId()),
                        valueOf(uniformizaciones, lote.getId()),
                        valueOf(formalizaciones, lote.getId()),
                        valueOf(clasificaciones, lote.getId()),
                        valueOf(despachos, lote.getId())
                ))
                .map(mapper::trazabilidad)
                .toList();
    }

    private <T> Map<Long, List<T>> groupByLote(List<T> items, Function<T, Long> loteIdOf) {
        return items.stream()
                .filter(item -> loteIdOf.apply(item) != null)
                .collect(Collectors.groupingBy(loteIdOf));
    }

    private <T> List<T> valueOf(Map<Long, List<T>> grouped, Long loteId) {
        return grouped.getOrDefault(loteId, List.of());
    }

    private TrazabilidadRow rowFor(Lote lote, List<Cama> camas, List<Siembra> siembras,
                                   List<Uniformizacion> uniformizaciones, List<Formalizacion> formalizaciones,
                                   List<Clasificacion> clasificaciones, List<Despacho> despachos) {
        List<Siembra> siembrasEfectivas = siembras.stream().filter(item -> is("REGISTRADA", item.getEstado())).toList();
        List<Uniformizacion> uniformizacionesEfectivas = uniformizaciones.stream().filter(item -> is("REGISTRADA", item.getEstado())).toList();
        List<Formalizacion> formalizacionesEfectivas = formalizaciones.stream().filter(item -> is("REGISTRADA", item.getEstado())).toList();
        List<Clasificacion> clasificacionesEfectivas = clasificaciones.stream().filter(item -> classificationActive(item.getEstado())).toList();
        List<Despacho> despachosEfectivos = despachos.stream().filter(item -> is("DESPACHADO", item.getEstado())).toList();
        long plantasSembradas = siembrasEfectivas.stream().mapToLong(s -> s.getCantidadRegistrada() == null ? 0 : s.getCantidadRegistrada()).sum();
        long plantasDespachadas = despachosEfectivos.stream().mapToLong(d -> d.getCantidadDespachada() == null ? 0 : d.getCantidadDespachada()).sum();
        String ultimoEvento = latestEvent(
                siembrasEfectivas.stream().map(s -> event(s.getFechaSiembra(), "Siembra registrada")).toList(),
                uniformizacionesEfectivas.stream().map(u -> event(u.getFechaUniformizacion(), "Uniformización registrada")).toList(),
                formalizacionesEfectivas.stream().map(f -> event(f.getFechaFormalizacion(), "Formalización registrada")).toList(),
                clasificacionesEfectivas.stream().map(c -> event(c.getFechaClasificacion(), "Clasificación " + c.getEstado())).toList(),
                despachosEfectivos.stream().map(d -> event(d.getFechaDespacho(), "Despacho " + d.getEstado())).toList()
        );
        return new TrazabilidadRow(lote.getId(), lote.getId(), lote.getCodigo(), lote.getDescripcion(), camas.size(), siembrasEfectivas.size(), plantasSembradas,
                uniformizacionesEfectivas.size(), formalizacionesEfectivas.size(), clasificacionesEfectivas.size(), despachosEfectivos.size(), plantasDespachadas, ultimoEvento);
    }

    @SafeVarargs
    private String latestEvent(List<EventMarker>... markers) {
        return List.of(markers).stream().flatMap(List::stream)
                .filter(marker -> marker.date() != null)
                .max(Comparator.comparing(EventMarker::date))
                .map(marker -> marker.label() + " · " + marker.date())
                .orElse("Sin eventos operativos");
    }

    private boolean classificationActive(String state) {
        return is("PENDIENTE", state) || is("VALIDADA", state) || is("OBSERVADA", state);
    }

    private boolean is(String expected, String state) {
        return state != null && expected.equals(state.trim().toUpperCase(Locale.ROOT));
    }

    private EventMarker event(LocalDate date, String label) {
        return new EventMarker(date, label);
    }

    private record EventMarker(LocalDate date, String label) {}
}
