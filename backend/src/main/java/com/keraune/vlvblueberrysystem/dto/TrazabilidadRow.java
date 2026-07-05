package com.keraune.vlvblueberrysystem.dto;

public record TrazabilidadRow(
        Long id,
        Long loteId,
        String codigoLote,
        String descripcionLote,
        long camas,
        long siembras,
        long plantasSembradas,
        long uniformizaciones,
        long formalizaciones,
        long clasificaciones,
        long despachos,
        long plantasDespachadas,
        String ultimoEvento
) {}
