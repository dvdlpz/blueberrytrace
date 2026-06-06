package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.dto.SiembraForm;
import com.keraune.vlvblueberrysystem.entity.Cama;
import com.keraune.vlvblueberrysystem.entity.Lote;
import com.keraune.vlvblueberrysystem.entity.Siembra;
import com.keraune.vlvblueberrysystem.entity.User;
import com.keraune.vlvblueberrysystem.repository.CamaRepository;
import com.keraune.vlvblueberrysystem.repository.LoteRepository;
import com.keraune.vlvblueberrysystem.repository.SiembraRepository;
import com.keraune.vlvblueberrysystem.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiembraService {

    private final SiembraRepository siembraRepository;
    private final LoteRepository loteRepository;
    private final CamaRepository camaRepository;
    private final UserRepository userRepository;

    public SiembraService(SiembraRepository siembraRepository, LoteRepository loteRepository,
                          CamaRepository camaRepository, UserRepository userRepository) {
        this.siembraRepository = siembraRepository;
        this.loteRepository = loteRepository;
        this.camaRepository = camaRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Siembra> listarTodas() {
        return siembraRepository.findAllByOrderByFechaSiembraDescIdDesc();
    }

    @Transactional(readOnly = true)
    public Siembra obtenerPorId(Long id) {
        return siembraRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el registro de siembra solicitado."));
    }

    @Transactional
    public void crearSiembra(SiembraForm form, String username) {
        Lote lote = obtenerLote(form.getLoteId());
        Cama cama = obtenerCamaValida(form.getCamaId(), lote.getId());
        User usuario = obtenerUsuario(username);

        Siembra siembra = new Siembra();
        siembra.setLote(lote);
        siembra.setCama(cama);
        siembra.setFechaSiembra(form.getFechaSiembra());
        siembra.setCantidadRegistrada(form.getCantidadRegistrada());
        siembra.setObservacion(normalizarOpcional(form.getObservacion()));
        siembra.setEstado(normalizarEstado(form.getEstado(), "REGISTRADA"));
        siembra.setUsuarioRegistro(usuario);
        siembraRepository.save(siembra);
    }

    @Transactional
    public void cambiarEstado(Long id) {
        Siembra siembra = obtenerPorId(id);
        siembra.setEstado("REGISTRADA".equalsIgnoreCase(siembra.getEstado()) ? "ANULADA" : "REGISTRADA");
        siembraRepository.save(siembra);
    }

    public SiembraForm crearFormularioInicial() {
        SiembraForm form = new SiembraForm();
        form.setFechaSiembra(LocalDate.now());
        form.setEstado("REGISTRADA");
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

    private String normalizarEstado(String estado, String estadoPorDefecto) {
        String valor = estado == null ? null : estado.trim();
        return (valor == null || valor.isBlank()) ? estadoPorDefecto : valor.toUpperCase();
    }

    private String normalizarOpcional(String valor) {
        String texto = valor == null ? null : valor.trim();
        return (texto == null || texto.isBlank()) ? null : texto;
    }
}
