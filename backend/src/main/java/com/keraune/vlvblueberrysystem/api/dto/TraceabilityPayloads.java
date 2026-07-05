package com.keraune.vlvblueberrysystem.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class TraceabilityPayloads {
    private TraceabilityPayloads() {
    }

    public record LoteTrazableFormPayload(
            @NotBlank @Size(max = 50) String codigo,
            @NotBlank @Size(max = 120) String variedad,
            @NotBlank @Size(max = 180) String procedencia,
            @NotNull LocalDate fechaIngreso,
            @NotBlank @Size(max = 30) String estado,
            @Size(max = 255) String observacion,
            @NotNull Long loteFisicoId,
            @NotNull Long camaInicialId
    ) {}

    public record LoteTrazableResponse(
            Long id,
            String codigo,
            String variedad,
            String procedencia,
            LocalDate fechaIngreso,
            String estado,
            String observacion,
            boolean legadoPendienteNormalizacion,
            ApiPayloads.ReferenceResponse loteFisico,
            ApiPayloads.ReferenceResponse camaInicial,
            ApiPayloads.UserReferenceResponse usuarioResponsable,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaActualizacion
    ) {}

    public record LegacyMovementResponse(
            String etapa,
            Long id,
            String referencia,
            LocalDate fecha,
            Integer cantidad,
            String estado,
            ApiPayloads.ReferenceResponse lote,
            ApiPayloads.ReferenceResponse cama,
            String detalle
    ) {}

    public record LegacyNormalizationPayload(
            @NotBlank @Size(max = 40) String etapa,
            @NotNull Long registroId,
            @NotBlank @Size(max = 255) String evidencia
    ) {}

    public record MermaFormPayload(
            @NotNull Long loteTrazableId,
            @NotBlank @Size(max = 40) String etapaOrigen,
            @NotBlank @Size(max = 120) String motivo,
            @NotNull @Positive Integer cantidad,
            @NotNull LocalDate fechaMerma,
            @Size(max = 255) String observacion
    ) {}

    public record MermaResponse(
            Long id,
            ApiPayloads.ReferenceResponse loteTrazable,
            String etapaOrigen,
            String motivo,
            Integer cantidad,
            LocalDate fechaMerma,
            String observacion,
            String estado,
            ApiPayloads.UserReferenceResponse usuarioRegistro,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaActualizacion
    ) {}

    public record BalanceOperativoResponse(
            long sembradas,
            long uniformizadas,
            long formalizadas,
            long clasificacionPendiente,
            long clasificacionValidada,
            long clasificacionObservada,
            long despachadas,
            long anuladas,
            long mermas,
            long saldoDisponible,
            long enRecuperacion
    ) {}

    public record TimelineEventResponse(
            String etapa,
            String estado,
            Integer cantidad,
            LocalDate fecha,
            String referencia,
            String detalle,
            String responsable
    ) {}

    public record LoteTrazableDetailResponse(
            LoteTrazableResponse loteTrazable,
            BalanceOperativoResponse balance,
            List<TimelineEventResponse> lineaTiempo,
            List<MermaResponse> mermas,
            List<LegacyMovementResponse> pendientesLegado
    ) {}
}
