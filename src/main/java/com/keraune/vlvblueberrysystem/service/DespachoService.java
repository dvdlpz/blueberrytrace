package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.dto.DespachoForm;
import com.keraune.vlvblueberrysystem.entity.Despacho;
import com.keraune.vlvblueberrysystem.entity.Lote;
import com.keraune.vlvblueberrysystem.entity.User;
import com.keraune.vlvblueberrysystem.repository.DespachoRepository;
import com.keraune.vlvblueberrysystem.repository.LoteRepository;
import com.keraune.vlvblueberrysystem.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DespachoService {

    private final DespachoRepository despachoRepository;
    private final LoteRepository loteRepository;
    private final UserRepository userRepository;

    public DespachoService(DespachoRepository despachoRepository, LoteRepository loteRepository,
                           UserRepository userRepository) {
        this.despachoRepository = despachoRepository;
        this.loteRepository = loteRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Despacho> listarTodos() {
        return despachoRepository.findAllByOrderByFechaDespachoDescIdDesc();
    }

    @Transactional
    public void crearDespacho(DespachoForm form, String username) {
        Lote lote = loteRepository.findById(form.getLoteId())
                .orElseThrow(() -> new IllegalArgumentException("El invernadero seleccionado no existe."));
        User usuario = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario autenticado."));

        Despacho despacho = new Despacho();
        despacho.setLote(lote);
        despacho.setFechaDespacho(form.getFechaDespacho());
        despacho.setModalidad(normalizar(form.getModalidad()));
        despacho.setCantidadDespachada(form.getCantidadDespachada());
        despacho.setDestino(normalizarOpcional(form.getDestino()));
        despacho.setGuiaRemision(normalizarOpcional(form.getGuiaRemision()));
        despacho.setValidacionCalidad(normalizar(form.getValidacionCalidad()));
        despacho.setObservacion(normalizarOpcional(form.getObservacion()));
        despacho.setEstado(normalizarEstado(form.getEstado(), "REGISTRADO"));
        despacho.setUsuarioRegistro(usuario);
        despachoRepository.save(despacho);
    }

    @Transactional
    public void cambiarEstado(Long id, String nuevoEstado) {
        Despacho despacho = despachoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el despacho solicitado."));
        despacho.setEstado(normalizarEstado(nuevoEstado, "REGISTRADO"));
        despachoRepository.save(despacho);
    }

    public DespachoForm crearFormularioInicial() {
        DespachoForm form = new DespachoForm();
        form.setFechaDespacho(LocalDate.now());
        form.setEstado("REGISTRADO");
        form.setModalidad("JABAS");
        form.setValidacionCalidad("APROBADO");
        return form;
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
