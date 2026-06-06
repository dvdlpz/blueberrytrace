-- Script base para BlueberryTrace
-- El esquema de tablas se genera desde las entidades JPA usando spring.jpa.hibernate.ddl-auto=update.
-- Este archivo sirve como evidencia académica y punto de partida para crear la base de datos local.

CREATE DATABASE IF NOT EXISTS vlv_blueberry_system
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE vlv_blueberry_system;

-- Usuario inicial generado automáticamente por DataInitializer:
-- Usuario: admin
-- Contraseña: admin123

-- Tablas principales esperadas por el modelo JPA:
-- roles, usuarios, lotes, camas, siembras, uniformizaciones,
-- formalizaciones, clasificaciones, despachos, auditorias.
