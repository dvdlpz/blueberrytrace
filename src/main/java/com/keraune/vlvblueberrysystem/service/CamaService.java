package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.dto.CamaForm;
import com.keraune.vlvblueberrysystem.entity.Cama;
import com.keraune.vlvblueberrysystem.entity.Lote;
import com.keraune.vlvblueberrysystem.entity.User;
import com.keraune.vlvblueberrysystem.repository.CamaRepository;
import com.keraune.vlvblueberrysystem.repository.LoteRepository;
import com.keraune.vlvblueberrysystem.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CamaService {

    private final CamaRepository camaRepository;
    private final LoteRepository loteRepository;
    private final UserRepository userRepository;

    public CamaService(CamaRepository camaRepository, LoteRepository loteRepository, UserRepository userRepository) {
        this.camaRepository = camaRepository;
        this.loteRepository = loteRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Cama> listarTodas() {
        return camaRepository.findAllByOrderByCodigoAsc();
    }

    @Transactional(readOnly = true)
    public Cama obtenerPorId(Long id) {
        return camaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la cama solicitada."));
    }

    @Transactional
    public void crearCama(CamaForm form, String username) {
        String codigoNormalizado = normalizar(form.getCodigo());
        validarCodigoDuplicado(codigoNormalizado, null);

        Lote lote = loteRepository.findById(form.getLoteId())
                .orElseThrow(() -> new IllegalArgumentException("El lote seleccionado no existe."));

        User usuario = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario autenticado."));

        Cama cama = new Cama();
        cama.setCodigo(codigoNormalizado);
        cama.setDescripcion(normalizar(form.getDescripcion()));
        cama.setCapacidadReferencial(form.getCapacidadReferencial());
        cama.setEstado(normalizarEstado(form.getEstado()));
        cama.setLote(lote);
        cama.setUsuarioRegistro(usuario);

        camaRepository.save(cama);
    }

    @Transactional
    public void actualizarCama(Long id, CamaForm form) {
        Cama cama = obtenerPorId(id);
        String codigoNormalizado = normalizar(form.getCodigo());
        validarCodigoDuplicado(codigoNormalizado, id);

        Lote lote = loteRepository.findById(form.getLoteId())
                .orElseThrow(() -> new IllegalArgumentException("El lote seleccionado no existe."));

        cama.setCodigo(codigoNormalizado);
        cama.setDescripcion(normalizar(form.getDescripcion()));
        cama.setCapacidadReferencial(form.getCapacidadReferencial());
        cama.setEstado(normalizarEstado(form.getEstado()));
        cama.setLote(lote);

        camaRepository.save(cama);
    }

    @Transactional
    public void cambiarEstado(Long id) {
        Cama cama = obtenerPorId(id);
        if ("ACTIVA".equalsIgnoreCase(cama.getEstado())) {
            cama.setEstado("INACTIVA");
        } else {
            cama.setEstado("ACTIVA");
        }
        camaRepository.save(cama);
    }

    @Transactional(readOnly = true)
    public CamaForm construirFormularioDesdeEntidad(Long id) {
        Cama cama = obtenerPorId(id);
        CamaForm form = new CamaForm();
        form.setCodigo(cama.getCodigo());
        form.setDescripcion(cama.getDescripcion());
        form.setCapacidadReferencial(cama.getCapacidadReferencial());
        form.setEstado(cama.getEstado());
        form.setLoteId(cama.getLote().getId());
        return form;
    }

    private void validarCodigoDuplicado(String codigo, Long camaIdActual) {
        camaRepository.findByCodigo(codigo).ifPresent(cama -> {
            boolean esMismoRegistro = camaIdActual != null && cama.getId().equals(camaIdActual);
            if (!esMismoRegistro) {
                throw new IllegalArgumentException("Ya existe una cama con ese código.");
            }
        });
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private String normalizarEstado(String estado) {
        String valor = normalizar(estado);
        if (valor == null || valor.isBlank()) {
            return "ACTIVA";
        }
        return valor.toUpperCase();
    }
}
