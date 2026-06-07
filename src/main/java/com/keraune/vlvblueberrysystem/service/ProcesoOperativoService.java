package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.dto.FormalizacionForm;
import com.keraune.vlvblueberrysystem.dto.UniformizacionForm;
import com.keraune.vlvblueberrysystem.entity.Cama;
import com.keraune.vlvblueberrysystem.entity.Formalizacion;
import com.keraune.vlvblueberrysystem.entity.Lote;
import com.keraune.vlvblueberrysystem.entity.Uniformizacion;
import com.keraune.vlvblueberrysystem.entity.User;
import com.keraune.vlvblueberrysystem.repository.CamaRepository;
import com.keraune.vlvblueberrysystem.repository.FormalizacionRepository;
import com.keraune.vlvblueberrysystem.repository.LoteRepository;
import com.keraune.vlvblueberrysystem.repository.UniformizacionRepository;
import com.keraune.vlvblueberrysystem.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcesoOperativoService {

    private final UniformizacionRepository uniformizacionRepository;
    private final FormalizacionRepository formalizacionRepository;
    private final LoteRepository loteRepository;
    private final CamaRepository camaRepository;
    private final UserRepository userRepository;

    public ProcesoOperativoService(UniformizacionRepository uniformizacionRepository,
                                   FormalizacionRepository formalizacionRepository,
                                   LoteRepository loteRepository,
                                   CamaRepository camaRepository,
                                   UserRepository userRepository) {
        this.uniformizacionRepository = uniformizacionRepository;
        this.formalizacionRepository = formalizacionRepository;
        this.loteRepository = loteRepository;
        this.camaRepository = camaRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Uniformizacion> listarUniformizaciones() {
        return uniformizacionRepository.findAllByOrderByFechaUniformizacionDescIdDesc();
    }

    @Transactional(readOnly = true)
    public List<Formalizacion> listarFormalizaciones() {
        return formalizacionRepository.findAllByOrderByFechaFormalizacionDescIdDesc();
    }

    @Transactional
    public void crearUniformizacion(UniformizacionForm form, String username) {
        Lote lote = obtenerLote(form.getLoteId());
        Cama cama = obtenerCamaValida(form.getCamaId(), lote.getId());
        User usuario = obtenerUsuario(username);

        Uniformizacion uniformizacion = new Uniformizacion();
        uniformizacion.setLote(lote);
        uniformizacion.setCama(cama);
        uniformizacion.setFechaUniformizacion(form.getFechaUniformizacion());
        uniformizacion.setCriterio(normalizar(form.getCriterio()));
        uniformizacion.setCantidadInicial(form.getCantidadInicial());
        uniformizacion.setCantidadUniformizada(form.getCantidadUniformizada());
        uniformizacion.setObservacion(normalizarOpcional(form.getObservacion()));
        uniformizacion.setEstado(normalizarEstado(form.getEstado(), "REGISTRADA"));
        uniformizacion.setUsuarioRegistro(usuario);
        uniformizacionRepository.save(uniformizacion);
    }

    @Transactional
    public void crearFormalizacion(FormalizacionForm form, String username) {
        Lote lote = obtenerLote(form.getLoteId());
        Cama cama = obtenerCamaValida(form.getCamaId(), lote.getId());
        User usuario = obtenerUsuario(username);

        Formalizacion formalizacion = new Formalizacion();
        formalizacion.setLote(lote);
        formalizacion.setCama(cama);
        formalizacion.setFechaFormalizacion(form.getFechaFormalizacion());
        formalizacion.setDetalle(normalizar(form.getDetalle()));
        formalizacion.setCantidadBandejas(form.getCantidadBandejas());
        formalizacion.setCantidadPlantas(form.getCantidadPlantas());
        formalizacion.setObservacion(normalizarOpcional(form.getObservacion()));
        formalizacion.setEstado(normalizarEstado(form.getEstado(), "REGISTRADA"));
        formalizacion.setUsuarioRegistro(usuario);
        formalizacionRepository.save(formalizacion);
    }

    @Transactional
    public void cambiarEstadoUniformizacion(Long id) {
        Uniformizacion uniformizacion = uniformizacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la uniformización solicitada."));
        uniformizacion.setEstado("REGISTRADA".equalsIgnoreCase(uniformizacion.getEstado()) ? "ANULADA" : "REGISTRADA");
        uniformizacionRepository.save(uniformizacion);
    }

    @Transactional
    public void cambiarEstadoFormalizacion(Long id) {
        Formalizacion formalizacion = formalizacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la formalización solicitada."));
        formalizacion.setEstado("REGISTRADA".equalsIgnoreCase(formalizacion.getEstado()) ? "ANULADA" : "REGISTRADA");
        formalizacionRepository.save(formalizacion);
    }

    public UniformizacionForm crearFormularioUniformizacionInicial() {
        UniformizacionForm form = new UniformizacionForm();
        form.setFechaUniformizacion(LocalDate.now());
        form.setEstado("REGISTRADA");
        form.setCriterio("Tamaño y vigor de planta");
        return form;
    }

    public FormalizacionForm crearFormularioFormalizacionInicial() {
        FormalizacionForm form = new FormalizacionForm();
        form.setFechaFormalizacion(LocalDate.now());
        form.setEstado("REGISTRADA");
        form.setDetalle("Ordenamiento y formalización de bandejas");
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
