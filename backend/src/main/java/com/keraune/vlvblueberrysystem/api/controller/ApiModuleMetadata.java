package com.keraune.vlvblueberrysystem.api.controller;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.EndpointResponse;
import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.ModuleResponse;
import java.util.List;
import java.util.Set;

public final class ApiModuleMetadata {
    private ApiModuleMetadata() {}

    public static List<ModuleResponse> modules() {
        return List.of(
                new ModuleResponse("dashboard", "Inicio", "/dashboard", "/api/v1/dashboard/summary"),
                new ModuleResponse("usuarios", "Usuarios", "/usuarios", "/api/v1/usuarios"),
                new ModuleResponse("roles", "Roles", "/roles", "/api/v1/roles"),
                new ModuleResponse("lotes", "Lotes", "/lotes", "/api/v1/lotes"),
                new ModuleResponse("camas", "Camas", "/camas", "/api/v1/camas"),
                new ModuleResponse("jabas", "Jabas de siembra", "/jabas", "/api/v1/jabas"),
                new ModuleResponse("lotes_trazables", "Lotes trazables", "/lotes-trazables", "/api/v1/lotes-trazables"),
                new ModuleResponse("siembra", "Siembra", "/siembra", "/api/v1/siembras"),
                new ModuleResponse("riegos", "Riegos programados", "/riegos", "/api/v1/riegos-programados"),
                new ModuleResponse("procesos", "Procesos", "/procesos", "/api/v1/procesos"),
                new ModuleResponse("recuperacion", "Recuperación por riego", "/recuperacion", "/api/v1/recuperaciones-riego"),
                new ModuleResponse("clasificacion", "Clasificación", "/clasificacion", "/api/v1/clasificaciones"),
                new ModuleResponse("pedidos", "Pedidos por variedad", "/pedidos", "/api/v1/pedidos"),
                new ModuleResponse("empaques", "Empaques", "/empaques", "/api/v1/empaques"),
                new ModuleResponse("despacho", "Despacho", "/despacho", "/api/v1/despachos"),
                new ModuleResponse("mermas", "Mermas", "/mermas", "/api/v1/mermas"),
                new ModuleResponse("trazabilidad", "Trazabilidad", "/trazabilidad", "/api/v1/lotes-trazables"),
                new ModuleResponse("reportes", "Reportes", "/reportes", "/api/v1/reportes/trazabilidad"),
                new ModuleResponse("auditoria", "Auditoría", "/auditoria", "/api/v1/auditoria")
        );
    }

    public static List<ModuleResponse> modulesFor(Set<String> allowed) {
        return modules().stream().filter(module -> allowed.contains(module.key())).toList();
    }

    public static List<EndpointResponse> endpoints() {
        return List.of(
                new EndpointResponse("GET", "/api/v1/health", "Estado del servicio"),
                new EndpointResponse("POST", "/api/v1/auth/login", "Inicio de sesión con usuario o correo"),
                new EndpointResponse("GET", "/api/v1/dashboard/summary", "Indicadores operativos"),
                new EndpointResponse("GET/POST/PUT/PATCH", "/api/v1/lotes-trazables", "Trazabilidad por lote de plantas"),
                new EndpointResponse("GET/POST/PUT/PATCH", "/api/v1/jabas", "Jabas de siembra y capacidad de macetas"),
                new EndpointResponse("GET/POST/PATCH", "/api/v1/riegos-programados", "Programación y ejecución de riegos"),
                new EndpointResponse("GET/POST/PATCH", "/api/v1/recuperaciones-riego", "Recuperación por riego y descarte"),
                new EndpointResponse("GET/POST/PUT/PATCH", "/api/v1/pedidos", "Pedidos por variedad"),
                new EndpointResponse("GET/POST/PATCH", "/api/v1/empaques", "Empaques por jaba cosechera o bin"),
                new EndpointResponse("GET/POST/DELETE/PATCH", "/api/v1/cargas-despacho", "Cargas de tráiler por pedido y líneas de variedad"),
                new EndpointResponse("GET/POST/PATCH", "/api/v1/mermas", "Mermas y ajustes de trazabilidad"),
                new EndpointResponse("GET/PUT/PATCH", "/api/v1/roles", "Gestión de roles fijos y permisos"),
                new EndpointResponse("GET", "/api/v1/auditoria", "Auditoría operativa y administrativa")
        );
    }
}
