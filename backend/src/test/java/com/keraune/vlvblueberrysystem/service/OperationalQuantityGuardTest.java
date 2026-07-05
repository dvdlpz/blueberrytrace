package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.entity.Cama;
import com.keraune.vlvblueberrysystem.entity.Clasificacion;
import com.keraune.vlvblueberrysystem.entity.Formalizacion;
import com.keraune.vlvblueberrysystem.entity.LoteTrazable;
import com.keraune.vlvblueberrysystem.entity.Merma;
import com.keraune.vlvblueberrysystem.entity.Siembra;
import com.keraune.vlvblueberrysystem.entity.Uniformizacion;
import com.keraune.vlvblueberrysystem.repository.ClasificacionRepository;
import com.keraune.vlvblueberrysystem.repository.DespachoRepository;
import com.keraune.vlvblueberrysystem.repository.FormalizacionRepository;
import com.keraune.vlvblueberrysystem.repository.MermaRepository;
import com.keraune.vlvblueberrysystem.repository.RecuperacionRiegoRepository;
import com.keraune.vlvblueberrysystem.repository.SiembraRepository;
import com.keraune.vlvblueberrysystem.repository.UniformizacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class OperationalQuantityGuardTest {
    private SiembraRepository siembras;
    private UniformizacionRepository uniformizaciones;
    private FormalizacionRepository formalizaciones;
    private ClasificacionRepository clasificaciones;
    private DespachoRepository despachos;
    private MermaRepository mermas;
    private RecuperacionRiegoRepository recuperaciones;
    private JabaInventoryService inventarioJabas;
    private OperationalQuantityGuard guard;
    private Cama cama;
    private LoteTrazable trazable;

    @BeforeEach
    void setUp() {
        siembras = mock(SiembraRepository.class);
        uniformizaciones = mock(UniformizacionRepository.class);
        formalizaciones = mock(FormalizacionRepository.class);
        clasificaciones = mock(ClasificacionRepository.class);
        despachos = mock(DespachoRepository.class);
        mermas = mock(MermaRepository.class);
        recuperaciones = mock(RecuperacionRiegoRepository.class);
        inventarioJabas = mock(JabaInventoryService.class);
        guard = new OperationalQuantityGuard(siembras, uniformizaciones, formalizaciones, clasificaciones, despachos, mermas, recuperaciones, inventarioJabas);

        cama = new Cama();
        cama.setId(10L);
        cama.setCapacidadReferencial(100);
        trazable = mock(LoteTrazable.class);
        when(trazable.getId()).thenReturn(20L);

        when(siembras.findByLoteTrazableId(20L)).thenReturn(List.of());
        when(uniformizaciones.findByLoteTrazableId(20L)).thenReturn(List.of());
        when(formalizaciones.findByLoteTrazableId(20L)).thenReturn(List.of());
        when(clasificaciones.findByLoteTrazableId(20L)).thenReturn(List.of());
        when(despachos.findByLoteTrazableId(20L)).thenReturn(List.of());
        when(mermas.findByLoteTrazableId(20L)).thenReturn(List.of());
        when(recuperaciones.findByLoteTrazableId(20L)).thenReturn(List.of());
    }

    @Test
    void rechazaSiembraQueSuperaLaCapacidadDeLaCama() {
        Siembra existing = new Siembra();
        existing.setId(1L);
        existing.setEstado("REGISTRADA");
        existing.setLoteTrazable(trazable);
        existing.setCantidadRegistrada(90);
        when(siembras.findByCamaId(10L)).thenReturn(List.of(existing));

        assertThrows(IllegalArgumentException.class, () -> guard.validateSiembra(cama, null, 11, "REGISTRADA"));
    }

    @Test
    void rechazaFormalizacionQueExcedeUniformizacionDisponible() {
        Uniformizacion uniformizacion = new Uniformizacion();
        uniformizacion.setId(1L);
        uniformizacion.setEstado("REGISTRADA");
        uniformizacion.setCantidadUniformizada(40);
        Formalizacion formalizacion = new Formalizacion();
        formalizacion.setId(2L);
        formalizacion.setEstado("REGISTRADA");
        formalizacion.setCantidadPlantas(35);
        when(uniformizaciones.findByLoteTrazableId(20L)).thenReturn(List.of(uniformizacion));
        when(formalizaciones.findByLoteTrazableId(20L)).thenReturn(List.of(formalizacion));

        assertThrows(IllegalArgumentException.class, () -> guard.validateFormalizacion(trazable, null, 6, "REGISTRADA"));
    }

    @Test
    void rechazaDespachoQueExcedeSaldoGlobalDespuesDeMermaDeClasificacion() {
        Clasificacion clasificacion = new Clasificacion();
        clasificacion.setId(30L);
        clasificacion.setLoteTrazable(trazable);
        clasificacion.setEstado("VALIDADA");
        clasificacion.setCantidad(50);
        when(clasificaciones.findByLoteTrazableId(20L)).thenReturn(List.of(clasificacion));
        when(despachos.findByClasificacionId(30L)).thenReturn(List.of());
        when(despachos.findByLoteTrazableId(20L)).thenReturn(List.of());

        Merma merma = new Merma();
        merma.setEstado("REGISTRADA");
        merma.setEtapaOrigen("CLASIFICACION");
        merma.setCantidad(10);
        when(mermas.findByLoteTrazableId(20L)).thenReturn(List.of(merma));

        assertThrows(IllegalArgumentException.class,
                () -> guard.validateDespacho(trazable, clasificacion, null, 45, "DESPACHADO"));
    }

    @Test
    void rechazaFormalizacionConFechaAnteriorAUniformizacionRegistrada() {
        Uniformizacion uniformizacion = new Uniformizacion();
        uniformizacion.setEstado("REGISTRADA");
        uniformizacion.setFechaUniformizacion(LocalDate.of(2026, 6, 15));
        when(uniformizaciones.findByLoteTrazableId(20L)).thenReturn(List.of(uniformizacion));

        assertThrows(IllegalArgumentException.class,
                () -> guard.validateChronology(trazable, "FORMALIZACION", LocalDate.of(2026, 6, 14)));
    }

    @Test
    void permiteCantidadOperativaConsistente() {
        when(siembras.findByCamaId(10L)).thenReturn(List.of());
        assertDoesNotThrow(() -> guard.validateSiembra(cama, null, 100, "REGISTRADA"));
    }
    @Test
    void ignoraSiembrasHistoricasSinLoteTrazableAlCalcularCapacidadActual() {
        Siembra historical = new Siembra();
        historical.setId(2L);
        historical.setEstado("REGISTRADA");
        historical.setCantidadRegistrada(100);
        when(siembras.findByCamaId(10L)).thenReturn(List.of(historical));

        assertDoesNotThrow(() -> guard.validateSiembra(cama, null, 100, "REGISTRADA"));
    }

    @Test
    void permiteActualizarUnDespachoConfirmadoSinDescontarloDosVeces() {
        Clasificacion classification = new Clasificacion();
        classification.setId(30L);
        classification.setLoteTrazable(trazable);
        classification.setEstado("VALIDADA");
        classification.setCantidad(100);

        com.keraune.vlvblueberrysystem.entity.Despacho existing = new com.keraune.vlvblueberrysystem.entity.Despacho();
        existing.setId(40L);
        existing.setEstado("DESPACHADO");
        existing.setCantidadDespachada(90);

        when(clasificaciones.findByLoteTrazableId(20L)).thenReturn(List.of(classification));
        when(despachos.findByClasificacionId(30L)).thenReturn(List.of(existing));
        when(despachos.findByLoteTrazableId(20L)).thenReturn(List.of(existing));

        assertDoesNotThrow(() -> guard.validateDespacho(trazable, classification, 40L, 90, "DESPACHADO"));
    }

}
