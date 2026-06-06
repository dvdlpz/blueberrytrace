package com.keraune.vlvblueberrysystem.dto;

import com.keraune.vlvblueberrysystem.entity.Lote;

public record TrazabilidadRow(
        Lote lote,
        long camas,
        long siembras,
        long plantasSembradas,
        long uniformizaciones,
        long formalizaciones,
        long clasificaciones,
        long despachos,
        long plantasDespachadas,
        String ultimoEvento
) {
}
