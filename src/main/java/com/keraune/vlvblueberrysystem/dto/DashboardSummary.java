package com.keraune.vlvblueberrysystem.dto;

public record DashboardSummary(
        long lotesRegistrados,
        long lotesActivos,
        long lotesInactivos,
        long camasRegistradas,
        long camasActivas,
        long camasInactivas,
        long capacidadReferencialTotal,
        long siembrasRegistradas,
        long plantasSembradas,
        long uniformizacionesRegistradas,
        long formalizacionesRegistradas,
        long clasificacionesRegistradas,
        long clasificacionesPendientes,
        long clasificacionesValidadas,
        long despachosRegistrados,
        long plantasDespachadas,
        int porcentajeLotesActivos,
        int porcentajeCamasActivas,
        int porcentajeClasificacionesValidadas,
        int porcentajePlantasDespachadas,
        int porcentajeUniformizacionesSobreSiembras,
        int porcentajeFormalizacionesSobreSiembras,
        int porcentajeClasificacionesSobreSiembras,
        int porcentajeDespachosSobreSiembras
) {
}
