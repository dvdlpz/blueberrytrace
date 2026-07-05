# Backend BlueberryTrace

API Java 21/Spring Boot para BlueberryTrace. Expone rutas bajo `/api/v1/**`, aplica sesión con CSRF, autorización por roles y reglas de consistencia de la trazabilidad.

## Perfiles

- `local`: configuración de desarrollo con variables explícitas en `backend/.env.example`.
- `prod`: exige `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `CORS_ALLOWED_ORIGINS`; usa cookies HTTPS seguras y `ddl-auto=validate`.

No hay credenciales ni administrador predeterminado. La primera cuenta solo se crea si `BLUEBERRYTRACE_BOOTSTRAP_ADMIN_ENABLED=true` y se proporcionan datos de bootstrap seguros. Deshabilítalo inmediatamente después de usarlo.

## Comandos

```bash
./mvnw -pl backend test
./mvnw -pl backend clean package
```

## Base de datos

- Instalación nueva Docker: `db/init/01-schema.sql`.
- Instalación existente: revisar y ejecutar `db/migrations/V1__production_hardening.sql` después de un backup probado.

La guía de producción está en `../docs/despliegue-vps.md`.
