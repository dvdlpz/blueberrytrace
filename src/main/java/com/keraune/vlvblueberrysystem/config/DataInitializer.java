package com.keraune.vlvblueberrysystem.config;

import com.keraune.vlvblueberrysystem.entity.Cama;
import com.keraune.vlvblueberrysystem.entity.Clasificacion;
import com.keraune.vlvblueberrysystem.entity.Despacho;
import com.keraune.vlvblueberrysystem.entity.Formalizacion;
import com.keraune.vlvblueberrysystem.entity.Lote;
import com.keraune.vlvblueberrysystem.entity.Role;
import com.keraune.vlvblueberrysystem.entity.Siembra;
import com.keraune.vlvblueberrysystem.entity.Uniformizacion;
import com.keraune.vlvblueberrysystem.entity.User;
import com.keraune.vlvblueberrysystem.repository.CamaRepository;
import com.keraune.vlvblueberrysystem.repository.ClasificacionRepository;
import com.keraune.vlvblueberrysystem.repository.DespachoRepository;
import com.keraune.vlvblueberrysystem.repository.FormalizacionRepository;
import com.keraune.vlvblueberrysystem.repository.LoteRepository;
import com.keraune.vlvblueberrysystem.repository.RoleRepository;
import com.keraune.vlvblueberrysystem.repository.SiembraRepository;
import com.keraune.vlvblueberrysystem.repository.UniformizacionRepository;
import com.keraune.vlvblueberrysystem.repository.UserRepository;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initRolesAndAdmin(
            RoleRepository roleRepository,
            UserRepository userRepository,
            LoteRepository loteRepository,
            CamaRepository camaRepository,
            SiembraRepository siembraRepository,
            UniformizacionRepository uniformizacionRepository,
            FormalizacionRepository formalizacionRepository,
            ClasificacionRepository clasificacionRepository,
            DespachoRepository despachoRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            createRoleIfNotExists(roleRepository, "ADMINISTRADOR", "Acceso total al sistema");
            createRoleIfNotExists(roleRepository, "SUPERVISOR", "Supervisa el proceso operativo");
            createRoleIfNotExists(roleRepository, "CONTABILIZADOR", "Registra conteos y cantidades");
            createRoleIfNotExists(roleRepository, "OPERARIO", "Registra actividades operativas");
            createRoleIfNotExists(roleRepository, "CONTROL_CALIDAD", "Valida calidad y despacho");

            if (userRepository.findByUsername("admin").isEmpty()) {
                Role adminRole = roleRepository
                        .findByNombre("ADMINISTRADOR")
                        .orElseThrow(() -> new IllegalStateException("No existe el rol ADMINISTRADOR"));

                User admin = new User();
                admin.setRol(adminRole);
                admin.setNombreCompleto("Administrador del Sistema");
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setEmail("admin@vlv.local");
                admin.setEstado(true);

                userRepository.save(admin);
            }

            User admin = userRepository
                    .findByUsername("admin")
                    .orElseThrow(() -> new IllegalStateException("No existe el usuario administrador."));

            if (loteRepository.count() == 0) {
                Lote loteUno = new Lote();
                loteUno.setCodigo("A1");
                loteUno.setDescripcion("Invernadero del sector A para frutales");
                loteUno.setCultivo("Arándano");
                loteUno.setVariedad("Biloxi");
                loteUno.setFechaRegistro(LocalDate.now());
                loteUno.setObservacion("Invernadero cubierto con filas de bandejas para propagación.");
                loteUno.setEstado("ACTIVO");
                loteUno.setUsuarioRegistro(admin);

                Lote loteDos = new Lote();
                loteDos.setCodigo("B1");
                loteDos.setDescripcion("Invernadero del sector B en evaluación operativa");
                loteDos.setCultivo("Arándano");
                loteDos.setVariedad("Ventura");
                loteDos.setFechaRegistro(LocalDate.now().minusDays(1));
                loteDos.setObservacion("Pendiente de validación de camas y distribución.");
                loteDos.setEstado("PENDIENTE");
                loteDos.setUsuarioRegistro(admin);

                loteRepository.save(loteUno);
                loteRepository.save(loteDos);
            }

            if (camaRepository.count() == 0) {
                Lote lotePrincipal = loteRepository
                        .findByCodigo("A1")
                        .orElseThrow(() -> new IllegalStateException("No existe el invernadero A1."));
                Lote loteSecundario = loteRepository
                        .findByCodigo("B1")
                        .orElseThrow(() -> new IllegalStateException("No existe el invernadero B1."));

                Cama camaUno = new Cama();
                camaUno.setCodigo("CAM-001");
                camaUno.setDescripcion("Cama sector 1");
                camaUno.setCapacidadReferencial(1200);
                camaUno.setEstado("ACTIVA");
                camaUno.setLote(lotePrincipal);
                camaUno.setUsuarioRegistro(admin);

                Cama camaDos = new Cama();
                camaDos.setCodigo("CAM-002");
                camaDos.setDescripcion("Cama sector 2");
                camaDos.setCapacidadReferencial(1000);
                camaDos.setEstado("MANTENIMIENTO");
                camaDos.setLote(loteSecundario);
                camaDos.setUsuarioRegistro(admin);

                camaRepository.save(camaUno);
                camaRepository.save(camaDos);
            }

            loteRepository.findByCodigo("A1").ifPresent(lote ->
                    camaRepository.findByCodigo("CAM-001").ifPresent(cama -> {
                        if (siembraRepository.count() == 0) {
                            Siembra siembra = new Siembra();
                            siembra.setLote(lote);
                            siembra.setCama(cama);
                            siembra.setFechaSiembra(LocalDate.now().minusDays(5));
                            siembra.setCantidadRegistrada(900);
                            siembra.setObservacion("Registro inicial de plantas ubicadas en cama.");
                            siembra.setEstado("REGISTRADA");
                            siembra.setUsuarioRegistro(admin);
                            siembraRepository.save(siembra);
                        }

                        if (uniformizacionRepository.count() == 0) {
                            Uniformizacion uniformizacion = new Uniformizacion();
                            uniformizacion.setLote(lote);
                            uniformizacion.setCama(cama);
                            uniformizacion.setFechaUniformizacion(LocalDate.now().minusDays(3));
                            uniformizacion.setCriterio("Tamaño y vigor de planta");
                            uniformizacion.setCantidadInicial(900);
                            uniformizacion.setCantidadUniformizada(820);
                            uniformizacion.setObservacion("Agrupación por uniformidad de crecimiento.");
                            uniformizacion.setEstado("REGISTRADA");
                            uniformizacion.setUsuarioRegistro(admin);
                            uniformizacionRepository.save(uniformizacion);
                        }

                        if (formalizacionRepository.count() == 0) {
                            Formalizacion formalizacion = new Formalizacion();
                            formalizacion.setLote(lote);
                            formalizacion.setCama(cama);
                            formalizacion.setFechaFormalizacion(LocalDate.now().minusDays(2));
                            formalizacion.setDetalle("Formalización de bandejas por lote y cama");
                            formalizacion.setCantidadBandejas(42);
                            formalizacion.setCantidadPlantas(800);
                            formalizacion.setObservacion("Bandejas formalizadas para clasificación posterior.");
                            formalizacion.setEstado("REGISTRADA");
                            formalizacion.setUsuarioRegistro(admin);
                            formalizacionRepository.save(formalizacion);
                        }

                        if (clasificacionRepository.count() == 0) {
                            Clasificacion clasificacion = new Clasificacion();
                            clasificacion.setLote(lote);
                            clasificacion.setCama(cama);
                            clasificacion.setFechaClasificacion(LocalDate.now().minusDays(1));
                            clasificacion.setEstadoPlanta("Óptimo");
                            clasificacion.setTamano("Mediano");
                            clasificacion.setCondicion("Apta para despacho");
                            clasificacion.setCantidad(760);
                            clasificacion.setObservacion("Clasificación preliminar validada por control de calidad.");
                            clasificacion.setEstado("VALIDADA");
                            clasificacion.setUsuarioRegistro(admin);
                            clasificacionRepository.save(clasificacion);
                        }

                        if (despachoRepository.count() == 0) {
                            Despacho despacho = new Despacho();
                            despacho.setLote(lote);
                            despacho.setFechaDespacho(LocalDate.now());
                            despacho.setModalidad("JABAS");
                            despacho.setCantidadDespachada(300);
                            despacho.setDestino("Almacén de despacho interno");
                            despacho.setGuiaRemision("GR-001");
                            despacho.setValidacionCalidad("APROBADO");
                            despacho.setObservacion("Primer despacho de referencia del sistema.");
                            despacho.setEstado("REGISTRADO");
                            despacho.setUsuarioRegistro(admin);
                            despachoRepository.save(despacho);
                        }
                    }));
        };
    }

    private void createRoleIfNotExists(RoleRepository roleRepository, String nombre, String descripcion) {
        if (roleRepository.findByNombre(nombre).isEmpty()) {
            Role role = new Role();
            role.setNombre(nombre);
            role.setDescripcion(descripcion);
            role.setEstado(true);
            roleRepository.save(role);
        }
    }
}
