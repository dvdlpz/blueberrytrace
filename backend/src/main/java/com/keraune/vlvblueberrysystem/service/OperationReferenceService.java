package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.entity.Cama;
import com.keraune.vlvblueberrysystem.entity.Lote;
import com.keraune.vlvblueberrysystem.repository.CamaRepository;
import com.keraune.vlvblueberrysystem.repository.LoteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

@Service
public class OperationReferenceService {
    private final LoteRepository loteRepository;
    private final CamaRepository camaRepository;

    public OperationReferenceService(LoteRepository loteRepository, CamaRepository camaRepository) {
        this.loteRepository = loteRepository;
        this.camaRepository = camaRepository;
    }

    public Lote lote(Long id) {
        return loteRepository.findByIdForUpdate(id).orElseThrow(() -> new IllegalArgumentException("Lote no encontrado"));
    }

    public Cama cama(Long id) {
        return camaRepository.findByIdForUpdate(id).orElseThrow(() -> new IllegalArgumentException("Cama no encontrada"));
    }

    public Cama camaDelLote(Long camaId, Long loteId) {
        Cama cama = cama(camaId);
        if (cama.getLote() == null || !cama.getLote().getId().equals(loteId)) {
            throw new IllegalArgumentException("La cama seleccionada no pertenece al lote indicado.");
        }
        return cama;
    }

    public void validateOperationalDate(Lote lote, LocalDate date, String fieldLabel) {
        if (date == null) {
            throw new IllegalArgumentException("La fecha de " + fieldLabel + " es obligatoria.");
        }
        if (lote.getFechaRegistro() != null && date.isBefore(lote.getFechaRegistro())) {
            throw new IllegalArgumentException("La fecha de " + fieldLabel + " no puede ser anterior al registro del lote.");
        }
    }

    public String clean(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.trim().toUpperCase(Locale.ROOT);
    }

    public String allowedState(String value, String fallback, Set<String> permitted, String fieldLabel) {
        String normalized = clean(value, fallback);
        if (!permitted.contains(normalized)) {
            throw new IllegalArgumentException("Estado de " + fieldLabel + " no válido.");
        }
        return normalized;
    }

    public String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
