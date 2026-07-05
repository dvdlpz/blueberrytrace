# Entornos de ejecución

## Desarrollo local

Se ejecuta con Node 22, Java 21 y una instancia local de MySQL compatible. El frontend usa Vite en `http://localhost:5173` y el backend Spring Boot en `http://localhost:8080`.

```bash
npm ci
cp frontend/.env.example frontend/.env
cp backend/.env.example backend/.env
npm run backend:run
# En otra terminal
npm run frontend:dev
```

`npm run backend:run` procesa `backend/.env` sin ejecutarlo como Bash. No uses `source backend/.env`.

## Docker local

Usa una copia privada de `.env` y Compose para reproducir producción. No publiques MySQL ni backend fuera de la red Docker.

```bash
cp .env.example .env
docker compose --env-file .env config
docker compose --env-file .env up -d --build
docker compose --env-file .env ps
```

Para pruebas de HTTP local se puede mantener Nginx sin certificado; la configuración HTTPS se valida en el VPS con un dominio real.

## VPS de producción

El VPS usa Docker Compose, Nginx y Certbot. Solo se permiten los puertos 22, 80 y 443. MySQL y el backend se mantienen en la red interna Docker. El perfil `prod` ejecuta Flyway y usa `ddl-auto=validate`; no modifica el esquema mediante Hibernate.

Consulta `despliegue-vps.md`, `backup-restore.md`, `migracion-datos.md`, `mantenimiento.md` y `rollback.md` antes de intervenir producción.


## Flyway en desarrollo local

Para una base local nueva, el perfil `local` usa `ddl-auto=update` y deja Flyway desactivado por defecto para que Hibernate cree las tablas iniciales. En un entorno que ya fue inicializado con `backend/db/init/01-schema.sql` o que contiene información heredada, activa Flyway de forma explícita y después de un backup:

```bash
FLYWAY_ENABLED=true npm run backend:run
```

En producción, Flyway permanece habilitado y `ddl-auto=validate`; nunca se usa `update`.
