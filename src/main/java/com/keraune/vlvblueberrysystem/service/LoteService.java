package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.dto.LoteForm;
import com.keraune.vlvblueberrysystem.entity.Lote;
import com.keraune.vlvblueberrysystem.entity.User;
import com.keraune.vlvblueberrysystem.repository.LoteRepository;
import com.keraune.vlvblueberrysystem.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoteService {

    private final LoteRepository loteRepository;
    private final UserRepository userRepository;

    public LoteService(LoteRepository loteRepository, UserRepository userRepository) {
        this.loteRepository = loteRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Lote> listarTodos() {
        return loteRepository.findAllByOrderByFechaRegistroDescIdDesc();
    }

    @Transactional(readOnly = true)
    public Lote obtenerPorId(Long id) {
        return loteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el invernadero solicitado."));
    }

    @Transactional
    public void crearLote(LoteForm form, String username) {
        String codigoNormalizado = normalizar(form.getCodigo()).toUpperCase();
        validarCodigoDuplicado(codigoNormalizado, null);

        User usuario = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario autenticado."));

        Lote lote = new Lote();
        aplicarFormulario(lote, form, codigoNormalizado);
        lote.setUsuarioRegistro(usuario);
        loteRepository.save(lote);
    }

    @Transactional
    public void actualizarLote(Long id, LoteForm form) {
        Lote lote = obtenerPorId(id);
        String codigoNormalizado = normalizar(form.getCodigo()).toUpperCase();
        validarCodigoDuplicado(codigoNormalizado, id);
        aplicarFormulario(lote, form, codigoNormalizado);
        loteRepository.save(lote);
    }

    @Transactional
    public void cambiarEstado(Long id) {
        Lote lote = obtenerPorId(id);
        if ("ACTIVO".equalsIgnoreCase(lote.getEstado())) {
            lote.setEstado("INACTIVO");
        } else {
            lote.setEstado("ACTIVO");
        }
        loteRepository.save(lote);
    }

    @Transactional
    public void eliminarLogicamente(Long id) {
        Lote lote = obtenerPorId(id);
        lote.setEstado("ELIMINADO");
        loteRepository.save(lote);
    }

    @Transactional(readOnly = true)
    public LoteForm construirFormularioDesdeEntidad(Long id) {
        Lote lote = obtenerPorId(id);
        LoteForm form = new LoteForm();
        form.setCodigo(lote.getCodigo());
        form.setDescripcion(lote.getDescripcion());
        form.setCultivo(lote.getCultivo());
        form.setVariedad(lote.getVariedad());
        form.setFechaRegistro(lote.getFechaRegistro());
        form.setObservacion(lote.getObservacion());
        form.setEstado(lote.getEstado());
        return form;
    }

    private void aplicarFormulario(Lote lote, LoteForm form, String codigoNormalizado) {
        lote.setCodigo(codigoNormalizado);
        lote.setDescripcion(normalizar(form.getDescripcion()));
        lote.setCultivo(normalizar(form.getCultivo()));
        lote.setVariedad(normalizar(form.getVariedad()));
        lote.setFechaRegistro(form.getFechaRegistro());
        lote.setObservacion(normalizarOpcional(form.getObservacion()));
        lote.setEstado(normalizarEstado(form.getEstado()));
    }

    private void validarCodigoDuplicado(String codigo, Long loteIdActual) {
        loteRepository.findByCodigo(codigo).ifPresent(lote -> {
            boolean esMismoRegistro = loteIdActual != null && lote.getId().equals(loteIdActual);
            if (!esMismoRegistro) {
                throw new IllegalArgumentException("Ya existe un invernadero con ese código.");
            }
        });
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private String normalizarOpcional(String valor) {
        String texto = normalizar(valor);
        return (texto == null || texto.isBlank()) ? null : texto;
    }

    private String normalizarEstado(String estado) {
        String valor = normalizar(estado);
        if (valor == null || valor.isBlank()) {
            return "ACTIVO";
        }
        return valor.toUpperCase();
    }
}
