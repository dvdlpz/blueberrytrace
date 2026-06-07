-- =========================================================
-- BlueberryTrace - Script base de datos MySQL
-- Sistema de trazabilidad de arándanos - Vivero Los Viñedos
-- Versión recomendada para el backend MVC actual
-- =========================================================

-- Opcional para reiniciar desde cero:
-- DROP DATABASE IF EXISTS vlv_blueberry_system;

CREATE DATABASE IF NOT EXISTS vlv_blueberry_system
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE vlv_blueberry_system;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS despachos;
DROP TABLE IF EXISTS clasificaciones;
DROP TABLE IF EXISTS formalizaciones;
DROP TABLE IF EXISTS uniformizaciones;
DROP TABLE IF EXISTS siembras;
DROP TABLE IF EXISTS camas;
DROP TABLE IF EXISTS lotes;
DROP TABLE IF EXISTS usuarios;
DROP TABLE IF EXISTS roles;

SET FOREIGN_KEY_CHECKS = 1;

-- =========================================================
-- 1. ROLES
-- =========================================================
CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================================
-- 2. USUARIOS
-- =========================================================
CREATE TABLE usuarios (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rol_id BIGINT NOT NULL,
    nombre_completo VARCHAR(150) NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(120),
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuarios_roles
        FOREIGN KEY (rol_id) REFERENCES roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================================
-- 3. LOTES / INVERNADEROS
-- =========================================================
CREATE TABLE lotes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    descripcion VARCHAR(150) NOT NULL,
    cultivo VARCHAR(120),
    variedad VARCHAR(120),
    fecha_registro DATE NOT NULL,
    observacion VARCHAR(255),
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    usuario_registro_id BIGINT NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lotes_usuario_registro
        FOREIGN KEY (usuario_registro_id) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_lotes_estado ON lotes(estado);
CREATE INDEX idx_lotes_fecha_registro ON lotes(fecha_registro);

-- =========================================================
-- 4. CAMAS / FILAS DE CULTIVO
-- =========================================================
CREATE TABLE camas (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lote_id BIGINT NOT NULL,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    descripcion VARCHAR(150) NOT NULL,
    capacidad_referencial INT NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVA',
    usuario_registro_id BIGINT NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_camas_lote
        FOREIGN KEY (lote_id) REFERENCES lotes(id),
    CONSTRAINT fk_camas_usuario_registro
        FOREIGN KEY (usuario_registro_id) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_camas_lote ON camas(lote_id);
CREATE INDEX idx_camas_estado ON camas(estado);

-- =========================================================
-- 5. SIEMBRAS / UBICACIÓN EN CAMAS
-- =========================================================
CREATE TABLE siembras (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lote_id BIGINT NOT NULL,
    cama_id BIGINT NOT NULL,
    fecha_siembra DATE NOT NULL,
    cantidad_registrada INT NOT NULL,
    observacion VARCHAR(255),
    estado VARCHAR(30) NOT NULL DEFAULT 'REGISTRADA',
    usuario_registro_id BIGINT NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_siembras_lote
        FOREIGN KEY (lote_id) REFERENCES lotes(id),
    CONSTRAINT fk_siembras_cama
        FOREIGN KEY (cama_id) REFERENCES camas(id),
    CONSTRAINT fk_siembras_usuario
        FOREIGN KEY (usuario_registro_id) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_siembras_lote ON siembras(lote_id);
CREATE INDEX idx_siembras_cama ON siembras(cama_id);
CREATE INDEX idx_siembras_fecha ON siembras(fecha_siembra);

-- =========================================================
-- 6. UNIFORMIZACIONES
-- =========================================================
CREATE TABLE uniformizaciones (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lote_id BIGINT NOT NULL,
    cama_id BIGINT NOT NULL,
    fecha_uniformizacion DATE NOT NULL,
    criterio VARCHAR(120) NOT NULL,
    cantidad_inicial INT NOT NULL,
    cantidad_uniformizada INT NOT NULL,
    observacion VARCHAR(255),
    estado VARCHAR(30) NOT NULL DEFAULT 'REGISTRADA',
    usuario_registro_id BIGINT NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_uniformizaciones_lote
        FOREIGN KEY (lote_id) REFERENCES lotes(id),
    CONSTRAINT fk_uniformizaciones_cama
        FOREIGN KEY (cama_id) REFERENCES camas(id),
    CONSTRAINT fk_uniformizaciones_usuario
        FOREIGN KEY (usuario_registro_id) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_uniformizaciones_lote ON uniformizaciones(lote_id);
CREATE INDEX idx_uniformizaciones_cama ON uniformizaciones(cama_id);
CREATE INDEX idx_uniformizaciones_fecha ON uniformizaciones(fecha_uniformizacion);

-- =========================================================
-- 7. FORMALIZACIONES
-- =========================================================
CREATE TABLE formalizaciones (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lote_id BIGINT NOT NULL,
    cama_id BIGINT NOT NULL,
    fecha_formalizacion DATE NOT NULL,
    detalle VARCHAR(180) NOT NULL,
    cantidad_bandejas INT NOT NULL,
    cantidad_plantas INT NOT NULL,
    observacion VARCHAR(255),
    estado VARCHAR(30) NOT NULL DEFAULT 'REGISTRADA',
    usuario_registro_id BIGINT NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_formalizaciones_lote
        FOREIGN KEY (lote_id) REFERENCES lotes(id),
    CONSTRAINT fk_formalizaciones_cama
        FOREIGN KEY (cama_id) REFERENCES camas(id),
    CONSTRAINT fk_formalizaciones_usuario
        FOREIGN KEY (usuario_registro_id) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_formalizaciones_lote ON formalizaciones(lote_id);
CREATE INDEX idx_formalizaciones_cama ON formalizaciones(cama_id);
CREATE INDEX idx_formalizaciones_fecha ON formalizaciones(fecha_formalizacion);

-- =========================================================
-- 8. CLASIFICACIONES
-- =========================================================
CREATE TABLE clasificaciones (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lote_id BIGINT NOT NULL,
    cama_id BIGINT NOT NULL,
    fecha_clasificacion DATE NOT NULL,
    estado_planta VARCHAR(60) NOT NULL,
    tamano VARCHAR(60) NOT NULL,
    condicion VARCHAR(120) NOT NULL,
    cantidad INT NOT NULL,
    observacion VARCHAR(255),
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    usuario_registro_id BIGINT NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_clasificaciones_lote
        FOREIGN KEY (lote_id) REFERENCES lotes(id),
    CONSTRAINT fk_clasificaciones_cama
        FOREIGN KEY (cama_id) REFERENCES camas(id),
    CONSTRAINT fk_clasificaciones_usuario
        FOREIGN KEY (usuario_registro_id) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_clasificaciones_lote ON clasificaciones(lote_id);
CREATE INDEX idx_clasificaciones_cama ON clasificaciones(cama_id);
CREATE INDEX idx_clasificaciones_estado ON clasificaciones(estado);
CREATE INDEX idx_clasificaciones_fecha ON clasificaciones(fecha_clasificacion);

-- =========================================================
-- 9. DESPACHOS
-- =========================================================
CREATE TABLE despachos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lote_id BIGINT NOT NULL,
    fecha_despacho DATE NOT NULL,
    modalidad_despacho VARCHAR(80) NOT NULL,
    -- Columna opcional de compatibilidad. No debe ser obligatoria.
    modalidad VARCHAR(80) NULL,
    cantidad_despachada INT NOT NULL,
    -- Columna opcional de compatibilidad. No debe ser obligatoria.
    cantidad INT NULL,
    destino VARCHAR(120),
    guia_remision VARCHAR(80),
    validacion_calidad VARCHAR(120) NOT NULL,
    observacion VARCHAR(255),
    estado VARCHAR(30) NOT NULL DEFAULT 'REGISTRADO',
    usuario_registro_id BIGINT NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_despachos_lote
        FOREIGN KEY (lote_id) REFERENCES lotes(id),
    CONSTRAINT fk_despachos_usuario
        FOREIGN KEY (usuario_registro_id) REFERENCES usuarios(id),
    CONSTRAINT chk_despachos_modalidad
        CHECK (modalidad_despacho IN ('JABAS', 'BINS', 'MADERA', 'OTRO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_despachos_lote ON despachos(lote_id);
CREATE INDEX idx_despachos_fecha ON despachos(fecha_despacho);
CREATE INDEX idx_despachos_estado ON despachos(estado);

-- =========================================================
-- DATOS INICIALES
-- =========================================================
INSERT IGNORE INTO roles (nombre, descripcion, estado) VALUES
('ADMINISTRADOR', 'Acceso total al sistema', TRUE),
('SUPERVISOR', 'Supervisa el proceso operativo', TRUE),
('CONTABILIZADOR', 'Registra conteos y cantidades', TRUE),
('OPERARIO', 'Registra actividades operativas', TRUE),
('CONTROL_CALIDAD', 'Valida calidad y despacho', TRUE);

-- Nota:
-- El usuario admin se crea automáticamente desde DataInitializer con:
-- usuario: admin
-- contraseña: admin123
-- Si se desea insertar manualmente, se debe usar una contraseña en formato BCrypt.

