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
        double porcentajeLotesActivos,
        double porcentajeCamasActivas,
        double porcentajeClasificacionesValidadas,
        double porcentajePlantasDespachadas,
        double porcentajeUniformizacionesSobreSiembras,
        double porcentajeFormalizacionesSobreSiembras,
        double porcentajeClasificacionesSobreSiembras,
        double porcentajeDespachosSobreSiembras
) {}
