package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.dto.ClasificacionForm;
import com.keraune.vlvblueberrysystem.entity.Cama;
import com.keraune.vlvblueberrysystem.entity.Clasificacion;
import com.keraune.vlvblueberrysystem.entity.Lote;
import com.keraune.vlvblueberrysystem.entity.User;
import com.keraune.vlvblueberrysystem.repository.CamaRepository;
import com.keraune.vlvblueberrysystem.repository.ClasificacionRepository;
import com.keraune.vlvblueberrysystem.repository.LoteRepository;
import com.keraune.vlvblueberrysystem.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClasificacionService {

    private final ClasificacionRepository clasificacionRepository;
    private final LoteRepository loteRepository;
    private final CamaRepository camaRepository;
    private final UserRepository userRepository;

    public ClasificacionService(ClasificacionRepository clasificacionRepository, LoteRepository loteRepository,
                                CamaRepository camaRepository, UserRepository userRepository) {
        this.clasificacionRepository = clasificacionRepository;
        this.loteRepository = loteRepository;
        this.camaRepository = camaRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Clasificacion> listarTodas() {
        return clasificacionRepository.findAllByOrderByFechaClasificacionDescIdDesc();
    }

    @Transactional
    public void crearClasificacion(ClasificacionForm form, String username) {
        Lote lote = obtenerLote(form.getLoteId());
        Cama cama = obtenerCamaValida(form.getCamaId(), lote.getId());
        User usuario = obtenerUsuario(username);

        Clasificacion clasificacion = new Clasificacion();
        clasificacion.setLote(lote);
        clasificacion.setCama(cama);
        clasificacion.setFechaClasificacion(form.getFechaClasificacion());
        clasificacion.setEstadoPlanta(normalizar(form.getEstadoPlanta()));
        clasificacion.setTamano(normalizar(form.getTamano()));
        clasificacion.setCondicion(normalizar(form.getCondicion()));
        clasificacion.setCantidad(form.getCantidad());
        clasificacion.setObservacion(normalizarOpcional(form.getObservacion()));
        clasificacion.setEstado(normalizarEstado(form.getEstado(), "PENDIENTE"));
        clasificacion.setUsuarioRegistro(usuario);
        clasificacionRepository.save(clasificacion);
    }

    @Transactional
    public void cambiarEstado(Long id, String nuevoEstado) {
        Clasificacion clasificacion = clasificacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la clasificación solicitada."));
        clasificacion.setEstado(normalizarEstado(nuevoEstado, "PENDIENTE"));
        clasificacionRepository.save(clasificacion);
    }

    public ClasificacionForm crearFormularioInicial() {
        ClasificacionForm form = new ClasificacionForm();
        form.setFechaClasificacion(LocalDate.now());
        form.setEstado("PENDIENTE");
        return form;
    }

    private Lote obtenerLote(Long loteId) {
        return loteRepository.findById(loteId)
                .orElseThrow(() -> new IllegalArgumentException("El invernadero seleccionado no existe."));
    }

    private Cama obtenerCamaValida(Long camaId, Long loteId) {
        Cama cama = camaRepository.findById(camaId)
                .orElseThrow(() -> new IllegalArgumentException("La cama seleccionada no existe."));
        if (cama.getLote() == null || !cama.getLote().getId().equals(loteId)) {
            throw new IllegalArgumentException("La cama seleccionada no pertenece al invernadero elegido.");
        }
        return cama;
    }

    private User obtenerUsuario(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario autenticado."));
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private String normalizarOpcional(String valor) {
        String texto = normalizar(valor);
        return (texto == null || texto.isBlank()) ? null : texto;
    }

    private String normalizarEstado(String estado, String estadoPorDefecto) {
        String valor = normalizar(estado);
        return (valor == null || valor.isBlank()) ? estadoPorDefecto : valor.toUpperCase();
    }
}
