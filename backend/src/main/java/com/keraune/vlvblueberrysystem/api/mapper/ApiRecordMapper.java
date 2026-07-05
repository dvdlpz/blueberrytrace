package com.keraune.vlvblueberrysystem.api.mapper;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.*;
import com.keraune.vlvblueberrysystem.dto.TrazabilidadRow;
import com.keraune.vlvblueberrysystem.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ApiRecordMapper {
    public UserReferenceResponse user(User user) {
        if (user == null) return null;
        return new UserReferenceResponse(
                user.getId(),
                user.getUsername(),
                user.getNombreCompleto(),
                user.getEmail(),
                user.getCargo(),
                user.getTelefono(),
                user.getAvatarColor(),
                user.getAvatarImage(),
                user.getRole() != null ? user.getRole().getNombre() : null,
                Boolean.TRUE.equals(user.getEstado()),
                user.getFechaCreacion(),
                user.getFechaActualizacion()
        );
    }

    public ReferenceResponse reference(Lote lote) {
        if (lote == null) return null;
        return new ReferenceResponse(lote.getId(), lote.getCodigo(), lote.getDescripcion());
    }

    public ReferenceResponse reference(Cama cama) {
        if (cama == null) return null;
        return new ReferenceResponse(cama.getId(), cama.getCodigo(), cama.getDescripcion());
    }

    public ReferenceResponse reference(Jaba jaba) {
        if (jaba == null) return null;
        return new ReferenceResponse(jaba.getId(), jaba.getCodigo(), "Capacidad: " + (jaba.getCapacidadMacetas() == null ? 0 : jaba.getCapacidadMacetas()) + " macetas");
    }

    public ReferenceResponse reference(Pedido pedido) {
        if (pedido == null) return null;
        return new ReferenceResponse(pedido.getId(), pedido.getCodigo(), pedido.getCliente());
    }

    public ReferenceResponse reference(PedidoDetalle detalle) {
        if (detalle == null) return null;
        return new ReferenceResponse(detalle.getId(), detalle.getVariedad(), "Cantidad solicitada: " + (detalle.getCantidadSolicitada() == null ? 0 : detalle.getCantidadSolicitada()));
    }

    public ReferenceResponse reference(Empaque empaque) {
        if (empaque == null) return null;
        return new ReferenceResponse(empaque.getId(), "EMP-" + empaque.getId(), empaque.getTipo() + " · " + (empaque.getCantidadPlantas() == null ? 0 : empaque.getCantidadPlantas()) + " plantas");
    }

    public ReferenceResponse reference(CargaDespacho carga) {
        if (carga == null) return null;
        return new ReferenceResponse(carga.getId(), carga.getCodigo(), carga.getEstado() + " · " + carga.getVehiculo());
    }

    public ReferenceResponse reference(Merma merma) {
        if (merma == null) return null;
        return new ReferenceResponse(merma.getId(), "MERMA-" + merma.getId(), merma.getMotivo());
    }

    public ReferenceResponse reference(RecuperacionRiego recuperacion) {
        if (recuperacion == null) return null;
        String detail = (recuperacion.getEstado() == null ? "Recuperación por riego" : recuperacion.getEstado())
                + " · " + (recuperacion.getCantidadIngresada() == null ? 0 : recuperacion.getCantidadIngresada()) + " plantas";
        return new ReferenceResponse(recuperacion.getId(), "RIEGO-" + recuperacion.getId(), detail);
    }

    public ReferenceResponse reference(LoteTrazable loteTrazable) {
        if (loteTrazable == null) return null;
        return new ReferenceResponse(loteTrazable.getId(), loteTrazable.getCodigo(), loteTrazable.getVariedad());
    }

    public ReferenceResponse reference(Clasificacion clasificacion) {
        if (clasificacion == null) return null;
        return new ReferenceResponse(clasificacion.getId(), "CLAS-" + clasificacion.getId(), clasificacion.getEstado() + " · " + clasificacion.getTamano());
    }

    public LoteResponse lote(Lote lote) {
        return new LoteResponse(lote.getId(), lote.getCodigo(), lote.getDescripcion(), lote.getCultivo(), lote.getVariedad(),
                lote.getFechaRegistro(), lote.getObservacion(), lote.getEstado(), user(lote.getUsuarioRegistro()),
                lote.getFechaCreacion(), lote.getFechaActualizacion());
    }

    public CamaResponse cama(Cama cama) {
        return new CamaResponse(cama.getId(), cama.getCodigo(), cama.getDescripcion(), cama.getCapacidadReferencial(), cama.getEstado(),
                reference(cama.getLote()), user(cama.getUsuarioRegistro()), cama.getFechaCreacion(), cama.getFechaActualizacion());
    }

    public JabaResponse jaba(Jaba jaba) {
        return new JabaResponse(jaba.getId(), jaba.getCodigo(), jaba.getCapacidadMacetas(), jaba.getOrdenEnCama(), null, null, null, jaba.getEstado(), jaba.getObservacion(), reference(jaba.getCama()), user(jaba.getUsuarioRegistro()), jaba.getFechaCreacion(), jaba.getFechaActualizacion());
    }

    public SiembraResponse siembra(Siembra siembra) {
        return new SiembraResponse(siembra.getId(), reference(siembra.getLote()), reference(siembra.getCama()), reference(siembra.getJaba()), siembra.getFechaSiembra(),
                siembra.getCantidadRegistrada(), siembra.getObservacion(), siembra.getEstado(), user(siembra.getUsuarioRegistro()),
                reference(siembra.getLoteTrazable()), siembra.getFechaCreacion(), siembra.getFechaActualizacion());
    }

    public UniformizacionResponse uniformizacion(Uniformizacion uniformizacion) {
        return new UniformizacionResponse(uniformizacion.getId(), reference(uniformizacion.getLote()), reference(uniformizacion.getCama()), reference(uniformizacion.getJabaOrigen()), reference(uniformizacion.getJabaDestino()),
                uniformizacion.getFechaUniformizacion(), uniformizacion.getCriterio(), uniformizacion.getCantidadInicial(), uniformizacion.getCantidadUniformizada(),
                uniformizacion.getOrigenOperativo(), uniformizacion.getCantidadRecuperacion(), reference(uniformizacion.getRecuperacionRiego()), uniformizacion.getMalezasRetiradas(), uniformizacion.getObservacion(), uniformizacion.getEstado(),
                user(uniformizacion.getUsuarioRegistro()), reference(uniformizacion.getLoteTrazable()), uniformizacion.getFechaCreacion(), uniformizacion.getFechaActualizacion());
    }

    public FormalizacionResponse formalizacion(Formalizacion formalizacion) {
        return new FormalizacionResponse(formalizacion.getId(), reference(formalizacion.getLote()), reference(formalizacion.getCama()),
                formalizacion.getFechaFormalizacion(), formalizacion.getDetalle(), formalizacion.getCantidadBandejas(),
                formalizacion.getJabasMovidas().stream().map(this::reference).toList(), formalizacion.getCantidadPlantas(), formalizacion.getOrdenamientoJabas(), formalizacion.getObservacion(), formalizacion.getEstado(),
                user(formalizacion.getUsuarioRegistro()), reference(formalizacion.getLoteTrazable()), formalizacion.getFechaCreacion(), formalizacion.getFechaActualizacion());
    }

    public ClasificacionResponse clasificacion(Clasificacion clasificacion) {
        return new ClasificacionResponse(clasificacion.getId(), reference(clasificacion.getLote()), reference(clasificacion.getCama()), reference(clasificacion.getJaba()),
                clasificacion.getFechaClasificacion(), clasificacion.getEstadoPlanta(), clasificacion.getTamano(),
                clasificacion.getCondicion(), clasificacion.getCantidad(), clasificacion.getObservacion(), clasificacion.getEstado(),
                reference(clasificacion.getRecuperacionRiego()), user(clasificacion.getUsuarioRegistro()), reference(clasificacion.getLoteTrazable()), clasificacion.getFechaCreacion(), clasificacion.getFechaActualizacion());
    }

    public DespachoResponse despacho(Despacho despacho) {
        String modalidad = despacho.getEmpaque() != null ? despacho.getEmpaque().getTipo() : (despacho.getModalidadDespacho() != null ? despacho.getModalidadDespacho() : despacho.getModalidad());
        return new DespachoResponse(despacho.getId(), reference(despacho.getLote()), despacho.getFechaDespacho(), modalidad,
                despacho.getCantidadDespachada(), despacho.getDestino(), despacho.getGuiaRemision(), despacho.getValidacionCalidad(),
                despacho.getObservacion(), despacho.getEstado(), user(despacho.getUsuarioRegistro()), reference(despacho.getLoteTrazable()), reference(despacho.getClasificacion()),
                reference(despacho.getPedido()), reference(despacho.getPedidoDetalle()), reference(despacho.getEmpaque()), despacho.getUnidadesEmpaque(), despacho.getVehiculo(), reference(despacho.getCargaDespacho()), despacho.getFechaCreacion(), despacho.getFechaActualizacion());
    }

    public RecuperacionRiegoResponse recuperacion(RecuperacionRiego item) {
        return new RecuperacionRiegoResponse(item.getId(), reference(item.getLoteTrazable()), reference(item.getJaba()), item.getEtapaOrigen(), item.getEtapaRetorno(), item.getFechaIngresoRiego(),
                item.getCantidadIngresada(), item.getCantidadRecuperada(), item.getCantidadDescartada(), item.getMotivoDescarte(), item.getObservacion(), item.getEstado(), reference(item.getMermaGenerada()),
                user(item.getUsuarioRegistro()), item.getFechaCreacion(), item.getFechaActualizacion());
    }

    public RiegoProgramadoResponse riego(RiegoProgramado item) {
        return new RiegoProgramadoResponse(item.getId(), reference(item.getLoteTrazable()), reference(item.getCama()), reference(item.getJaba()),
                item.getFechaProgramada(), item.getHoraProgramada(), item.getFechaEjecucion(), item.getHoraEjecucion(), item.getEtapaAplicacion(),
                item.getEstado(), item.getObservacion(), user(item.getUsuarioRegistro()), item.getFechaCreacion(), item.getFechaActualizacion());
    }

    public TrazabilidadResponse trazabilidad(TrazabilidadRow row) {
        return new TrazabilidadResponse(row.id(), new ReferenceResponse(row.loteId(), row.codigoLote(), row.descripcionLote()),
                row.camas(), row.siembras(), row.plantasSembradas(), row.uniformizaciones(), row.formalizaciones(),
                row.clasificaciones(), row.despachos(), row.plantasDespachadas(), row.ultimoEvento());
    }

    public <T> ListResponse<T> list(List<T> items) {
        return new ListResponse<>(items.size(), items);
    }
}
