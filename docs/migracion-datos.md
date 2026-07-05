# Migración de datos y Flyway

## Regla previa

Antes de cualquier migración en un VPS, realiza un backup y prueba una restauración en un entorno separado. No uses `ddl-auto=update` en producción.

## Flyway

El backend usa Flyway y aplica migraciones desde:

```text
backend/src/main/resources/db/migration/
```

La migración `V1__traceability_roles_audit_and_session_hardening.sql` es aditiva para MySQL 8.4: agrega columnas de sesión y versión, crea lotes trazables y mermas, extiende auditoría e incorpora índices. No elimina tablas ni datos.

En producción el perfil tiene:

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=0
```

## Instalación existente

1. Detén cambios operativos o programa una ventana de mantenimiento.
2. Crea y verifica un backup.
3. Actualiza la imagen/backend con la migración.
4. Inicia el backend: Flyway registra y aplica las migraciones pendientes antes de que Hibernate valide el esquema.
5. Verifica `/api/v1/health`, tablas nuevas, `flyway_schema_history` y login.
6. Revisa datos históricos: los movimientos anteriores continúan sin `lote_trazable_id` hasta que exista evidencia suficiente para normalizarlos.

## Normalización de legado

No asignes un lote trazable por inferencia. Solo normaliza registros antiguos con procedencia documentada, cama identificable, fechas consistentes y responsable validado.

El módulo **Lotes trazables** muestra a un administrador los candidatos no vinculados que coinciden por invernadero físico, cama inicial y fecha. Para vincular uno se exige una evidencia textual y se registra el evento `NORMALIZAR_LEGADO` en Auditoría.

No se vinculan despachos históricos automáticamente: un despacho requiere una clasificación fuente validada. Mantén esos registros como legado hasta contar con una relación documental verificable.

## Flujo real del vivero

Las migraciones `V4` a `V8` incorporan jabas, riegos programados, recuperación por riego, pedidos, empaques y las relaciones necesarias para formalización y clasificación. No asignan datos físicos a movimientos anteriores: cualquier normalización debe realizarse con evidencia operativa.
