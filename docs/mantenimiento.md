# Mantenimiento y actualización

## Principios

- El despliegue se ejecuta con un usuario no root.
- El archivo `.env` vive solo en el VPS con permisos `600`.
- MySQL y backend no publican puertos al exterior.
- Antes de modificar versiones, esquema o dependencias, crea un backup y valida la restauración más reciente.
- Producción usa `ddl-auto=validate`; las modificaciones de esquema deben revisarse y versionarse.

## Actualización ordinaria

1. Revisa cambios y etiqueta/commit a desplegar.
2. Crea backup y guarda la referencia actual:

```bash
cd /srv/blueberrytrace
bash scripts/backup-db.sh
git rev-parse HEAD | tee /srv/blueberrytrace/.deployed-commit
```

3. Descarga el release y construye:

```bash
git fetch --tags
git checkout TAG_O_COMMIT_APROBADO
docker compose --env-file .env config
docker compose --env-file .env up -d --build
```

4. Revisa contenedores, healthcheck, login, una operación y un reporte.
5. Si la validación falla, sigue `rollback.md`.

## Cambios de base de datos

- Revisa la migración SQL antes de ejecutarla.
- Ejecuta backup primero.
- Prueba la migración en una copia/restauración de ensayo.
- Ejecuta la migración con la cuenta administrativa solo cuando esté aprobada:

```bash
MIGRATION_CONFIRM=YES bash scripts/migrate-db.sh
```
- Mantén `spring.jpa.hibernate.ddl-auto=validate` en producción.

## Reinicio controlado

```bash
cd /srv/blueberrytrace
docker compose --env-file .env restart
docker compose --env-file .env ps
docker compose --env-file .env logs --tail=100
```

Para recrear después de cambios de variables o plantillas Nginx:

```bash
docker compose --env-file .env up -d --force-recreate backend frontend
```

## Rotación de credenciales

1. Programa mantenimiento.
2. Cambia la contraseña de MySQL con una cuenta administrativa, actualiza `.env` y reinicia backend.
3. Cambia `MYSQL_ROOT_PASSWORD` solo con un procedimiento MySQL documentado y probado; no edites la variable esperando que cambie una base ya inicializada.
4. Rota la cuenta bootstrap solo si aún estuviera habilitada; lo recomendado es mantener `BOOTSTRAP_ADMIN_ENABLED=false` después de la primera cuenta.
5. Verifica acceso y elimina copias temporales de archivos secretos.

## Política de backup

- Diario, 14 días como mínimo localmente.
- Copia externa cifrada según la política de Vivero Los Viñedos.
- Prueba de restauración mensual.
- Backup previo a cada actualización, migración o intervención en MySQL.
